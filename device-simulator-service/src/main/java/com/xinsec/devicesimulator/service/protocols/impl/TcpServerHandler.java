package com.xinsec.devicesimulator.service.protocols.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Scope;

@ComponentType("tcp-server")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class TcpServerHandler implements ProtocolHandler {

    private int port;
    private MessageListener messageListener;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Override
    public void configure(JsonNode config) {
        JsonNode propertiesNode = config.path("properties");
        this.port = propertiesNode.path("port").asInt(18888);
    }

    @Override
    public void start(MessageListener listener) {
        this.messageListener = listener;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TcpServerInboundHandler(messageListener));
                        }
                    });

            // 绑定并开始接受传入的连接。
            ChannelFuture f = b.bind(port).sync();
            serverChannel = f.channel();
            log.info("TCP服务器已启动，正在监听端口 {}", port);

            // f.channel().closeFuture().sync(); // 这会阻塞主线程，因此被注释掉
        } catch (InterruptedException e) {
            log.error("TCP服务器启动过程中被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("TCP服务器启动失败", e);
        }
    }

    @Override
    public void stop() {
        log.info("正在停止TCP服务器，端口 {}", port);
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
        log.info("TCP服务器已停止。");
    }

    @Override
    public void send(byte[] data) {
        // 对于服务端，此方法不受支持，因为它缺少客户端上下文。
        log.warn("尝试在TcpServerHandler上通过无上下文的send()方法发送数据。此操作不受支持，请改用 send(context, data)。");
    }

    @Override
    public void send(Object context, byte[] data) {
        if (!(context instanceof ChannelHandlerContext)) {
            log.error("向TcpServerHandler.send()提供了无效的上下文类型。期望类型: ChannelHandlerContext, 实际类型: {}.", context.getClass().getName());
            return;
        }
        ChannelHandlerContext ctx = (ChannelHandlerContext) context;
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
        } else {
            log.warn("尝试向一个非活动通道发送数据: {}", ctx.channel().remoteAddress());
        }
    }

    @Slf4j
    private static class TcpServerInboundHandler extends ChannelInboundHandlerAdapter {
        private final MessageListener listener;

        public TcpServerInboundHandler(MessageListener listener) {
            this.listener = listener;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (listener != null && msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                buf.release(); // 释放ByteBuf
                // 将上下文和数据都传递给监听器
                listener.onMessageReceived(ctx, bytes);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("客户端已连接: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("客户端已断开连接: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("TCP服务器pipeline中来自客户端 {} 的异常", ctx.channel().remoteAddress(), cause);
            ctx.close();
        }
    }
}
