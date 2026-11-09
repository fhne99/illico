package fr.kernelis.illico.securite.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TotpTest {

    /** Secret de test de la RFC 6238 (annexe B), pour l'algorithme HMAC-SHA1. */
    private static final byte[] SECRET_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    /** Instant de référence : 1 111 111 111 s, soit le pas 37 037 037, dont le code est 050471. */
    private static final Instant INSTANT = Instant.ofEpochSecond(1_111_111_111L);
    private static final String CODE_DU_PAS = "050471";
    private static final long PAS = 37_037_037L;

    /**
     * Valeurs de test officielles de la RFC 6238 (annexe B).
     * La RFC donne des codes à 8 chiffres ; le code à 6 chiffres en est la fin.
     */
    @ParameterizedTest(name = "à {0} s, le code est {1}")
    @CsvSource({
        "59, 94287082",
        "1111111109, 07081804",
        "1111111111, 14050471",
        "1234567890, 89005924",
        "2000000000, 69279037",
        "20000000000, 65353130"
    })
    void le_calcul_des_codes_respecte_la_rfc_6238(long secondes, String codeAttendu) {
        long pas = Totp.pas(Instant.ofEpochSecond(secondes));

        assertThat(Totp.code(SECRET_RFC, pas, 8)).isEqualTo(codeAttendu);
        assertThat(Totp.code(SECRET_RFC, pas, 6)).isEqualTo(codeAttendu.substring(2));
    }

    @Test
    void le_code_du_moment_est_accepte_et_son_pas_est_renvoye() {
        assertThat(Totp.verifier(SECRET_RFC, CODE_DU_PAS, INSTANT, null)).hasValue(PAS);
    }

    /** Le téléphone de l'utilisateur peut avancer ou retarder de quelques secondes. */
    @Test
    void un_code_d_un_pas_d_ecart_est_accepte_pour_absorber_un_decalage_d_horloge() {
        assertThat(Totp.verifier(SECRET_RFC, CODE_DU_PAS, INSTANT.plusSeconds(30), null)).hasValue(PAS);
        assertThat(Totp.verifier(SECRET_RFC, CODE_DU_PAS, INSTANT.minusSeconds(30), null)).hasValue(PAS);
    }

    @Test
    void un_code_perime_de_plus_d_un_pas_est_refuse() {
        assertThat(Totp.verifier(SECRET_RFC, CODE_DU_PAS, INSTANT.plusSeconds(60), null)).isEmpty();
    }

    /** Un code intercepté ne doit pas pouvoir resservir, même dans sa fenêtre de validité. */
    @Test
    void un_code_deja_utilise_est_refuse() {
        assertThat(Totp.verifier(SECRET_RFC, CODE_DU_PAS, INSTANT, PAS)).isEmpty();
    }

    @Test
    void un_nouveau_code_reste_accepte_apres_un_code_precedent() {
        assertThat(Totp.verifier(SECRET_RFC, CODE_DU_PAS, INSTANT, PAS - 1)).hasValue(PAS);
    }

    @Test
    void un_code_faux_est_refuse() {
        assertThat(Totp.verifier(SECRET_RFC, "123456", INSTANT, null)).isEmpty();
    }

    /** Une saisie mal formée est refusée sans erreur technique. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "05047", "0504711", "05047a", " 050471"})
    void un_code_mal_forme_est_refuse(String codeSaisi) {
        assertThat(Totp.verifier(SECRET_RFC, codeSaisi, INSTANT, null)).isEmpty();
    }

    @Test
    void un_secret_genere_fait_160_bits_et_differe_a_chaque_fois() {
        String secret = Totp.genererSecret();

        assertThat(Base32.decoder(secret)).hasSize(20);
        assertThat(Totp.genererSecret()).isNotEqualTo(secret);
    }
}
