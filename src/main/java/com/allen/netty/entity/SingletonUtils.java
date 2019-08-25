package com.allen.netty.entity;

import com.alibaba.druid.pool.DruidDataSource;
import com.allen.netty.proxy.GatewayConfig;
import org.nutz.dao.Dao;
import org.nutz.dao.impl.NutDao;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SingletonUtils {

    private static Jedis jedis = null;
    private static Dao dao = null;

    private SingletonUtils() {
    }

    public static Jedis getRedis(){

        if (jedis==null){
            InputStream resource = null;
            try {
                resource = SingletonUtils.class.getResourceAsStream("/redis.properties");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Properties properties= new Properties() ;
            try {
                properties.load(resource);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String host=properties.getProperty("redis.host");
            int port= Integer.parseInt(properties.getProperty("redis.port"));
            jedis=new Jedis(host,port);
            System.out.println("创建一个redis");
            jedis.auth(properties.getProperty("redis.password"));
        }
        return jedis;
    }

    public static Dao getDao() {
        if (dao == null) {
            DruidDataSource ds = new DruidDataSource();
            ds.setDriverClassName(GatewayConfig.jdbcDriver);
            ds.setUrl(GatewayConfig.jdbcUrl);
            ds.setUsername(GatewayConfig.jdbcUser);
            ds.setPassword(GatewayConfig.jdbcPwd);
            System.out.println("创建一个dao");
            dao = new NutDao(ds);
        }
        return dao;
    }

}
