package com.allen.netty.proxy;

import lombok.Data;

@Data
public class GatewayConfig {

    public static Integer listen;
    public static String remoteHost;
    public static Integer remotePort;
    public static Double permitsPerSecond;


    public static String jdbcUser;
    public static String jdbcPwd;
    public static String jdbcUrl;
    public static String jdbcDriver;

}
