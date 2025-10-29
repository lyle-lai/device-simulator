package com.xinsec.devicesimulator.service.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import com.xinsec.devicesimulator.service.websocket.WebSocketLogService;
import com.xinsec.devicesimulator.service.websocket.dto.LogMessage;
import lombok.Getter; // Add Getter for profile
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class SimulationInstance implements Runnable {

    @Getter // Add Getter for profile to be accessible by controllers
    private final SimulationProfile profile;
    private final SimulationStrategy strategy;
    private final WebSocketLogService webSocketLogService; // New field

    public SimulationInstance(SimulationProfile profile, ComponentFactory factory, WebSocketLogService webSocketLogService) { // Updated constructor
        this.profile = profile;
        this.webSocketLogService = webSocketLogService; // Initialize new field

        // From configuration extract JsonNode for each layer
        JsonNode protocolNode = profile.getProtocol();
        JsonNode codecNode = profile.getCodec();
        JsonNode strategyNode = profile.getStrategy();
        // 数据生成器现在是顶级组件
        JsonNode generatorNode = profile.getDataGenerator();

        // 1. Use factory to create components
        ProtocolHandler protocolHandler = factory.createProtocolHandler(getType(protocolNode));
        MessageCodec messageCodec = factory.createMessageCodec(getType(codecNode));
        this.strategy = factory.createSimulationStrategy(getType(strategyNode));
        DataGenerator dataGenerator = factory.createDataGenerator(getType(generatorNode));

        // 2. Configure protocol handler (it manages its own lifecycle and connections)
        protocolHandler.configure(protocolNode);

        // 3. Configure main strategy and inject other components
        // The strategy will now receive the dataGenerator's properties to pass them as context
        this.strategy.configure(strategyNode, protocolHandler, messageCodec, dataGenerator, getProperties(generatorNode), webSocketLogService, profile.getProfileName());
    }

    private String getType(JsonNode node) {
        if (node == null || !node.has("type") || !node.get("type").isTextual()) {
            // 默认返回一个无操作的组件类型，以增强健壮性
            return "passthrough";
        }
        return node.get("type").asText();
    }

    private JsonNode getProperties(JsonNode node) {
        if (node == null || !node.has("properties")) {
            return null;
        }
        return node.get("properties");
    }

    @Override
    public void run() {
        sendLog(Level.INFO, "启动模拟实例: " + profile.getProfileName());
        try {
            strategy.execute();
            sendLog(Level.INFO, "模拟实例 '" + profile.getProfileName() + "' 已成功启动。");
        } catch (Exception e) {
            sendLog(Level.ERROR, "启动模拟实例 '" + profile.getProfileName() + "' 时发生错误: " + e.getMessage());
            log.error("启动模拟实例 '{}' 时发生错误。", profile.getProfileName(), e); // Keep traditional logging for stack trace
        }
    }

    public void stop() {
        sendLog(Level.INFO, "正在停止模拟实例: " + profile.getProfileName());
        try {
            strategy.stop();
            sendLog(Level.INFO, "模拟实例 '" + profile.getProfileName() + "' 已成功停止。");
        } catch (Exception e) {
            sendLog(Level.ERROR, "停止模拟实例 '" + profile.getProfileName() + "' 时发生错误: " + e.getMessage());
            log.error("停止模拟实例 '{}' 时发生错误。", profile.getProfileName(), e); // Keep traditional logging for stack trace
        }
    }

    // Helper method to send log messages via WebSocket
    private void sendLog(Level level, String message) {
        webSocketLogService.sendLogMessage(new LogMessage(profile.getProfileName(), LocalDateTime.now(), level.name(), message));
        // Also keep traditional logging for file/console output
        switch (level) {
            case INFO: log.info(message); break;
            case WARN: log.warn(message); break;
            case ERROR: log.error(message); break;
            case DEBUG: log.debug(message); break;
            case TRACE: log.trace(message); break;
        }
    }

    // Enum for log levels
    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}