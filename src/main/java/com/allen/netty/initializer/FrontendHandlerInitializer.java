package com.allen.netty.initializer;

import com.allen.netty.handler.HttpFrontendHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class FrontendHandlerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new HttpServerCodec()).addLast(new HttpObjectAggregator(1024 * 1024 * 1024)).addLast(new HttpFrontendHandler());
        //心跳600秒检测一次
        ch.pipeline().addLast(new IdleStateHandler(600, 600, 600, TimeUnit.SECONDS));


    }

}
