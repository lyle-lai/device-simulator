package com.xinsec.devicesimulator.service.strategies.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
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

/**
 * 一个“无操作”的策略。
 */
@ComponentType("passthrough-strategy")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class PassthroughStrategy implements SimulationStrategy {

    private WebSocketLogService webSocketLogService; // 新增：WebSocket日志服务
    private String profileName; // 新增：画像名称

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator, JsonNode generatorProperties, WebSocketLogService logService, String profileName) {
        this.webSocketLogService = logService; // 赋值
        this.profileName = profileName; // 赋值
        log.debug("PassthroughStrategy 已配置。无操作。");
        sendLog(Level.DEBUG, "PassthroughStrategy 已配置。无操作。");
    }

    @Override
    public void execute() {
        log.debug("PassthroughStrategy 已执行。无操作。");
        sendLog(Level.DEBUG, "PassthroughStrategy 已执行。无操作。");
    }

    @Override
    public void stop() {
        log.debug("PassthroughStrategy 已停止。无操作。");
        sendLog(Level.DEBUG, "PassthroughStrategy 已停止。无操作。");
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