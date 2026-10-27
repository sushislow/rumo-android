package br.com.rumo.diario;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

/** Authenticated encryption format, independent of Android for JVM verification. */
public final class VaultCrypto {
    private static final byte[] MAGIC = new byte[]{82, 85, 77, 79, 1};
    public static final int PIN_ITERATIONS = 310000;
    private VaultCrypto() {}

    public static byte[] encrypt(byte[] clear, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        if (iv.length != 12) throw new GeneralSecurityException("Unexpected IV size");
        cipher.updateAAD(MAGIC);
        byte[] encrypted = cipher.doFinal(clear);
        return ByteBuffer.allocate(MAGIC.length + iv.length + encrypted.length)
                .put(MAGIC).put(iv).put(encrypted).array();
    }

    public static byte[] decrypt(byte[] packed, SecretKey key) throws GeneralSecurityException {
        if (packed.length < MAGIC.length + 12 + 16
                || !MessageDigest.isEqual(MAGIC, Arrays.copyOf(packed, MAGIC.length))) {
            throw new GeneralSecurityException("Invalid vault envelope");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(128, Arrays.copyOfRange(packed, MAGIC.length, MAGIC.length + 12)));
        cipher.updateAAD(MAGIC);
        return cipher.doFinal(packed, MAGIC.length + 12, packed.length - MAGIC.length - 12);
    }

    public static boolean validPin(String pin) {
        return pin != null && pin.matches("[0-9]{6,12}");
    }

    public static byte[] pinHash(char[] pin, byte[] salt, String algorithm) throws GeneralSecurityException {
        if (salt.length != 16 || !("PBKDF2WithHmacSHA256".equals(algorithm)
                || "PBKDF2WithHmacSHA1".equals(algorithm))) throw new GeneralSecurityException("Invalid PIN parameters");
        PBEKeySpec spec = new PBEKeySpec(pin, salt, PIN_ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }

    public static long delayForFailures(int attempts) {
        if (attempts < 5) return 0;
        return Math.min(1800000L, 30000L * (1L << Math.min(6, (attempts - 5) / 5)));
    }
}
