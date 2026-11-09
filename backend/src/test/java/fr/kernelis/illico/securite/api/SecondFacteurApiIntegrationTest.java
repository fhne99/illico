package fr.kernelis.illico.securite.api;

import static fr.kernelis.illico.securite.ComptesDeTest.MOT_DE_PASSE;
import static fr.kernelis.illico.securite.ComptesDeTest.compteActif;
import static org.assertj.core.api.Assertions.assertThat;

import fr.kernelis.illico.TestcontainersConfiguration;
import fr.kernelis.illico.securite.domain.Base32;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Parcours complet de connexion en deux étapes, tel que l'effectuera l'application Angular. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SecondFacteurApiIntegrationTest {

    private static final String IDENTIFIANT = "sophie.martin@logis-estuaire.fr";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UtilisateurRepository depot;

    @Autowired
    private PasswordEncoder encodeur;

    private Utilisateur compte;
    private NavigateurSimule navigateur;

    @BeforeEach
    void preparer() {
        compte = depot.save(compteActif(IDENTIFIANT, encodeur));
        navigateur = new NavigateurSimule(port);
    }

    @AfterEach
    void nettoyer() {
        depot.deleteAll();
    }

    @Test
    void avec_le_bon_code_l_utilisateur_est_connecte_et_accede_a_son_espace() throws Exception {
        etapeMotDePasse();

        HttpResponse<String> reponse = envoyerCode(codeDuMoment());
        HttpResponse<String> profil = navigateur.get("/api/auth/moi");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("CONNECTE");
        assertThat(profil.statusCode()).isEqualTo(200);
        assertThat(profil.body()).contains(IDENTIFIANT).contains("GESTIONNAIRE");
    }

    @Test
    void la_premiere_etape_seule_ne_donne_acces_a_rien() throws Exception {
        etapeMotDePasse();

        assertThat(navigateur.get("/api/auth/moi").statusCode()).isEqualTo(401);
    }

    @Test
    void avec_un_code_faux_l_acces_reste_ferme() throws Exception {
        etapeMotDePasse();

        HttpResponse<String> reponse = envoyerCode(codeFaux());

        assertThat(reponse.statusCode()).isEqualTo(401);
        assertThat(reponse.body()).contains(ReponseConnexion.MESSAGE_CODE_REFUSE);
        assertThat(navigateur.get("/api/auth/moi").statusCode()).isEqualTo(401);
    }

    /** On ne peut pas sauter l'étape du mot de passe en envoyant directement un code. */
    @Test
    void un_code_sans_premiere_etape_est_refuse() throws Exception {
        assertThat(envoyerCode(codeDuMoment()).statusCode()).isEqualTo(401);
    }

    /** Un code intercepté pendant une connexion ne permet pas d'en ouvrir une seconde. */
    @Test
    void un_code_deja_utilise_ne_permet_pas_une_nouvelle_connexion() throws Exception {
        String code = codeDuMoment();
        etapeMotDePasse();
        envoyerCode(code);
        navigateur.postJson("/api/auth/deconnexion", "{}");

        etapeMotDePasse();

        assertThat(envoyerCode(code).statusCode()).isEqualTo(401);
    }

    /** Protection contre la fixation de session. */
    @Test
    void l_identifiant_de_session_change_a_la_connexion() throws Exception {
        etapeMotDePasse();
        String sessionAvant = navigateur.cookie("JSESSIONID");

        envoyerCode(codeDuMoment());

        assertThat(navigateur.cookie("JSESSIONID")).isNotNull().isNotEqualTo(sessionAvant);
    }

    /** Critère d'US-01 : la session repose sur un cookie protégé, jamais sur un jeton lisible par le navigateur. */
    @Test
    void le_cookie_de_session_est_http_only_secure_et_same_site_strict() throws Exception {
        etapeMotDePasse();
        envoyerCode(codeDuMoment());

        assertThat(navigateur.derniersCookiesRecus())
            .filteredOn(cookie -> cookie.startsWith("JSESSIONID="))
            .isNotEmpty()
            .allSatisfy(cookie -> assertThat(cookie)
                .containsIgnoringCase("HttpOnly")
                .containsIgnoringCase("Secure")
                .containsIgnoringCase("SameSite=Strict"));
    }

    /** Bonne pratique : la déconnexion invalide la session côté serveur. */
    @Test
    void apres_la_deconnexion_l_espace_n_est_plus_accessible() throws Exception {
        etapeMotDePasse();
        envoyerCode(codeDuMoment());

        HttpResponse<String> deconnexion = navigateur.postJson("/api/auth/deconnexion", "{}");

        assertThat(deconnexion.statusCode()).isEqualTo(204);
        assertThat(navigateur.get("/api/auth/moi").statusCode()).isEqualTo(401);
    }

    private void etapeMotDePasse() throws Exception {
        HttpResponse<String> reponse = navigateur.postJson("/api/auth/connexion",
            "{\"identifiant\":\"" + IDENTIFIANT + "\",\"motDePasse\":\"" + MOT_DE_PASSE + "\"}");
        assertThat(reponse.statusCode()).as("première étape").isEqualTo(200);
    }

    private HttpResponse<String> envoyerCode(String code) throws Exception {
        return navigateur.postJson("/api/auth/second-facteur", "{\"code\":\"" + code + "\"}");
    }

    /** Calcule le code qu'afficherait l'application d'authentification de l'utilisateur. */
    private String codeDuMoment() {
        return Totp.code(Base32.decoder(compte.getSecretTotp()), Totp.pas(Instant.now()), 6);
    }

    private String codeFaux() {
        String bon = codeDuMoment();
        return bon.equals("000000") ? "111111" : "000000";
    }
}
