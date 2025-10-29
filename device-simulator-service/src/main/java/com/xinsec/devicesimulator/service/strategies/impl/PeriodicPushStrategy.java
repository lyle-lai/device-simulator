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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ComponentType("periodic-push")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class PeriodicPushStrategy implements SimulationStrategy {

    private long intervalMillis;
    private ProtocolHandler protocolHandler;
    private MessageCodec messageCodec;
    private DataGenerator dataGenerator;
    private Map<String, Object> generatorProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketLogService webSocketLogService; // 新增：WebSocket日志服务
    private String profileName; // 新增：画像名称

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator, JsonNode generatorProps, WebSocketLogService logService, String profileName) {
        this.intervalMillis = config.path("properties").path("intervalMillis").asLong(5000);
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
        protocolHandler.start(null);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(this::generateAndSendData, 0, intervalMillis, TimeUnit.MILLISECONDS);
        sendLog(Level.INFO, "定时推送策略已启动。每 " + intervalMillis + " 毫秒推送一次数据。");
    }

    @Override
    public void stop() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        protocolHandler.stop();
        sendLog(Level.INFO, "定时推送策略已停止。");
    }

    private void generateAndSendData() {
        try {
            // 1. 生成数据，将properties作为上下文传入
            String data = dataGenerator.generate(generatorProperties);
            if (data == null) {
                sendLog(Level.TRACE, "数据生成器未返回数据，跳过本次发送。");
                return;
            }
            sendLog(Level.DEBUG, "生成数据: " + data);

            // 2. 编码数据
            byte[] encodedData = messageCodec.encode(data, null);
            if (encodedData == null || encodedData.length == 0) {
                sendLog(Level.TRACE, "编码器未返回数据，跳过本次发送。");
                return;
            }
            sendLog(Level.DEBUG, "编码后数据长度: " + encodedData.length);

            // 3. 发送数据
            protocolHandler.send(encodedData);

        } catch (Exception e) {
            sendLog(Level.ERROR, "定时推送任务执行时发生错误。" + e.getMessage());
            log.error("定时推送任务执行时发生错误。", e); // 传统日志保留堆栈信息
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
