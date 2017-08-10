package org.lff.plainsocks;

import org.lff.Configuration;

/**
 * Created by liuff on 2017/8/9 15:05
 */
public class RemoteConfig {
    public static String remote;
    public static String keyUri;
    public static String base;

    public static String getKeyURL() {
        return remote + "/" +  base + "/" + keyUri;
    }

    public static void init() {
        String remote = Configuration.getData("remote");
        RemoteConfig.remote = remote;

        String base = Configuration.getData("uri.base");
        RemoteConfig.base = base;

        String keyUri = Configuration.getData("uri.key");
        RemoteConfig.keyUri = keyUri;

    }
}
