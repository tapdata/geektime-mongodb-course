package org.geekbang.time.spark;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config extends Properties {
    public Config() {
        InputStream configStream = Config.class.getResourceAsStream("/config.properties");
        try {
            this.load(configStream);
        } catch(IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
}
