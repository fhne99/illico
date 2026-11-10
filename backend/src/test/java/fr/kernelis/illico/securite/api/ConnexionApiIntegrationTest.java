package fr.kernelis.illico.securite.api;

import static fr.kernelis.illico.securite.FabriqueDeComptes.MOT_DE_PASSE;
import static fr.kernelis.illico.securite.FabriqueDeComptes.compteActif;
import static org.assertj.core.api.Assertions.assertThat;

import fr.kernelis.illico.TestcontainersConfiguration;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Teste le contrat HTTP de la première étape, comme le fera l'application Angular :
 * récupération du jeton CSRF, puis envoi de l'identifiant et du mot de passe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ConnexionApiIntegrationTest {

    private static final String IDENTIFIANT = "sophie.martin@logis-estuaire.fr";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UtilisateurRepository depot;

    @Autowired
    private PasswordEncoder encodeur;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void creerUnCompte() {
        depot.save(compteActif(IDENTIFIANT, encodeur));
    }

    /** Le serveur tourne dans un autre fil d'exécution : pas d'annulation automatique, on nettoie. */
    @AfterEach
    void nettoyer() {
        depot.deleteAll();
    }

    @Test
    void une_connexion_sans_jeton_csrf_est_rejetee() throws Exception {
        HttpResponse<String> reponse = client.send(requeteConnexion(IDENTIFIANT, MOT_DE_PASSE, null).build(),
            HttpResponse.BodyHandlers.ofString());

        assertThat(reponse.statusCode()).isEqualTo(403);
    }

    @Test
    void le_bon_mot_de_passe_demande_le_second_facteur() throws Exception {
        HttpResponse<String> reponse = connecter(IDENTIFIANT, MOT_DE_PASSE);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("SECOND_FACTEUR_REQUIS");
    }

    /** Anti-énumération : rien dans la réponse ne permet de savoir si le compte existe. */
    @Test
    void un_mot_de_passe_faux_et_un_compte_inconnu_recoivent_exactement_la_meme_reponse() throws Exception {
        HttpResponse<String> motDePasseFaux = connecter(IDENTIFIANT, "un autre mot de passe");
        HttpResponse<String> compteInconnu = connecter("inconnu@logis-estuaire.fr", MOT_DE_PASSE);

        assertThat(motDePasseFaux.statusCode()).isEqualTo(401);
        assertThat(compteInconnu.statusCode()).isEqualTo(401);
        assertThat(compteInconnu.body()).isEqualTo(motDePasseFaux.body());
        assertThat(motDePasseFaux.body()).contains(ReponseConnexion.MESSAGE_REFUS);
    }

    @Test
    void une_demande_sans_mot_de_passe_est_refusee_comme_mal_formee() throws Exception {
        HttpResponse<String> reponse = envoyerAvecCsrf("{\"identifiant\":\"" + IDENTIFIANT + "\"}");

        assertThat(reponse.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> connecter(String identifiant, String motDePasse) throws Exception {
        return envoyerAvecCsrf(corps(identifiant, motDePasse));
    }

    private HttpResponse<String> envoyerAvecCsrf(String corpsJson) throws Exception {
        String jeton = obtenirJetonCsrf();
        HttpRequest requete = HttpRequest.newBuilder(URI.create(url("/api/auth/connexion")))
            .header("Content-Type", "application/json")
            .header("Cookie", "XSRF-TOKEN=" + jeton)
            .header("X-XSRF-TOKEN", jeton)
            .POST(HttpRequest.BodyPublishers.ofString(corpsJson))
            .build();
        return client.send(requete, HttpResponse.BodyHandlers.ofString());
    }

    /** Comme Angular : un premier appel dépose le cookie XSRF-TOKEN, renvoyé ensuite dans un en-tête. */
    private String obtenirJetonCsrf() throws Exception {
        HttpResponse<String> reponse = client.send(
            HttpRequest.newBuilder(URI.create(url("/api/auth/csrf"))).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        return reponse.headers().allValues("Set-Cookie").stream()
            .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
            .map(cookie -> cookie.substring("XSRF-TOKEN=".length(), cookie.indexOf(';')))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cookie XSRF-TOKEN absent"));
    }

    private HttpRequest.Builder requeteConnexion(String identifiant, String motDePasse, String jeton) {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create(url("/api/auth/connexion")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(corps(identifiant, motDePasse)));
        if (jeton != null) {
            requete.header("X-XSRF-TOKEN", jeton);
        }
        return requete;
    }

    private static String corps(String identifiant, String motDePasse) {
        return "{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"" + motDePasse + "\"}";
    }

    private String url(String chemin) {
        return "http://localhost:" + port + chemin;
    }
}
