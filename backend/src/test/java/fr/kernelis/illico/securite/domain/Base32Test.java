package fr.kernelis.illico.securite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Base32Test {

    /** Valeurs de test officielles de la RFC 4648, section 10. */
    @ParameterizedTest(name = "« {0} » s''encode en « {1} »")
    @CsvSource({
        "f, MY======",
        "fo, MZXQ====",
        "foo, MZXW6===",
        "foob, MZXW6YQ=",
        "fooba, MZXW6YTB",
        "foobar, MZXW6YTBOI======"
    })
    void l_encodage_et_le_decodage_respectent_la_rfc_4648(String texte, String attendu) {
        byte[] octets = texte.getBytes(StandardCharsets.US_ASCII);

        assertThat(Base32.encoder(octets)).isEqualTo(attendu);
        assertThat(Base32.decoder(attendu)).isEqualTo(octets);
    }

    /** Un utilisateur peut recopier son secret en minuscules, avec des espaces, sans « = ». */
    @Test
    void le_decodage_tolere_minuscules_espaces_et_absence_de_remplissage() {
        assertThat(Base32.decoder("mzxw 6ytb oi")).isEqualTo("foobar".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void un_caractere_hors_alphabet_est_refuse() {
        assertThatThrownBy(() -> Base32.decoder("MZXW1"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
