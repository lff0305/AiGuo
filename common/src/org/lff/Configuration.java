package org.lff;

import com.typesafe.config.ConfigFactory;

import java.io.File;

/**
 * @author Feifei Liu
 * @datetime Jul 28 2017 11:53
 */
public class Configuration {

    private static com.typesafe.config.Config data;

    static {
        File f = new File("local.conf");
        if (!f.exists()) {
            System.out.println(f.getAbsoluteFile() + " does not exist!!");
            System.exit(1);
        }
        data = ConfigFactory.parseFile(f);
    }

    public static com.typesafe.config.Config getData() {
        return data;
    }

    public static String getData(String key) {
        return data.getString(key);
    }
}
