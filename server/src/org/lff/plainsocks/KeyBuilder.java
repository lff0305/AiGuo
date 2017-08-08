package org.lff.plainsocks;

import java.util.Base64;

/**
 * @author Feifei Liu
 * @datetime Aug 08 2017 10:39
 */
public class KeyBuilder {

    private static byte[] key;

    public static void setKey(String str) {
        key = Base64.getDecoder().decode(str);
    }
}
