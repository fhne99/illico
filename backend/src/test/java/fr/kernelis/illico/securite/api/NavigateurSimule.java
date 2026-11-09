package fr.kernelis.illico.securite.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Imite le comportement d'un navigateur et de l'application Angular :
 * conserve les cookies d'une réponse à l'autre et renvoie le jeton CSRF dans l'en-tête attendu.
 * Les cookies sont gérés à la main, car le cookie de session est marqué « Secure »
 * et un client HTTP standard refuserait de le renvoyer sur http://localhost.
 */
class NavigateurSimule {

    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final int port;
    private List<String> derniersCookiesRecus = List.of();

    NavigateurSimule(int port) {
        this.port = port;
    }

    HttpResponse<String> get(String chemin) throws Exception {
        return envoyer(requete(chemin).GET());
    }

    HttpResponse<String> postJson(String chemin, String corpsJson) throws Exception {
        if (!cookies.containsKey("XSRF-TOKEN")) {
            get("/api/auth/csrf");
        }
        return envoyer(requete(chemin)
            .header("Content-Type", "application/json")
            .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
            .POST(HttpRequest.BodyPublishers.ofString(corpsJson)));
    }

    String cookie(String nom) {
        return cookies.get(nom);
    }

    /** En-têtes Set-Cookie bruts de la dernière réponse, pour vérifier les attributs de sécurité. */
    List<String> derniersCookiesRecus() {
        return derniersCookiesRecus;
    }

    private HttpRequest.Builder requete(String chemin) {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin));
        if (!cookies.isEmpty()) {
            requete.header("Cookie", cookies.entrySet().stream()
                .map(cookie -> cookie.getKey() + "=" + cookie.getValue())
                .collect(Collectors.joining("; ")));
        }
        return requete;
    }

    private HttpResponse<String> envoyer(HttpRequest.Builder requete) throws Exception {
        HttpResponse<String> reponse = client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
        derniersCookiesRecus = reponse.headers().allValues("Set-Cookie");
        for (String entete : derniersCookiesRecus) {
            String paire = entete.split(";", 2)[0];
            String nom = paire.substring(0, paire.indexOf('='));
            String valeur = paire.substring(paire.indexOf('=') + 1);
            boolean supprime = valeur.isEmpty() || entete.contains("Max-Age=0");
            if (supprime) {
                cookies.remove(nom);
            } else {
                cookies.put(nom, valeur);
            }
        }
        return reponse;
    }
}
