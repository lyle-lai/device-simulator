package com.xinsec.devicesimulator.service.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.config.SimulationProfile;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import com.xinsec.devicesimulator.service.websocket.WebSocketLogService;
import com.xinsec.devicesimulator.service.websocket.dto.LogMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 代表一个可运行的、独立的模拟设备实例。
 * <p>
 * 每个 {@code SimulationInstance} 都由一个 {@link SimulationProfile} 配置，该配置定义了其全部行为。
 * 在构造时，它使用 {@link ComponentFactory} 根据配置动态地创建和组装所需的组件，
 * 包括协议处理器 ({@link ProtocolHandler})、模拟策略 ({@link SimulationStrategy})、
 * 消息编解码器 ({@link MessageCodec}) 和数据生成器 ({@link DataGenerator})。
 * <p>
 * 此类的实例是可运行的 (implements {@link Runnable})，其核心逻辑由注入的 {@link SimulationStrategy} 驱动。
 * 它由 {@link SimulationManager} 创建和管理。
 */
@Slf4j
public class SimulationInstance implements Runnable {

    @Getter // 为 profile 添加 Getter，以便控制器可以访问
    private final SimulationProfile profile;
    private final SimulationStrategy strategy;
    private final WebSocketLogService webSocketLogService; // 新增字段，用于WebSocket日志服务

    public SimulationInstance(SimulationProfile profile, ComponentFactory factory, WebSocketLogService webSocketLogService) { // 更新后的构造函数
        this.profile = profile;
        this.webSocketLogService = webSocketLogService; // 初始化日志服务字段

        // 从配置中为每个层提取JsonNode
        JsonNode protocolNode = profile.getProtocol();
        JsonNode codecNode = profile.getCodec();
        JsonNode strategyNode = profile.getStrategy();
        // 数据生成器现在是顶级组件
        JsonNode generatorNode = profile.getDataGenerator();

        // 1. 使用工厂创建各个组件
        ProtocolHandler protocolHandler = factory.createProtocolHandler(getType(protocolNode));
        MessageCodec messageCodec = factory.createMessageCodec(getType(codecNode));
        this.strategy = factory.createSimulationStrategy(getType(strategyNode));
        DataGenerator dataGenerator = factory.createDataGenerator(getType(generatorNode));

        // 2. 配置协议处理器（它管理自身的生命周期和连接）
        protocolHandler.configure(protocolNode);

        // 3. 配置主策略，并注入其他组件
        // 策略现在会接收数据生成器的属性，以作为上下文传递
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
            log.error("启动模拟实例 '{}' 时发生错误。", profile.getProfileName(), e); // 保留传统日志记录，用于输出完整的堆栈跟踪信息
        }
    }

    public void stop() {
        sendLog(Level.INFO, "正在停止模拟实例: " + profile.getProfileName());
        try {
            strategy.stop();
            sendLog(Level.INFO, "模拟实例 '" + profile.getProfileName() + "' 已成功停止。");
        } catch (Exception e) {
            sendLog(Level.ERROR, "停止模拟实例 '" + profile.getProfileName() + "' 时发生错误: " + e.getMessage());
            log.error("停止模拟实例 '{}' 时发生错误。", profile.getProfileName(), e); // 保留传统日志记录，用于输出完整的堆栈跟踪信息
        }
    }

    // 通过WebSocket发送日志消息的辅助方法
    private void sendLog(Level level, String message) {
        webSocketLogService.sendLogMessage(new LogMessage(profile.getProfileName(), LocalDateTime.now(), level.name(), message));
        // 同时保留传统日志记录，用于文件或控制台输出
        switch (level) {
            case INFO: log.info(message); break;
            case WARN: log.warn(message); break;
            case ERROR: log.error(message); break;
            case DEBUG: log.debug(message); break;
            case TRACE: log.trace(message); break;
        }
    }

    // 日志级别枚举
    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}