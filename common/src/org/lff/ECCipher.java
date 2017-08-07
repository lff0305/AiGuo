package org.lff;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Aug 07 2017 17:32
 */
public class ECCipher implements BytesCipher {


    @Override
    public String encode(String source) {
        return null;
    }

    @Override
    public String encode(byte[] source) {
        return null;
    }

    @Override
    public byte[] decodeBytes(String source) {
        return new byte[0];
    }

    @Override
    public String decode(String encoded) {
        return null;
    }


    public static void main(String[] argu) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {

        Security.addProvider(new BouncyCastleProvider());

        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp521r1");
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");
        g.initialize(ecSpec, new SecureRandom());
        KeyPair pair = g.generateKeyPair();

        X509EncodedKeySpec publicKey = new X509EncodedKeySpec(pair.getPublic().getEncoded());
        PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded());

        System.out.println("PUB=" + Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        System.out.println("PRIV=" + Base64.getEncoder().encodeToString(privateKey.getEncoded()));

        KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
        PublicKey pubKeyu =  kf.generatePublic(publicKey);
        PrivateKey privKey =  kf.generatePrivate(privateKey);
    }

}
