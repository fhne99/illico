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
