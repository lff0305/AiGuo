import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.*;
import java.security.interfaces.*;
import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Jul 28 2017 10:15
 */
public class TestEC {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    // See https://www.ietf.org/rfc/rfc5480.txt
    /**
       Minimum  | ECDSA    | Message    | Curves
       Bits of  | Key Size | Digest     |
       Security |          | Algorithms |
       ---------+----------+------------+-----------
       80       | 160-223  | SHA-1      | sect163k1
                | SHA-224  | secp163r2  |
                | SHA-256  | secp192r1  |
                | SHA-384  |            |
                | SHA-512  |            |
       ---------+----------+------------+-----------
       112      | 224-255  | SHA-224    | secp224r1
                | SHA-256  | sect233k1  |
                | SHA-384  | sect233r1  |
                | SHA-512  |            |
       ---------+----------+------------+-----------
       128      | 256-383  | SHA-256    | secp256r1
                | SHA-384  | sect283k1  |
                | SHA-512  | sect283r1  |
       ---------+----------+------------+-----------
       192      | 384-511  | SHA-384    | secp384r1
                | SHA-512  | sect409k1  |
                |          | sect409r1  |
       ---------+----------+------------+-----------
       256      | 512+     | SHA-512    | secp521r1
                |          | sect571k1  |
                |          | sect571r1  |
     * @param argu
     * @throws Exception
     */

    public static void main(String[] argu) throws Exception {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp521r1");
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");
        g.initialize(ecSpec, new SecureRandom());
        KeyPair pair = g.generateKeyPair();


        String pub = to(pair.getPublic());
        System.out.println("Build public = " + pub);
        String priv = to(pair.getPrivate());
        System.out.println("Build private = " + priv);
        ECPublicKey ecPublicKey = buildECPublic(pub);
        System.out.println("Build public = " + ecPublicKey);

        Cipher cipher = Cipher.getInstance("ECIES");
        cipher.init(Cipher.ENCRYPT_MODE, ecPublicKey);

        String toEncrypt = "Hello";

        // Check that cipher works ok
        byte[] enc = cipher.doFinal(toEncrypt.getBytes());

        System.out.println(Base64.getEncoder().encodeToString(enc));


        Cipher dec = Cipher.getInstance("ECIES");
        dec.init(Cipher.DECRYPT_MODE, pair.getPrivate());

        byte[] decoded = dec.doFinal(enc);
        System.out.println(new String(decoded));

    }

    private static String to(Key key) throws IOException {
        ByteOutputStream bos = new ByteOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(key);
        oos.close();
        return Base64.getEncoder().encodeToString(bos.getBytes());
    }

    private static ECPublicKey buildECPublic(String source) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(source);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        ECPublicKey key = (ECPublicKey)ois.readObject();
        return key;
    }

    private static ECPrivateKey buildECPrivate(String source) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(source);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        ECPrivateKey key = (ECPrivateKey)ois.readObject();
        return key;
    }
}
