package com.xinsec.devicesimulator.service.protocols.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Scope;

import java.util.concurrent.TimeUnit;

@ComponentType("tcp-client")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class TcpClientHandler implements ProtocolHandler {

    private String host;
    private int port;
    private long reconnectDelay;

    private final Bootstrap bootstrap;
    private final EventLoopGroup group;
    private Channel channel;

    public TcpClientHandler() {
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    @Override
    public void configure(JsonNode config) {
        JsonNode propertiesNode = config.path("properties");
        this.host = propertiesNode.path("host").asText("localhost");
        this.port = propertiesNode.path("port").asInt(18888);
        this.reconnectDelay = propertiesNode.path("reconnectDelay").asLong(5000);
    }

    @Override
    public void start(MessageListener listener) {
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new TcpInboundLogicHandler(listener, host, port, reconnectDelay, TcpClientHandler.this));
            }
        });
        log.info("TCP客户端启动，尝试连接 {}:{}...", host, port);
        doConnect();
    }

    private void doConnect() {
        if (channel != null && channel.isActive() || group.isShutdown()) {
            return;
        }
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                this.channel = future.channel();
                log.info("成功连接到TCP服务器 {}:{}", host, port);
            } else {
                log.warn("连接到 {}:{} 失败。将在 {} 毫秒后重试。", host, port, reconnectDelay, future.cause());
                future.channel().eventLoop().schedule(this::doConnect, reconnectDelay, TimeUnit.MILLISECONDS);
            }
        });
    }

    @Override
    public void stop() {
        log.info("正在停止TCP客户端并关闭到 {}:{} 的连接。", host, port);
        if (group != null && !group.isShutdown()) {
            group.shutdownGracefully();
        }
    }

    @Override
    public void send(byte[] data) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.wrappedBuffer(data)).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("向 {}:{} 发送数据失败。", host, port, future.cause());
                } else {
                    log.trace("成功发送 {} 字节数据。", data.length);
                }
            });
        } else {
            log.warn("无法发送数据，TCP通道未激活或未连接。");
        }
    }

    @Override
    public void send(Object context, byte[] data) {
        // 对于TCP客户端，context通常不用于发送，直接调用无context的send方法
        send(data);
    }

    @Slf4j
    private static class TcpInboundLogicHandler extends ChannelInboundHandlerAdapter {
        private final MessageListener listener;
        private final String host;
        private final int port;
        private final long reconnectDelay;
        private final TcpClientHandler outerInstance; // Reference to the outer class

        public TcpInboundLogicHandler(MessageListener listener, String host, int port, long reconnectDelay, TcpClientHandler outerInstance) {
            this.listener = listener;
            this.host = host;
            this.port = port;
            this.reconnectDelay = reconnectDelay;
            this.outerInstance = outerInstance;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (listener != null && msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                buf.release(); // 必须释放ByteBuf
                listener.onMessageReceived(ctx,bytes);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.warn("与 {}:{} 的TCP连接已断开。将在 {} 毫秒后尝试重连...", host, port, reconnectDelay);
            ctx.channel().eventLoop().schedule(outerInstance::doConnect, reconnectDelay, TimeUnit.MILLISECONDS);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("TCP客户端pipeline中捕获到异常", cause);
            ctx.close();
        }
    }
}
