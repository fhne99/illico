package fr.kernelis.illico.securite.api;

/** Réponse de l'étape de connexion ; le message n'est renseigné qu'en cas de refus. */
public record ReponseConnexion(String etape, String message) {

    static final String MESSAGE_REFUS = "Identifiant ou mot de passe incorrect";

    static ReponseConnexion secondFacteurRequis() {
        return new ReponseConnexion("SECOND_FACTEUR_REQUIS", null);
    }

    /** Message identique quelle que soit la cause : compte inconnu, inactif ou mot de passe faux. */
    static ReponseConnexion refusee() {
        return new ReponseConnexion("REFUSEE", MESSAGE_REFUS);
    }
}
