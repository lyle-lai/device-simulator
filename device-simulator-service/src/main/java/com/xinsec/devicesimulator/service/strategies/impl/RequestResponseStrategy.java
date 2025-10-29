
package com.xinsec.devicesimulator.service.strategies.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.codecs.impl.HexCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import com.xinsec.devicesimulator.service.websocket.WebSocketLogService;
import com.xinsec.devicesimulator.service.websocket.dto.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 实现“请求-响应”模式的模拟策略。
 * 它会监听接收到的消息，并使用数据生成器查找并回复一个响应。
 */
@Slf4j
@ComponentType("request-response")
@Scope("prototype") // 确保每次获取都是新实例
public class RequestResponseStrategy implements SimulationStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProtocolHandler protocolHandler;
    private MessageCodec messageCodec;
    private DataGenerator dataGenerator;
    private Map<String, Object> generatorProperties;
    private WebSocketLogService webSocketLogService; // 新增：WebSocket日志服务
    private String profileName; // 新增：画像名称

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator, JsonNode generatorProps, WebSocketLogService logService, String profileName) {
        this.protocolHandler = protocol;
        this.messageCodec = codec;
        this.dataGenerator = generator;
        if (generatorProps != null) {
            this.generatorProperties = objectMapper.convertValue(generatorProps, Map.class);
        }
        this.webSocketLogService = logService; // 赋值
        this.profileName = profileName; // 赋值
    }

    @Override
    public void execute() {
        sendLog(Level.INFO, "启动请求-响应策略...");

        ProtocolHandler.MessageListener listener = (context, data) -> {
            try {
                // 1. 使用编解码器将收到的字节解码为业务数据Map
                Map<String, Object> requestMap = this.messageCodec.decode(data);
                if (requestMap == null || requestMap.isEmpty()) {
                    sendLog(Level.WARN, "解码后的请求为空或无效，忽略。");
                    return;
                }
                sendLog(Level.DEBUG, "收到并解码请求: " + requestMap);

                // 2. 获取十六进制字符串
                String requestJsonString = requestMap.get(HexCodec.HEX_DATA_KEY).toString();

                // 3. 使用数据生成器获取响应报文
                String responseJsonString = dataGenerator.generate(requestJsonString);

                if (responseJsonString == null) {
                    sendLog(Level.WARN, "数据生成器未为请求: " + requestJsonString + " 返回响应。");
                    return;
                }
                sendLog(Level.DEBUG, "生成响应: " + responseJsonString);

                // 5. 编码响应数据
                byte[] responseBytes = this.messageCodec.encode(responseJsonString, null);

                // 6. 通过协议处理器将响应发送回指定的客户端上下文
                this.protocolHandler.send(context, responseBytes);

            } catch (Exception e) {
                sendLog(Level.ERROR, "请求-响应策略监听器中发生错误: " + e.getMessage());
                log.error("请求-响应策略监听器中发生错误。", e); // 传统日志保留堆栈信息
            }
        };

        // 5. 启动协议处理器并传入我们的监听器
        this.protocolHandler.start(listener);
    }

    @Override
    public void stop() {
        sendLog(Level.INFO, "停止请求-响应策略。");
        if (this.protocolHandler != null) {
            this.protocolHandler.stop();
        }
    }

    // Helper method to send log messages via WebSocket
    private void sendLog(Level level, String message) {
        if (webSocketLogService != null) {
            webSocketLogService.sendLogMessage(new LogMessage(profileName, LocalDateTime.now(), level.name(), message));
        }
        // Also keep traditional logging for file/console output
        switch (level) {
            case INFO: log.info(message); break;
            case WARN: log.warn(message); break;
            case ERROR: log.error(message); break;
            case DEBUG: log.debug(message); break;
            case TRACE: log.trace(message); break;
        }
    }

    // Enum for log levels (copied from SimulationInstance for consistency)
    private enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
