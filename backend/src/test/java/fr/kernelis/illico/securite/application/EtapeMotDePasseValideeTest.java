package fr.kernelis.illico.securite.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EtapeMotDePasseValideeTest {

    private static final Instant VALIDEE_LE = Instant.parse("2027-02-01T09:00:00Z");
    private final EtapeMotDePasseValidee etape = new EtapeMotDePasseValidee(UUID.randomUUID(), VALIDEE_LE);

    @Test
    void l_etape_reste_valide_pendant_cinq_minutes() {
        assertThat(etape.estExpiree(VALIDEE_LE.plusSeconds(5 * 60))).isFalse();
    }

    /** Un mot de passe validé puis abandonné ne doit pas laisser une porte ouverte indéfiniment. */
    @Test
    void l_etape_expire_au_dela_de_cinq_minutes() {
        assertThat(etape.estExpiree(VALIDEE_LE.plusSeconds(5 * 60 + 1))).isTrue();
    }
}
