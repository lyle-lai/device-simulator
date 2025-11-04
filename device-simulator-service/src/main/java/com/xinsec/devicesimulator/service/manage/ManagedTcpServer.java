package com.xinsec.devicesimulator.service.manage;

import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ManagedTcpServer {

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final int port;
    private final List<ProtocolHandler.MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Channel> clientChannels = new CopyOnWriteArrayList<>(); // 用于跟踪所有客户端连接
    private Channel serverChannel;

    public ManagedTcpServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, int port) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.port = port;
    }

    public void start() {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ServerInboundHandler());
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            serverChannel = f.channel();
            log.info("共享TCP服务器已在端口 {} 启动", port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("共享TCP服务器在端口 {} 的启动过程中被中断", port, e);
        } catch (Exception e) {
            log.error("无法在端口 {} 启动共享TCP服务器", port, e);
        }
    }

    public void stop() {
        log.info("正在停止端口 {} 上的共享TCP服务器", port);
        clientChannels.forEach(Channel::close);
        clientChannels.clear();
        if (serverChannel != null) {
            serverChannel.close();
        }
        // 不在此处关闭 EventLoopGroup，因为它们是共享的
        log.info("端口 {} 上的共享TCP服务器已停止", port);
    }

    public void broadcast(byte[] data) {
        if (clientChannels.isEmpty()) {
            log.trace("端口 {} 上没有连接的客户端，跳过广播", port);
            return;
        }
        log.debug("向端口 {} 上的 {} 个客户端广播 {} 字节数据", port, clientChannels.size(), data.length);
        ByteBuf message = Unpooled.wrappedBuffer(data);
        for (Channel clientChannel : clientChannels) {
            if (clientChannel.isActive()) {
                clientChannel.writeAndFlush(message.retainedDuplicate());
            }
        }
    }

    public void addListener(ProtocolHandler.MessageListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ProtocolHandler.MessageListener listener) {
        this.listeners.remove(listener);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public boolean isActive() {
        return serverChannel != null && serverChannel.isActive();
    }

    private class ServerInboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                buf.release();
                for (ProtocolHandler.MessageListener listener : listeners) {
                    try {
                        // 传递上下文(ctx)以便能够响应
                        listener.onMessageReceived(ctx, bytes.clone());
                    } catch (Exception e) {
                        log.error("向监听器 {} 分发消息时出错", listener.getClass().getName(), e);
                    }
                }
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("客户端已连接到端口 {} 的共享服务器: {}", port, ctx.channel().remoteAddress());
            clientChannels.add(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("客户端已从端口 {} 的共享服务器断开: {}", port, ctx.channel().remoteAddress());
            clientChannels.remove(ctx.channel());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("端口 {} 的共享TCP服务器从客户端 {} 收到异常:", port, ctx.channel().remoteAddress(), cause);
            clientChannels.remove(ctx.channel());
            ctx.close();
        }
    }
}
