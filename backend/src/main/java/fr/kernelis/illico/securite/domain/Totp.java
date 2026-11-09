package fr.kernelis.illico.securite.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Codes à usage unique basés sur le temps (TOTP, RFC 6238), compatibles avec
 * Google Authenticator, Microsoft Authenticator et les autres applications du standard.
 * Paramètres usuels : HMAC-SHA1, codes de 6 chiffres renouvelés toutes les 30 secondes.
 */
public final class Totp {

    static final int DUREE_PAS_SECONDES = 30;
    static final int NOMBRE_CHIFFRES = 6;
    /** Tolérance d'un pas avant ou après : absorbe un décalage d'horloge de 30 secondes. */
    static final int FENETRE_PAS = 1;
    private static final int TAILLE_SECRET_OCTETS = 20; // 160 bits, taille recommandée par la RFC 4226
    private static final Pattern FORMAT_CODE = Pattern.compile("\\d{" + NOMBRE_CHIFFRES + "}");
    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private Totp() {
    }

    /** Nouveau secret aléatoire, encodé en Base32 pour l'application d'authentification (US-07). */
    public static String genererSecret() {
        byte[] secret = new byte[TAILLE_SECRET_OCTETS];
        ALEATOIRE.nextBytes(secret);
        return Base32.encoder(secret);
    }

    /** Numéro du pas de 30 secondes contenant cet instant. */
    public static long pas(Instant instant) {
        return Math.floorDiv(instant.getEpochSecond(), DUREE_PAS_SECONDES);
    }

    /** Calcul du code pour un pas donné (RFC 4226, section 5.3). */
    public static String code(byte[] secret, long pas, int nombreChiffres) {
        byte[] empreinte = hmacSha1(secret, ByteBuffer.allocate(Long.BYTES).putLong(pas).array());
        int decalage = empreinte[empreinte.length - 1] & 0x0F;
        int binaire = ((empreinte[decalage] & 0x7F) << 24)
            | ((empreinte[decalage + 1] & 0xFF) << 16)
            | ((empreinte[decalage + 2] & 0xFF) << 8)
            | (empreinte[decalage + 3] & 0xFF);
        int valeur = binaire % (int) Math.pow(10, nombreChiffres);
        return String.format(Locale.ROOT, "%0" + nombreChiffres + "d", valeur);
    }

    /**
     * Vérifie un code saisi par l'utilisateur.
     *
     * @param dernierPasAccepte pas du dernier code accepté pour ce compte, ou null :
     *                          un code déjà utilisé, ou plus ancien, est refusé (protection contre le rejeu)
     * @return le pas du code accepté, à enregistrer sur le compte, ou vide si le code est refusé
     */
    public static OptionalLong verifier(byte[] secret, String codeSaisi, Instant instant, Long dernierPasAccepte) {
        if (codeSaisi == null || !FORMAT_CODE.matcher(codeSaisi).matches()) {
            return OptionalLong.empty();
        }
        long pasCourant = pas(instant);
        for (long candidat = pasCourant - FENETRE_PAS; candidat <= pasCourant + FENETRE_PAS; candidat++) {
            boolean dejaUtilise = dernierPasAccepte != null && candidat <= dernierPasAccepte;
            if (!dejaUtilise && egauxEnTempsConstant(code(secret, candidat, NOMBRE_CHIFFRES), codeSaisi)) {
                return OptionalLong.of(candidat);
            }
        }
        return OptionalLong.empty();
    }

    /** Comparaison en temps constant : le temps de réponse ne révèle pas combien de chiffres sont justes. */
    private static boolean egauxEnTempsConstant(String attendu, String saisi) {
        return MessageDigest.isEqual(
            attendu.getBytes(StandardCharsets.US_ASCII), saisi.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] hmacSha1(byte[] cle, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(cle, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            // HmacSHA1 est garanti par toute machine virtuelle Java : cette erreur ne peut pas survenir
            throw new IllegalStateException("HMAC-SHA1 indisponible", e);
        }
    }
}
