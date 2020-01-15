package com.github.serezhka.jap2s.receiver;

import com.github.serezhka.jap2s.receiver.handler.mirroring.MirroringHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class AirPlayReceiver implements Runnable {

    private final int port;
    private final MirroringHandler mirroringHandler;

    public AirPlayReceiver(int port, MirroringHandler mirroringHandler) {
        this.port = port;
        this.mirroringHandler = mirroringHandler;
    }

    @Override
    public void run() {
        var serverBootstrap = new ServerBootstrap();
        var bossGroup = eventLoopGroup();
        var workerGroup = eventLoopGroup();
        try {
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClass())
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) {
                            ch.pipeline().addLast(mirroringHandler);
                        }
                    })
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            var channelFuture = serverBootstrap.bind().sync();
            log.info("AirPlay server listening on port: {}", port);
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.info("AirPlay server interrupted");
        } finally {
            log.info("AirPlay server stopped");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private EventLoopGroup eventLoopGroup() {
        return Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    private Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }
}
