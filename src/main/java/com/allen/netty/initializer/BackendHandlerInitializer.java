package com.allen.netty.initializer;

import com.allen.netty.handler.HttpBackendHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

/**
 *
 */
public class BackendHandlerInitializer extends ChannelInitializer<SocketChannel> {

    final Channel inboundChannel;

    public BackendHandlerInitializer(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new HttpClientCodec()).addLast(new HttpObjectAggregator(1024 * 1024 * 1024)).addLast(new HttpBackendHandler(inboundChannel));

    }


}
