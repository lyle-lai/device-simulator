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

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            serverChannel = f.channel();
            log.info("TCP Server started and listening on port {}", port);

            // f.channel().closeFuture().sync(); // This would block the main thread
        } catch (InterruptedException e) {
            log.error("TCP Server interrupted during startup", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("TCP Server failed to start", e);
        }
    }

    @Override
    public void stop() {
        log.info("Stopping TCP Server on port {}", port);
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
        log.info("TCP Server stopped.");
    }

    @Override
    public void send(byte[] data) {
        // This method is not supported for a server as it lacks client context.
        log.warn("Attempted to send data via context-less send() on TcpServerHandler. This is not supported. Use send(context, data) instead.");
    }

    @Override
    public void send(Object context, byte[] data) {
        if (!(context instanceof ChannelHandlerContext)) {
            log.error("Invalid context type provided to TcpServerHandler.send(). Expected ChannelHandlerContext, got {}.", context.getClass().getName());
            return;
        }
        ChannelHandlerContext ctx = (ChannelHandlerContext) context;
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
        } else {
            log.warn("Attempted to send data to an inactive channel: {}", ctx.channel().remoteAddress());
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
                buf.release(); // Release the ByteBuf
                // Pass both context and data to the listener
                listener.onMessageReceived(ctx, bytes);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("Client connected: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("Client disconnected: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Exception in TCP server pipeline from client {}", ctx.channel().remoteAddress(), cause);
            ctx.close();
        }
    }
}
