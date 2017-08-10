package org.lff.plainsocks;

/**
 * Created by liuff on 2017/8/9 15:05
 */
public class RemoteConfig {
    public static String remote;
    public static String keyUri;
    public static String base;

    public static String getKeyURL() {
        return remote + "/" +  keyUri;
    }
}
