package com.xinsec.devicesimulator.service.protocols.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.manage.NettyResourceManager;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

@ComponentType("tcp-client")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class TcpClientHandler implements ProtocolHandler {

    private String host;
    private int port;
    private String localAddress;
    private long reconnectDelay;
    private MessageListener messageListener;

    @Autowired
    private NettyResourceManager nettyResourceManager;

    @Override
    public void configure(JsonNode config) {
        JsonNode propertiesNode = config.path("properties");
        this.host = propertiesNode.path("host").asText("localhost");
        this.port = propertiesNode.path("port").asInt(18888);
        this.localAddress = propertiesNode.path("localAddress").asText(null);
        this.reconnectDelay = propertiesNode.path("reconnectDelay").asLong(5000);
        log.info("配置TCP客户端: 目标 {}:{}, 本地地址 {}, 重连延迟 {}ms", host, port, localAddress, reconnectDelay);
    }

    @Override
    public void start(MessageListener listener) {
        this.messageListener = listener;
        log.info("请求共享TCP客户端: 目标 {}:{}, 本地地址 {}", host, port, localAddress);
        nettyResourceManager.acquireClient(host, port, localAddress, reconnectDelay, this.messageListener);
    }

    @Override
    public void stop() {
        log.info("释放共享TCP客户端: 目标 {}:{}, 本地地址 {}", host, port, localAddress);
        if (this.messageListener != null) {
            nettyResourceManager.releaseClient(host, port, localAddress, this.messageListener);
        }
    }

    @Override
    public void send(byte[] data) {
        log.trace("发送 {} 字节数据到 {}:{} (本地地址: {})", data.length, host, port, localAddress);
        nettyResourceManager.sendToClient(host, port, localAddress, data);
    }

    @Override
    public void send(Object context, byte[] data) {
        // 对于TCP客户端，context通常不用于发送，直接调用无context的send方法
        send(data);
    }
}
