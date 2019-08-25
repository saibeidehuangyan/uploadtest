package com.allen.netty.proxy;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String args[]) {
        /* Start your server */
        PropertyUtil.getPropertie();
        Options options = new Options();
        options.addOption("listen", true, "监听端口");
        options.addOption("remoteHost", true, "代理主机");
        options.addOption("remotePort", true, "代理端口");
        options.addOption("permitsPerSecond", true, "拉黑最大并发数");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            int listen = Integer.parseInt(cmd.getOptionValue("listen", GatewayConfig.listen.toString()));
            String remoteHost = cmd.getOptionValue("remoteHost",GatewayConfig.remoteHost);
            int remotePort = Integer.parseInt(cmd.getOptionValue("remotePort",GatewayConfig.remotePort.toString()));

            GatewayConfig.listen = listen;
            GatewayConfig.remoteHost = remoteHost;
            GatewayConfig.remotePort = remotePort;

            System.out.println("监听端口：" + listen);
            System.out.println("代理主机：" + remoteHost);
            System.out.println("代理端口：" + remotePort);

            NettyProxyServer server  = new NettyProxyServer(listen);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
