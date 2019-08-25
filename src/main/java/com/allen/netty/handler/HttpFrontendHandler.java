package com.allen.netty.handler;


import com.allen.netty.entity.SingletonUtils;
import com.allen.netty.initializer.BackendHandlerInitializer;
import com.allen.netty.proxy.GatewayConfig;
import com.allen.netty.utils.Md5Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.entity.Record;
import org.nutz.json.Json;
import org.nutz.lang.util.NutMap;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 *
 */
public class HttpFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    Logger logger = Logger.getLogger(getClass());

    private Channel outboundChannel;

    private static Jedis jedis=SingletonUtils.getRedis();
    private static Dao dao=SingletonUtils.getDao();



    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("FrontEnd Handler is Active!");
        super.channelActive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        logger.debug("Uri:" + msg.getUri());
        if (msg.getMethod() != HttpMethod.POST) {
            responseJson(ctx, msg, "{\"success\": false,\"code\": \"0001\",\n\"msg\": \"非POST请求，请求失败\",\"fee\": \"N\"}");
            logger.warn("非POST请求，请求失败");
            return;
        }

        if (msg instanceof FullHttpRequest) {
            String requestContent = getRequestContent(request);
            NutMap map = Json.fromJson(NutMap.class, requestContent);





            String ipAddr = msg.headers().get("X-Forwarded-For");
            if (ipAddr == null) {
                InetSocketAddress insocket = (InetSocketAddress) ctx.channel()
                        .remoteAddress();
                ipAddr = insocket.getAddress().getHostAddress();
            }

            Record ips =dao.fetch("mch_user", Cnd.where("merchant_no", "=", map.get("merchant_no")));
            if (ips==null){
                responseJson(ctx, msg, "{\"success\": false,\"code\": \"0005\",\n\"msg\": \"商户号错误，请求失败\",\"fee\": \"N\"}");
                logger.warn("用户商户号错误，请求失败");
                return;
            }else {
                //ip不在白名单内
                System.out.println("请求主机IP===="+ipAddr);
                boolean b1=ips.get("ip_white_list").toString().indexOf("127.0.0.1")==-1;

                boolean b2=ips.get("ip_white_list")==null;
                boolean b3=ips.get("ip_white_list").toString().indexOf(ipAddr)==-1;

                if (b1 && (b2 || b3)){
                    responseJson(ctx, msg, "{\"success\": false,\"code\": \"0006\",\n\"msg\": \"ip不在白名单，请求失败\",\"fee\": \"N\"}");
                    logger.warn("ip不在白名单，请求失败");
                    return;
                }
            }
            String api=jedis.get(map.getString("biz_code"));
            if (api==null){
//                Record record = getDao().fetch("system_config", Cnd.where("sys_type", "=", "API_TYPE").and("sys_key", "=", map.getString("biz_code")));
                Record record = dao.fetch("system_config", Cnd.where("sys_type", "=", "API_TYPE").and("sys_key", "=", map.getString("biz_code")));
                if (record==null){
                    responseJson(ctx, msg, "{\"orderId\": \""+map.get("order_id")+"\",\"success\": false,\"code\": \"0003\",\"msg\": \"接口类型错误\",\"fee\": \"N\"}");
                    logger.warn("接口类型错误");
                    return;
                }
                api= record.getString("sys_value");
                jedis.set(map.getString("biz_code"),api);
            }
            logger.info("接口类型============"+api);
            System.out.println("接口类型============"+api);
            //根据接口类型进行验签
            boolean falg=checkSign(map);//验签成功返回 true
            if (!falg){
                responseJson(ctx, msg, "{\"orderId\": \""+map.get("order_id")+"\",\"success\": false,\"code\": \"0002\",\"msg\": \"验签失败\",\"fee\": \"N\"}");
                logger.warn("验签失败");
                return;
            }

            //验签成功后进行转发
            bootstrap(ctx, request, map);
        } else {
            outboundChannel.writeAndFlush(msg);
        }
    }



    public void bootstrap(ChannelHandlerContext ctx, FullHttpRequest request, NutMap map) throws Exception{
        Channel inboundChannel = ctx.channel();
        Bootstrap bootstrap = new Bootstrap();
        ((Bootstrap) ((Bootstrap) bootstrap.group(inboundChannel.eventLoop())).channel(ctx.channel().getClass())).handler(new BackendHandlerInitializer(inboundChannel));
        ChannelFuture f = bootstrap.connect(GatewayConfig.remoteHost, GatewayConfig.remotePort);
        this.outboundChannel = f.channel();
        request.retain();
        String bizCode = map.getString("biz_code");
        request.setUri(jedis.get(bizCode));
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess())
                    outboundChannel.writeAndFlush(request);
                else
                    inboundChannel.close();
            }
        });

    }

    //验签成功返回 true
    public boolean checkSign(NutMap map){
        try {
            if (map.getString("biz_code").indexOf("EASY")==0){// 简单验签
                return checkEasy(map);//验签成功
            }else {//复杂验签
                return check(map);//验签成功
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;//验签失败
        }
    }

    public boolean check(NutMap map)throws Exception {
        StringBuffer sign=new StringBuffer();
//        Record merchant = getDao().fetch("mch_user", Cnd.where("merchant_no", "=", map.getString("merchant_no")));
        Record merchant = dao.fetch("mch_user", Cnd.where("merchant_no", "=", map.getString("merchant_no")));
        sign.append("{\"name\":\"");
        sign.append(map.getString("name"));
        sign.append("\",\"mobile\":\"");
        sign.append(map.getString("mobile"));
        sign.append("\",\"idcard\":\"");
        sign.append(map.getString("idcard"));
        sign.append("\",\"appkey\":\"");
        sign.append(merchant.getString("merchant_api_key"));
        sign.append("\"}");

        System.out.println("参与签名参数="+sign.toString());
        String si = Md5Utils.encrypt(sign.toString());
        System.out.println("签名="+si);
        return map.getString("sign").equals(si)&&merchant.getString("merchant_token").equals(merchant.get("merchant_token"));
    }

    public boolean checkEasy(NutMap map)throws Exception {
        StringBuffer sign=new StringBuffer();
//        Record merchant = getDao().fetch("mch_user", Cnd.where("merchant_no", "=", map.getString("merchant_no")));
        Record merchant = dao.fetch("mch_user", Cnd.where("merchant_no", "=", map.getString("merchant_no")));

        sign.append("{\"mobile\":\"");
        sign.append(map.getString("mobile"));
        sign.append("\",\"content\":\"");
        sign.append(map.getString("content"));
        sign.append("\",\"appkey\":\"");
        sign.append(merchant.getString("merchant_api_key"));
        sign.append("\"}");
        System.out.println("参与签名参数="+sign.toString());
        String si = Md5Utils.encrypt(sign.toString());
        System.out.println("签名="+si);
        return map.getString("sign").equals(si)&&merchant.getString("merchant_token").equals(merchant.get("merchant_token"));
    }

    /**
     * 响应HTTP的请求
     *
     * @param ctx
     * @param req
     * @param jsonStr
     * @throws UnsupportedEncodingException
     */
    private void responseJson(ChannelHandlerContext ctx, FullHttpRequest req, String jsonStr) throws UnsupportedEncodingException {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(jsonStr.getBytes("utf-8")));
        response.headers().set("Content-Type", "text/xml; charset=UTF-8");
        logger.warn("网关拦截后返回：" + jsonStr);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 响应HTTP的请求
     *
     * @param ctx
     * @param req
     * @param obj
     * @throws UnsupportedEncodingException
     */
    private void responseJson(ChannelHandlerContext ctx, FullHttpRequest req, Object obj) throws UnsupportedEncodingException {
        responseJson(ctx, req, Json.toJson(obj));
    }

    /**
     * 获取请求的内容
     *
     * @param request
     * @return
     */
    private String getRequestContent(FullHttpRequest request) {
        ByteBuf contentBuf = request.content();
        String content = contentBuf.toString(CharsetUtil.UTF_8);
        return content;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }



    public static void main(String[] args) {
        String mobile="18186471397";
        String content="18186471397";
        String appkey="18186471397";
        String sign =  "{\"mobile\":\""+ mobile +"\",\"content\":\""+content+"\",\"appkey\":\""+ appkey+"\"}";
        System.out.println("sign===="+sign);
    }
}
