package org.lff;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Aug 08 2017 10:10
 */
public class RSACipher implements BytesCipher {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public String encode(String source) {
        return this.encode(source.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String encode(byte[] source) {
        try {
            Cipher d = Cipher.getInstance("RSA", "BC");
            d.init(Cipher.ENCRYPT_MODE, this.pubKey);
            return Base64.getEncoder().encodeToString(d.doFinal(source));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] decodeBytes(String source) {
        try {
            Cipher d = Cipher.getInstance("RSA", "BC");
            d.init(Cipher.DECRYPT_MODE, this.privKey);
            return d.doFinal(Base64.getDecoder().decode(source));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String decode(String encoded) {
        return new String(decodeBytes(encoded), StandardCharsets.UTF_8);
    }


    PublicKey pubKey;
    PrivateKey privKey;

    public RSACipher() {
        String pub = Configuration.getData().getString("publicrsa");
        String priv = Configuration.getData().getString("privatersa");

        X509EncodedKeySpec publicKey = new X509EncodedKeySpec(Base64.getDecoder().decode(pub));
        PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(priv));

        System.out.println("PUB=" + Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        System.out.println("PRIV=" + Base64.getEncoder().encodeToString(privateKey.getEncoded()));

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
            pubKey = kf.generatePublic(publicKey);
            privKey = kf.generatePrivate(privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argu) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {

        generate();
        String s = "asfdasf2341234#@$!@#$sadfasdf";

        BytesCipher ecCipher = new RSACipher();

        long l0 = System.currentTimeMillis();
        String r = ecCipher.encode(s);
        long l1 = System.currentTimeMillis();
        System.out.println(ecCipher.decode(r));
        long l2 = System.currentTimeMillis();
        System.out.println((l2 - l1) + " " + (l1 - l0));
    }

    private static void generate() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA", "BC");
        g.initialize(128, new SecureRandom());
        KeyPair pair = g.generateKeyPair();

        X509EncodedKeySpec publicKey = new X509EncodedKeySpec(pair.getPublic().getEncoded());
        PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded());

        System.out.println("PUB=" + Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        System.out.println("PRIV=" + Base64.getEncoder().encodeToString(privateKey.getEncoded()));

        KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
        PublicKey pubKeyu =  kf.generatePublic(publicKey);
        PrivateKey privKey =  kf.generatePrivate(privateKey);
    }
}
