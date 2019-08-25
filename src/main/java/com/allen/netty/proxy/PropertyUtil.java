package com.allen.netty.proxy;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {

    public static void getPropertie() {
        Properties prop = new Properties();
        try {
            InputStream in = PropertyUtil.class.getResourceAsStream("/config.properties");
            prop.load(in);
            GatewayConfig.listen = Integer.valueOf(prop.getProperty("listen"));
            GatewayConfig.remoteHost = prop.getProperty("remoteHost");
            GatewayConfig.remotePort = Integer.valueOf(prop.getProperty("remotePort"));

            GatewayConfig.jdbcDriver = prop.getProperty("jdbc.driver");
            GatewayConfig.jdbcUrl = prop.getProperty("jdbc.url");
            GatewayConfig.jdbcUser = prop.getProperty("jdbc.username");
            GatewayConfig.jdbcPwd = prop.getProperty("jdbc.password");


            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
