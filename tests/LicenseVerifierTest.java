package br.com.rumo.diario;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import org.json.JSONObject;

/** Real RSA signatures and claims, independent from Android UI. No fixed private test key. */
public final class LicenseVerifierTest {
    private static final String ISSUER="https://rumo.example",UID="synthetic-buyer",APP="br.com.rumo.diario";
    private static final long NOW=1800000000L;
    private static int assertions;
    private static final Base64.Encoder B64=Base64.getUrlEncoder().withoutPadding();
    private static void check(boolean condition,String name){assertions++;if(!condition)throw new AssertionError(name);}
    private static JSONObject claims()throws Exception{return new JSONObject().put("iss",ISSUER).put("sub",UID).put("aud",APP).put("mode","test").put("product","rumo_full").put("iat",NOW).put("exp",NOW+604800);}
    private static String jwt(KeyPair keys,JSONObject data,String alg)throws Exception{
        String message=B64.encodeToString(new JSONObject().put("alg",alg).put("typ","JWT").toString().getBytes(StandardCharsets.UTF_8))+"."+B64.encodeToString(data.toString().getBytes(StandardCharsets.UTF_8));
        Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(keys.getPrivate());signer.update(message.getBytes(StandardCharsets.US_ASCII));return message+"."+B64.encodeToString(signer.sign());
    }
    public static void main(String[] args)throws Exception{
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair keys=generator.generateKeyPair();
        String pem="-----BEGIN PUBLIC KEY-----\n"+Base64.getEncoder().encodeToString(keys.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----";
        String token=jwt(keys,claims(),"RS256");
        check(LicenseVerifier.valid(token,pem,ISSUER,UID,APP,"test",NOW),"valid server lease");
        check(!LicenseVerifier.valid(token,pem,ISSUER,"other",APP,"test",NOW),"other account");
        check(!LicenseVerifier.valid(token,pem,"https://other.example",UID,APP,"test",NOW),"other server");
        check(!LicenseVerifier.valid(token,pem,ISSUER,UID,"another.app","test",NOW),"other app");
        check(!LicenseVerifier.valid(token,pem,ISSUER,UID,APP,"live",NOW),"test proof in production");
        check(!LicenseVerifier.valid(token,pem,ISSUER,UID,APP,"test",NOW+604800),"expired");
        check(!LicenseVerifier.valid(jwt(keys,claims().put("iat",NOW+301).put("exp",NOW+1000),"RS256"),pem,ISSUER,UID,APP,"test",NOW),"future");
        check(!LicenseVerifier.valid(jwt(keys,claims().put("exp",NOW+604801),"RS256"),pem,ISSUER,UID,APP,"test",NOW),"overlong lease");
        check(!LicenseVerifier.valid(jwt(keys,claims().put("product","another-product"),"RS256"),pem,ISSUER,UID,APP,"test",NOW),"other entitlement");
        check(!LicenseVerifier.valid(jwt(keys,claims(),"none"),pem,ISSUER,UID,APP,"test",NOW),"algorithm confusion");
        check(!LicenseVerifier.valid(token.substring(0,token.length()-15),pem,ISSUER,UID,APP,"test",NOW),"modified signature");
        String[] parts=token.split("\\.");parts[1]=B64.encodeToString(claims().put("sub","attacker").toString().getBytes(StandardCharsets.UTF_8));
        check(!LicenseVerifier.valid(String.join(".",parts),pem,ISSUER,"attacker",APP,"test",NOW),"altered claims");
        check(!LicenseVerifier.valid("..",pem,ISSUER,UID,APP,"test",NOW),"malformed");
        for(int size=0;size<128;size++){byte[] bytes=new byte[size];new java.security.SecureRandom().nextBytes(bytes);check(java.util.Arrays.equals(bytes,LicenseVerifier.decode(B64.encodeToString(bytes))),"base64 length "+size);}
        System.out.println("PASS: "+assertions+" license checks (RSA, identity, expiry, environment, tampering and decoder).");
    }
}
