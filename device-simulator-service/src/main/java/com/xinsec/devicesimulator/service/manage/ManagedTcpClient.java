package com.xinsec.devicesimulator.service.manage;

import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ManagedTcpClient {

    private final EventLoopGroup workerGroup;
    private final String host;
    private final int port;
    private final String localAddress;
    private final long reconnectDelay;
    private final List<ProtocolHandler.MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final Bootstrap bootstrap;
    private Channel channel;

    public ManagedTcpClient(EventLoopGroup workerGroup, String host, int port, String localAddress, long reconnectDelay) {
        this.workerGroup = workerGroup;
        this.host = host;
        this.port = port;
        this.localAddress = localAddress;
        this.reconnectDelay = reconnectDelay > 0 ? reconnectDelay : 5000;

        this.bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ClientInboundHandler());
                    }
                });
    }

    public void start() {
        if (channel != null && channel.isActive() || workerGroup.isShutdown()) {
            log.debug("TCP客户端已连接或正在关闭，跳过连接尝试。");
            return;
        }

        ChannelFuture connectFuture;
        if (StringUtils.hasText(localAddress)) {
            if (!isLocalAddressValid()) {
                log.error("配置的本地地址 '{}' 无效。将在 {} 毫秒后重试。", localAddress, reconnectDelay);
                workerGroup.schedule(this::start, reconnectDelay, TimeUnit.MILLISECONDS);
                return;
            }
            connectFuture = bootstrap.connect(new InetSocketAddress(host, port), new InetSocketAddress(localAddress, 0));
            log.info("共享TCP客户端尝试从本地地址 {} 连接到 {}:{}...", host, port, localAddress);
        } else {
            connectFuture = bootstrap.connect(host, port);
            log.info("共享TCP客户端尝试连接到 {}:{}...", host, port);
        }

        connectFuture.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                this.channel = future.channel();
                log.info("成功连接共享TCP客户端到 {}:{} (本地: {})。", host, port, future.channel().localAddress());
            } else {
                log.warn("连接共享TCP客户端到 {}:{} 失败。将在 {} 毫秒后重试。原因: {}", host, port, reconnectDelay, future.cause().getMessage());
                future.channel().eventLoop().schedule(this::start, reconnectDelay, TimeUnit.MILLISECONDS);
            }
        });
    }

    public void stop() {
        log.info("正在停止连接到 {}:{} (本地地址: {}) 的共享TCP客户端", host, port, localAddress);
        if (channel != null) {
            channel.close();
        }
    }

    public void send(byte[] data) {
        if (isActive()) {
            channel.writeAndFlush(Unpooled.wrappedBuffer(data));
        } else {
            log.warn("尝试在未激活的客户端通道上发送数据 (目标: {}:{})", host, port);
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
        return channel != null && channel.isActive();
    }

    private boolean isLocalAddressValid() {
        // 如果未指定地址，则视为有效
        if (!StringUtils.hasText(this.localAddress)) return true;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().equals(this.localAddress)) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取网络接口以验证本地地址时出错", e);
            return true; // 出现异常时，为避免阻断连接，让后续的connect尝试去失败
        }
        return false;
    }

    private class ClientInboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                buf.release();
                for (ProtocolHandler.MessageListener listener : listeners) {
                    try {
                        listener.onMessageReceived(null, bytes.clone());
                    } catch (Exception e) {
                        log.error("向监听器 {} 分发消息时出错", listener.getClass().getName(), e);
                    }
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.warn("共享TCP客户端与 {}:{} 的连接已断开。将在 {} 毫秒后尝试重连...", host, port, reconnectDelay);
            ctx.channel().eventLoop().schedule(ManagedTcpClient.this::start, reconnectDelay, TimeUnit.MILLISECONDS);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("共享TCP客户端 {}:{} 捕获到异常:", host, port, cause);
            ctx.close();
        }
    }
}
