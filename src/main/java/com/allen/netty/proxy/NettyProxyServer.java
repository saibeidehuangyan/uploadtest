package com.allen.netty.proxy;

import com.allen.netty.initializer.FrontendHandlerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Netty Server
 * @author Allen
 */
public class NettyProxyServer {

	private int listen = 80;
	public NettyProxyServer(){}
	
	public NettyProxyServer(int listen){
		this.listen = listen;
	}
	/**
	 * 启动 netty server
	 * @throws Exception
	 */
	public void start () throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup(8);
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new FrontendHandlerInitializer());
//            //检测客户端连接是否有效的参数
			bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
			//Linux内核默认的处理方式是当用户调用close()方法时，函数返回。在可能的情况下，尽量发送数据，不一定保证会有剩余的数据，造成的数据的不确定性。使用SO_LINGER可以堵塞close()的调用时间，直到数据完全发送。,暂时去掉
			bootstrap.childOption(ChannelOption.SO_LINGER,null);
			ChannelFuture future = bootstrap.bind(listen).sync();
			future.channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
}
