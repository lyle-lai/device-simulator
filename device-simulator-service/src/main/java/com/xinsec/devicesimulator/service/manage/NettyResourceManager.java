package com.xinsec.devicesimulator.service.manage;

import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@Slf4j
public class NettyResourceManager {

    private final ConcurrentHashMap<Integer, ManagedTcpServer> activeServers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ManagedTcpClient> activeClients = new ConcurrentHashMap<>();

    // Boss线程组，用于接受客户端连接
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // Worker线程组，用于处理网络I/O操作
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public void acquireServer(int port, ProtocolHandler.MessageListener listener) {
        activeServers.compute(port, (p, server) -> {
            if (server == null || !server.isActive()) {
                server = new ManagedTcpServer(bossGroup, workerGroup, p);
                server.start();
            }
            server.addListener(listener);
            return server;
        });
    }

    public void releaseServer(int port, ProtocolHandler.MessageListener listener) {
        activeServers.computeIfPresent(port, (p, server) -> {
            server.removeListener(listener);
            if (server.getListenerCount() == 0) {
                server.stop();
                return null; // 从Map中移除
            }
            return server;
        });
    }

    public void acquireClient(String host, int port, String localAddress, long reconnectDelay, ProtocolHandler.MessageListener listener) {
        String key = getClientKey(host, port, localAddress);
        activeClients.compute(key, (k, client) -> {
            if (client == null || !client.isActive()) {
                client = new ManagedTcpClient(workerGroup, host, port, localAddress, reconnectDelay);
                client.start();
            }
            client.addListener(listener);
            return client;
        });
    }

    public void releaseClient(String host, int port, String localAddress, ProtocolHandler.MessageListener listener) {
        String key = getClientKey(host, port, localAddress);
        activeClients.computeIfPresent(key, (k, client) -> {
            client.removeListener(listener);
            if (client.getListenerCount() == 0) {
                client.stop();
                return null; // 从Map中移除
            }
            return client;
        });
    }

    public void sendToClient(String host, int port, String localAddress, byte[] data) {
        String key = getClientKey(host, port, localAddress);
        ManagedTcpClient client = activeClients.get(key);
        if (client != null && client.isActive()) {
            client.send(data);
        } else {
            log.warn("尝试向一个不存在或未激活的客户端发送数据: {}", key);
        }
    }

    public void broadcastToServer(int port, byte[] data) {
        ManagedTcpServer server = activeServers.get(port);
        if (server != null && server.isActive()) {
            server.broadcast(data);
        } else {
            log.warn("尝试向一个不存在或未激活的服务器广播数据: 端口 {}", port);
        }
    }

    private String getClientKey(String host, int port, String localAddress) {
        return host + ":" + port + "@" + (StringUtils.hasText(localAddress) ? localAddress : "default");
    }

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭 NettyResourceManager...");
        activeServers.values().forEach(ManagedTcpServer::stop);
        activeClients.values().forEach(ManagedTcpClient::stop);
        activeServers.clear();
        activeClients.clear();

        Future<?> bossShutdownFuture = bossGroup.shutdownGracefully();
        Future<?> workerShutdownFuture = workerGroup.shutdownGracefully();

        try {
            bossShutdownFuture.get();
            workerShutdownFuture.get();
            log.info("Netty EventLoopGroup 已成功关闭。");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("关闭 Netty EventLoopGroup 期间被中断", e);
        } catch (ExecutionException e) {
            log.error("关闭 Netty EventLoopGroup 时发生错误", e);
        }
    }
}
