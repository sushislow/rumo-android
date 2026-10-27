package br.com.rumo.diario;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public final class VaultCryptoTest {
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); }
    private static void mustReject(byte[] packed, SecretKey key) throws Exception {
        try { VaultCrypto.decrypt(packed, key); throw new AssertionError("Tampered data was accepted"); }
        catch (java.security.GeneralSecurityException expected) { }
    }
    public static void main(String[] args) throws Exception {
        KeyGenerator generator=KeyGenerator.getInstance("AES");generator.init(256);
        SecretKey key=generator.generateKey();
        byte[] clear="Diário privado — emoções e planos".repeat(10000).getBytes(StandardCharsets.UTF_8);
        byte[] first=VaultCrypto.encrypt(clear,key), second=VaultCrypto.encrypt(clear,key);
        check(!Arrays.equals(first,second),"Every encryption needs a fresh IV");
        check(Arrays.equals(clear,VaultCrypto.decrypt(first,key)),"Round-trip");
        check(!new String(first,StandardCharsets.ISO_8859_1).contains("Diário privado"),"No clear diary in ciphertext");
        for(int at:new int[]{0,5,16,first.length-1}) { byte[] broken=first.clone();broken[at]^=1;mustReject(broken,key); }
        mustReject(Arrays.copyOf(first,first.length-1),key);mustReject(first,generator.generateKey());
        check(VaultCrypto.validPin("001234"),"Leading zeros accepted");
        for(String bad:new String[]{"12345","1234567890123","abcdef","123 456",""})check(!VaultCrypto.validPin(bad),"Invalid PIN");
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        for(String algorithm:new String[]{"PBKDF2WithHmacSHA256","PBKDF2WithHmacSHA1"}) {
            byte[] hash=VaultCrypto.pinHash("001234".toCharArray(),salt,algorithm);
            check(MessageDigest.isEqual(hash,VaultCrypto.pinHash("001234".toCharArray(),salt,algorithm)),"PIN verification");
            check(!MessageDigest.isEqual(hash,VaultCrypto.pinHash("001235".toCharArray(),salt,algorithm)),"Wrong PIN rejected");
        }
        check(VaultCrypto.delayForFailures(4)==0,"Initial attempts");
        check(VaultCrypto.delayForFailures(5)==30000,"Backoff after five attempts");
        check(VaultCrypto.delayForFailures(10)==60000,"Escalating backoff");
        check(VaultCrypto.delayForFailures(1000)==1800000,"Backoff cap");
        System.out.println("PASS: AES-GCM integrity, random IVs, wrong keys, PIN hashing/format and attempt backoff.");
    }
}
