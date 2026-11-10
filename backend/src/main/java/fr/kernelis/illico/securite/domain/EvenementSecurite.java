package fr.kernelis.illico.securite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Trace d'un événement de sécurité. Immuable : aucune méthode ne permet de la modifier après sa création.
 * Ne contient volontairement aucun champ pour un mot de passe ou un code.
 */
@Entity
@Table(name = "evenement_securite")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvenementSecurite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private TypeEvenementSecurite type;

    @Column(updatable = false)
    private UUID utilisateurId;

    @Column(updatable = false, length = 254)
    private String identifiantSaisi;

    @Column(updatable = false, length = 45)
    private String adresseIp;

    @Column(nullable = false, updatable = false)
    private Instant survenuLe;

    private EvenementSecurite(TypeEvenementSecurite type, UUID utilisateurId, String identifiantSaisi,
                              String adresseIp, Instant survenuLe) {
        this.type = type;
        this.utilisateurId = utilisateurId;
        this.identifiantSaisi = identifiantSaisi;
        this.adresseIp = adresseIp;
        this.survenuLe = survenuLe;
    }

    public static EvenementSecurite creer(TypeEvenementSecurite type, UUID utilisateurId, String identifiantSaisi,
                                         String adresseIp, Instant survenuLe) {
        if (type == null || survenuLe == null) {
            throw new IllegalArgumentException("Le type et la date de l'événement sont obligatoires");
        }
        return new EvenementSecurite(type, utilisateurId, identifiantSaisi, adresseIp, survenuLe);
    }
}
