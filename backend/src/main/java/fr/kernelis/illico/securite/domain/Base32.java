package fr.kernelis.illico.securite.domain;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Encodage Base32 (RFC 4648), format dans lequel les applications d'authentification
 * attendent le secret du second facteur (par exemple dans le QR code d'enrôlement).
 */
public final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {
    }

    public static String encoder(byte[] donnees) {
        StringBuilder resultat = new StringBuilder();
        int tampon = 0;
        int bits = 0;
        for (byte octet : donnees) {
            tampon = (tampon << 8) | (octet & 0xFF);
            bits += 8;
            while (bits >= 5) {
                resultat.append(ALPHABET.charAt((tampon >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
            tampon &= (1 << bits) - 1; // ne garde que les bits pas encore encodés
        }
        if (bits > 0) {
            resultat.append(ALPHABET.charAt((tampon << (5 - bits)) & 0x1F));
        }
        while (resultat.length() % 8 != 0) {
            resultat.append('=');
        }
        return resultat.toString();
    }

    /** Tolère les espaces, les minuscules et l'absence de caractères de remplissage « = ». */
    public static byte[] decoder(String texte) {
        String nettoye = texte.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream resultat = new ByteArrayOutputStream();
        int tampon = 0;
        int bits = 0;
        for (char caractere : nettoye.toCharArray()) {
            int valeur = ALPHABET.indexOf(caractere);
            if (valeur < 0) {
                throw new IllegalArgumentException("Caractère invalide en Base32 : " + caractere);
            }
            tampon = (tampon << 5) | valeur;
            bits += 5;
            if (bits >= 8) {
                resultat.write((tampon >> (bits - 8)) & 0xFF);
                bits -= 8;
                tampon &= (1 << bits) - 1;
            }
        }
        return resultat.toByteArray();
    }
}
