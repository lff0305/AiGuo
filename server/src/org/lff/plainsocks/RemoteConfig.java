package org.lff.plainsocks;

import org.lff.Configuration;

/**
 * Created by liuff on 2017/8/9 15:05
 */
public class RemoteConfig {
    private static String remote;
    private static String keyUri;
    private static String base;
    private static String connectUri;
    private static String postUri;
    private static String fetchUri;
    private static String closeUri;

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

        String connectUri = Configuration.getData("uri.connect");
        RemoteConfig.connectUri = connectUri;

        String postUri = Configuration.getData("uri.post");
        RemoteConfig.postUri = postUri;

        String fetchUri = Configuration.getData("uri.fetch");
        RemoteConfig.fetchUri = fetchUri;

        String closeUri = Configuration.getData("uri.close");
        RemoteConfig.closeUri = closeUri;
    }

    public static String getConnectURL() {
        return remote + "/" +  base + "/" + connectUri;
    }

    public static String getPostURL() {
        return remote + "/" +  base + "/" + postUri;
    }

    public static String getFetchURL() {
        return remote + "/" +  base + "/" + fetchUri;
    }
    public static String getCloseURL() {
        return remote + "/" +  base + "/" + closeUri;
    }
}
