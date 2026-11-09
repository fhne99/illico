package fr.kernelis.illico;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Test d'intégration du socle : l'application démarre sur une vraie base PostgreSQL (Testcontainers),
 * les migrations Flyway passent, et les deux règles de sécurité du sprint 0 sont respectées.
 * TestcontainersConfiguration est la classe générée par Spring Initializr (adapter le nom si besoin).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SanteEtSecuriteIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String chemin) throws Exception {
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin)).GET().build();
        return client.send(requete, HttpResponse.BodyHandlers.ofString());
    }

    /** US-32 : la supervision doit pouvoir interroger l'état de l'application sans authentification. */
    @Test
    void le_controle_de_sante_est_public_et_indique_que_l_application_fonctionne() throws Exception {
        HttpResponse<String> reponse = get("/actuator/health");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("\"status\":\"UP\"");
    }

    /** Risque R5 : aucune ressource métier n'est accessible à un visiteur non authentifié. */
    @Test
    void une_ressource_metier_refuse_un_visiteur_non_authentifie() throws Exception {
        assertThat(get("/api/demandes").statusCode()).isEqualTo(401);
    }

    /** Les métriques techniques ne doivent pas être publiques. */
    @Test
    void les_metriques_techniques_ne_sont_pas_publiques() throws Exception {
        assertThat(get("/actuator/metrics").statusCode()).isEqualTo(401);
    }
}
