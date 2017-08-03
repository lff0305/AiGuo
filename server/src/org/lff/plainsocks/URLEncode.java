package org.lff.plainsocks;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Feifei Liu
 * @datetime Aug 03 2017 12:34
 */
public class URLEncode {
    public static void main(String[] argu) throws UnsupportedEncodingException {
        String s = "i74hhfMJZ/fx3NKU/JhskUQt2fP6mfteMKo30vFEvVsqHzq9qHAWZ1xwiOqcoCIC";
        System.out.println(URLEncoder.encode(s, StandardCharsets.UTF_8.name()));
    }
}
