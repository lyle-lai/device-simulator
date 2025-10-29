
package com.xinsec.devicesimulator.service.strategies.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 实现“请求-响应”模式的模拟策略。
 * 它会监听接收到的消息，并根据预设的映射关系查找并回复一个响应。
 */
@Slf4j
@ComponentType("request-response")
public class RequestResponseStrategy implements SimulationStrategy {

    private RequestResponseStrategyProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProtocolHandler protocolHandler;
    private MessageCodec messageCodec;

    @Data
    public static class RequestResponseStrategyProperties {
        private List<Mapping> mappings;
    }

    @Data
    public static class Mapping {
        private Map<String, Object> request;
        private Map<String, Object> response;
    }

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator) {
        // 1. 从策略配置中提取 "properties" 子节点
        JsonNode propertiesNode = config.path("properties");

        // 2. 解析自身配置
        this.properties = objectMapper.convertValue(propertiesNode, RequestResponseStrategyProperties.class);
        if (this.properties == null || this.properties.getMappings() == null) {
            throw new IllegalArgumentException("RequestResponseStrategy requires a 'mappings' configuration inside 'properties'.");
        }
        // 3. 保存所需组件的引用
        this.protocolHandler = protocol;
        this.messageCodec = codec;
    }

    @Override
    public void execute() {
        log.info("Starting Request-Response strategy...");

        ProtocolHandler.MessageListener listener = (context, data) -> {
            // 1. 使用编解码器将收到的字节解码为业务数据Map
            Map<String, Object> requestMap = this.messageCodec.decode(data);
            if (requestMap == null || requestMap.isEmpty()) {
                log.warn("Decoded request is null or empty, ignoring.");
                return;
            }
            log.debug("Received and decoded request: {}", requestMap);

            // 2. 在映射列表中查找匹配的请求
            for (Mapping mapping : properties.getMappings()) {
                if (Objects.equals(mapping.getRequest(), requestMap)) {
                    log.info("Found matching request: {}. Sending response: {}", requestMap, mapping.getResponse());

                    // 3. 获取对应的响应Map，并编码为字节
                    byte[] responseBytes = this.messageCodec.encode(mapping.getResponse(), null);

                    // 4. 通过协议处理器将响应发送回指定的客户端上下文
                    this.protocolHandler.send(context, responseBytes);
                    return; // 处理完成，退出
                }
            }

            log.warn("No response mapping found for request: {}", requestMap);
        };

        // 5. 启动协议处理器并传入我们的监听器
        this.protocolHandler.start(listener);
    }

    @Override
    public void stop() {
        log.info("Stopping Request-Response strategy.");
        if (this.protocolHandler != null) {
            this.protocolHandler.stop();
        }
    }
}
