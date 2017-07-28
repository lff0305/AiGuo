package org.lff;

/**
 * @author Feifei Liu
 * @datetime Jul 28 2017 10:35
 */
public class EncDecUtility {

    public static String encrypt(String key, String message) {
        String publicKey = Configuration.getData().getString("public");
        return key;
    }

    public static void main(String[] arg) {
        encrypt("", "");
    }
}

