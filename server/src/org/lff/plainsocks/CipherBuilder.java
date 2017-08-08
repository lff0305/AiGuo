package org.lff.plainsocks;

import org.lff.BytesCipher;
import org.lff.SimpleAESCipher;

import javax.crypto.Cipher;
import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Aug 08 2017 10:37
 */
public class CipherBuilder {

    public static BytesCipher buildContentCipher(byte[] key) {
        return new SimpleAESCipher(key);
    }
}
