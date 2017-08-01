package org.lff;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Jul 31 2017 16:25
 */
public class SimpleAESCipher implements BytesCipher {

    private static final String KEY = "BR58LXQNtb5O6dN70wA6QA==";

    private static SecretKeySpec secretKey = null;

    static {
        secretKey = new SecretKeySpec(Base64.getDecoder().decode(KEY), "AES");
    }


    @Override
    public String encode(String source) {
        try {
            Cipher d = Cipher.getInstance("AES");
            d.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(d.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String decode(String encoded) {
        try {
            Cipher d = Cipher.getInstance("AES");
            d.init(Cipher.DECRYPT_MODE, secretKey);
            String r = new String(d.doFinal(Base64.getDecoder().decode(encoded)));
            return r;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] argu) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String s = "q23rasefasdfasdf";
        SimpleAESCipher c = new SimpleAESCipher();
        System.out.println(c.decode(c.encode(s)));
    }
}
