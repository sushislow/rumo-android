package br.com.rumo.diario;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import org.json.JSONObject;

/** Verifies a server-issued seven-day lease. Contains no signing secret. */
public final class LicenseVerifier {
    public static final long MAX_LEASE_SECONDS = 7L * 24 * 60 * 60;
    private LicenseVerifier() { }
    static byte[] decode(String input) {
        String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        if(input.length()%4==1)throw new IllegalArgumentException("Base64 inválido");
        java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();int bits=0,value=0;
        for(int i=0;i<input.length();i++){
            int digit=alphabet.indexOf(input.charAt(i));if(digit<0)throw new IllegalArgumentException("Base64 inválido");
            value=(value<<6)|digit;bits+=6;if(bits>=8){bits-=8;output.write((value>>bits)&255);}
        }
        if(bits>0&&(value&((1<<bits)-1))!=0)throw new IllegalArgumentException("Base64 inválido");
        return output.toByteArray();
    }
    public static boolean valid(String token,String publicKeyPem,String issuer,String uid,String audience,String mode,long nowSeconds) {
        try {
            if(token==null||token.length()>10000||uid==null||uid.isEmpty()||publicKeyPem==null||publicKeyPem.isEmpty())return false;
            String[] parts=token.split("\\.",-1);if(parts.length!=3)return false;
            JSONObject header=new JSONObject(new String(decode(parts[0]),StandardCharsets.UTF_8));
            if(!"RS256".equals(header.optString("alg"))||!"JWT".equals(header.optString("typ")))return false;
            String pem=publicKeyPem.replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----","").replaceAll("\\s","").replace("=","").replace('+','-').replace('/','_');
            java.security.interfaces.RSAPublicKey key=(java.security.interfaces.RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decode(pem)));
            if(key.getModulus().bitLength()<2048)return false;
            Signature signature=Signature.getInstance("SHA256withRSA");signature.initVerify(key);signature.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
            if(!signature.verify(decode(parts[2])))return false;
            JSONObject data=new JSONObject(new String(decode(parts[1]),StandardCharsets.UTF_8));
            long issued=data.getLong("iat"),expires=data.getLong("exp");
            return issuer.equals(data.getString("iss"))&&uid.equals(data.getString("sub"))
                &&mode.equals(data.getString("mode"))&&audience.equals(data.getString("aud"))&&"rumo_full".equals(data.getString("product"))
                &&issued>0&&issued<=nowSeconds+300&&expires>nowSeconds&&expires>issued&&expires-issued<=MAX_LEASE_SECONDS;
        }catch(Exception invalid){return false;}
    }
}
