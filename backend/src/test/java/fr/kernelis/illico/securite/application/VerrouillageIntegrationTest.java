package fr.kernelis.illico.securite.application;

import static fr.kernelis.illico.securite.ComptesDeTest.MOT_DE_PASSE;
import static fr.kernelis.illico.securite.ComptesDeTest.compteActif;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import fr.kernelis.illico.TestcontainersConfiguration;
import fr.kernelis.illico.securite.domain.Base32;
import fr.kernelis.illico.securite.domain.Totp;
import fr.kernelis.illico.securite.domain.Utilisateur;
import fr.kernelis.illico.securite.infrastructure.UtilisateurRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verrouillage après 5 échecs consécutifs (critère d'US-01).
 * L'horloge est simulée : on vérifie la levée du verrouillage sans attendre 15 minutes.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VerrouillageIntegrationTest {

    private static final String IDENTIFIANT = "sophie.martin@logis-estuaire.fr";
    private static final Instant T0 = Instant.parse("2027-02-01T09:00:00Z");

    @MockitoBean
    private Clock horloge;

    @Autowired
    private ConnexionService service;

    @Autowired
    private UtilisateurRepository depot;

    @Autowired
    private PasswordEncoder encodeur;

    private Utilisateur compte;

    @BeforeEach
    void preparer() {
        when(horloge.instant()).thenReturn(T0);
        compte = depot.saveAndFlush(compteActif(IDENTIFIANT, encodeur));
    }

    @Test
    void apres_cinq_mots_de_passe_faux_meme_le_bon_mot_de_passe_est_refuse() {
        IntStream.range(0, Utilisateur.SEUIL_ECHECS)
            .forEach(i -> service.verifierMotDePasse(IDENTIFIANT, "un mot de passe faux"));

        assertThat(service.verifierMotDePasse(IDENTIFIANT, MOT_DE_PASSE)).isEmpty();
    }

    @Test
    void le_verrouillage_est_leve_apres_quinze_minutes() {
        IntStream.range(0, Utilisateur.SEUIL_ECHECS)
            .forEach(i -> service.verifierMotDePasse(IDENTIFIANT, "un mot de passe faux"));

        when(horloge.instant()).thenReturn(T0.plus(Utilisateur.DUREE_VERROUILLAGE));

        assertThat(service.verifierMotDePasse(IDENTIFIANT, MOT_DE_PASSE)).hasValue(compte.getId());
    }

    /** Sans cela, un attaquant qui connaît le mot de passe pourrait essayer les codes sans limite. */
    @Test
    void les_codes_faux_comptent_aussi_comme_des_echecs() {
        EtapeMotDePasseValidee etape = new EtapeMotDePasseValidee(compte.getId(), T0);
        String codeFaux = codeFauxAuMoment(T0);

        IntStream.range(0, Utilisateur.SEUIL_ECHECS).forEach(i -> service.verifierSecondFacteur(etape, codeFaux));

        assertThat(service.verifierMotDePasse(IDENTIFIANT, MOT_DE_PASSE)).isEmpty();
        assertThat(service.verifierSecondFacteur(etape, codeAuMoment(T0))).isEmpty();
    }

    private String codeAuMoment(Instant instant) {
        return Totp.code(Base32.decoder(compte.getSecretTotp()), Totp.pas(instant), 6);
    }

    /** Un code qui n'est valide pour aucun des pas acceptés autour de l'instant donné. */
    private String codeFauxAuMoment(Instant instant) {
        byte[] secret = Base32.decoder(compte.getSecretTotp());
        long pas = Totp.pas(instant);
        Set<String> codesValides = Set.copyOf(LongStream.rangeClosed(pas - 1, pas + 1)
            .mapToObj(candidat -> Totp.code(secret, candidat, 6)).toList());
        return IntStream.range(0, 10)
            .mapToObj(chiffre -> String.valueOf(chiffre).repeat(6))
            .filter(code -> !codesValides.contains(code))
            .findFirst()
            .orElseThrow();
    }
}
