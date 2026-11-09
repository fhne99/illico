package fr.kernelis.illico.securite.api;

/** Réponse des étapes de connexion ; le message n'est renseigné qu'en cas de refus. */
public record ReponseConnexion(String etape, String message) {

    static final String MESSAGE_REFUS = "Identifiant ou mot de passe incorrect";
    static final String MESSAGE_CODE_REFUSE = "Code incorrect ou expiré";

    static ReponseConnexion secondFacteurRequis() {
        return new ReponseConnexion("SECOND_FACTEUR_REQUIS", null);
    }

    /** Message identique quelle que soit la cause : compte inconnu, inactif ou mot de passe faux. */
    static ReponseConnexion refusee() {
        return new ReponseConnexion("REFUSEE", MESSAGE_REFUS);
    }

    static ReponseConnexion connecte() {
        return new ReponseConnexion("CONNECTE", null);
    }

    /** Message identique pour un code faux, déjà utilisé, ou une première étape absente ou expirée. */
    static ReponseConnexion codeRefuse() {
        return new ReponseConnexion("REFUSEE", MESSAGE_CODE_REFUSE);
    }
}
