package fr.kernelis.illico.securite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Compte d'un utilisateur d'Illico.
 * Pas d'accesseurs en écriture : l'état du compte n'évolue que par des méthodes métier,
 * ajoutées au fil des tâches (activation, échecs de connexion, verrouillage).
 */
@Entity
@Table(name = "utilisateur")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // exigé par JPA, inaccessible au reste du code
public class Utilisateur {

    /** Nombre d'échecs consécutifs (mot de passe ou code) qui déclenche le verrouillage. */
    public static final int SEUIL_ECHECS = 5;
    /** Verrouillage temporaire plutôt que définitif : un attaquant ne peut pas bloquer un compte durablement. */
    public static final Duration DUREE_VERROUILLAGE = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String identifiant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutCompte statut;

    private String motDePasseHache;

    private String secretTotp;

    private Long dernierPasTotp;

    @Column(nullable = false)
    private int echecsConsecutifs;

    private Instant verrouilleJusqua;

    private Instant derniereConnexion;

    @Column(nullable = false, updatable = false)
    private Instant creeLe;

    @Column(nullable = false)
    private Instant modifieLe;

    @Version
    private long version;

    private Utilisateur(String identifiant, Role role) {
        this.identifiant = normaliser(identifiant);
        this.role = role;
        this.statut = StatutCompte.EN_ATTENTE_ACTIVATION;
    }

    /** Crée un compte en attente d'activation : il ne permet aucune connexion tant qu'il n'est pas activé. */
    public static Utilisateur creer(String identifiant, Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }
        return new Utilisateur(identifiant, role);
    }

    /**
     * Un identifiant est comparé sans tenir compte des espaces ni des majuscules :
     * « Sophie.Martin@exemple.fr » et « sophie.martin@exemple.fr » désignent le même compte.
     */
    public static String normaliser(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            throw new IllegalArgumentException("L'identifiant est obligatoire");
        }
        return identifiant.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Active le compte avec son mot de passe (déjà haché) et le secret de son second facteur.
     * Appelé par le parcours d'activation d'US-07 ; utilisé dès maintenant pour préparer les tests.
     */
    public void activer(String motDePasseHache, String secretTotp) {
        if (statut != StatutCompte.EN_ATTENTE_ACTIVATION) {
            throw new IllegalStateException("Seul un compte en attente d'activation peut être activé");
        }
        if (motDePasseHache == null || motDePasseHache.isBlank() || secretTotp == null || secretTotp.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe et le second facteur sont obligatoires");
        }
        this.motDePasseHache = motDePasseHache;
        this.secretTotp = secretTotp;
        this.statut = StatutCompte.ACTIF;
    }

    /** Seul un compte actif, doté d'un mot de passe, peut tenter une connexion. */
    public boolean peutSeConnecter() {
        return statut == StatutCompte.ACTIF && motDePasseHache != null;
    }

    /** Le compte refuse toute tentative, même correcte, jusqu'à la fin du verrouillage. */
    public boolean estVerrouille(Instant maintenant) {
        return verrouilleJusqua != null && maintenant.isBefore(verrouilleJusqua);
    }

    /**
     * Enregistre un mot de passe ou un code faux. Au cinquième échec consécutif, le compte est verrouillé
     * pour 15 minutes et le compteur repart de zéro : après le verrouillage, cinq nouveaux essais sont permis.
     *
     * @return vrai si cet échec vient de verrouiller le compte
     */
    public boolean enregistrerEchec(Instant maintenant) {
        this.echecsConsecutifs++;
        if (echecsConsecutifs >= SEUIL_ECHECS) {
            this.verrouilleJusqua = maintenant.plus(DUREE_VERROUILLAGE);
            this.echecsConsecutifs = 0;
            return true;
        }
        return false;
    }

    /**
     * Enregistre une connexion réussie : le pas du code accepté est mémorisé pour empêcher sa réutilisation,
     * et le compteur d'échecs repart de zéro.
     */
    public void enregistrerConnexionReussie(long pasTotpAccepte, Instant instant) {
        this.dernierPasTotp = pasTotpAccepte;
        this.derniereConnexion = instant;
        this.echecsConsecutifs = 0;
        this.verrouilleJusqua = null;
    }

    @PrePersist
    void avantCreation() {
        Instant maintenant = Instant.now();
        this.creeLe = maintenant;
        this.modifieLe = maintenant;
    }

    @PreUpdate
    void avantModification() {
        this.modifieLe = Instant.now();
    }
}
