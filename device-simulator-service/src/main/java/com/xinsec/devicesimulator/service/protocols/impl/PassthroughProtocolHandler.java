package com.xinsec.devicesimulator.service.protocols.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 一个“无操作”的协议处理器。
 * 它不执行任何网络操作。
 */
@ComponentType("passthrough-protocol")
@Slf4j
public class PassthroughProtocolHandler implements ProtocolHandler {
    @Override
    public void configure(JsonNode config) {
        log.debug("PassthroughProtocolHandler 已配置。无操作。");
    }

    @Override
    public void start(MessageListener listener) {
        log.debug("PassthroughProtocolHandler 已启动。无操作。");
    }

    @Override
    public void stop() {
        log.debug("PassthroughProtocolHandler 已停止。无操作。");
    }

    @Override
    public void send(byte[] data) {
        log.trace("PassthroughProtocolHandler 收到 {} 字节数据，但不执行任何发送操作。", data.length);
    }

    @Override
    public void send(Object context, byte[] data) {
        log.debug("PassthroughProtocolHandler 已发送。无操作。");
    }
}