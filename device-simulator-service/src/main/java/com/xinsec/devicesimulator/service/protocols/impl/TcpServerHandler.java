package com.xinsec.devicesimulator.service.protocols.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.manage.NettyResourceManager;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

@ComponentType("tcp-server")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class TcpServerHandler implements ProtocolHandler {

    private int port;
    private MessageListener messageListener;

    @Autowired
    private NettyResourceManager nettyResourceManager;

    @Override
    public void configure(JsonNode config) {
        JsonNode propertiesNode = config.path("properties");
        this.port = propertiesNode.path("port").asInt(18888);
    }

    @Override
    public void start(MessageListener listener) {
        this.messageListener = listener;
        log.info("请求端口 {} 上的共享TCP服务器", port);
        nettyResourceManager.acquireServer(port, this.messageListener);
    }

    @Override
    public void stop() {
        log.info("释放端口 {} 上的共享TCP服务器", port);
        if (this.messageListener != null) {
            nettyResourceManager.releaseServer(port, this.messageListener);
        }
    }

    @Override
    public void send(byte[] data) {
        // 对于服务端，无上下文的send被定义为向所有连接的客户端广播
        log.debug("广播 {} 字节数据到端口 {} 上的所有客户端", data.length, port);
        nettyResourceManager.broadcastToServer(port, data);
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
}
