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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 原始数据回放策略。
 * 该策略根据配置的原始消息列表，以顺序或随机模式定时发送这些消息。
 *
 * @deprecated 废弃。其功能现在可以通过结合使用 {@link PeriodicPushStrategy}
 *             和自定义的 {@link com.xinsec.devicesimulator.service.datagen.DataGenerator} 来实现，
 *             从而提供更灵活和统一的模拟配置方式。
 */
@Deprecated
@ComponentType("raw-replay")
@Slf4j
@Scope("prototype") // 确保每次获取都是新实例
public class RawReplayStrategy implements SimulationStrategy {

    private long intervalMillis; // 消息发送间隔（毫秒）
    private String replayMode; // 回放模式：sequential（顺序）或 random（随机）
    private List<Message> messages; // 要回放的消息列表
    private ProtocolHandler protocolHandler; // 协议处理器
    private WebSocketLogService webSocketLogService; // 新增：WebSocket日志服务
    private String profileName; // 新增：画像名称

    private ScheduledExecutorService scheduler; // 定时任务执行器
    private ScheduledFuture<?> scheduledFuture; // 定时任务的Future
    private final AtomicInteger sequentialCounter = new AtomicInteger(0); // 顺序模式下的计数器

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator, JsonNode generatorProperties, WebSocketLogService logService, String profileName) {
        // 从配置中获取消息发送间隔
        this.intervalMillis = config.path("properties").path("intervalMillis").asLong(3000);
        // 从配置中获取回放模式
        this.replayMode = config.path("properties").path("replayMode").asText("sequential");
        this.protocolHandler = protocol;
        this.webSocketLogService = logService; // 赋值
        this.profileName = profileName; // 赋值

        // 如果配置中包含消息列表，则解析并存储
        if (config.path("properties").has("messages")) {
            this.messages = StreamSupport.stream(config.path("properties").get("messages").spliterator(), false)
                    .map(Message::new)
                    .collect(Collectors.toList());
        } else {
            // 如果没有配置消息列表，则初始化为空列表并记录警告
            this.messages = new ArrayList<>();
            sendLog(Level.WARN, "RawReplayStrategy 配置中缺少 'messages' 属性. 该策略将不会执行任何操作.");
        }
    }

    @Override
    public void execute() {
        // 如果没有要回放的消息，则不启动策略
        if (messages.isEmpty()) {
            sendLog(Level.WARN, "没有可回放的消息. RawReplayStrategy 将不会启动。");
            return;
        }
        // 启动协议处理器，无需消息监听器
        protocolHandler.start(null);

        // 创建并启动定时任务，按指定间隔发送消息
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(this::selectAndSendData, 0, intervalMillis, TimeUnit.MILLISECONDS);
        sendLog(Level.INFO, "RawReplayStrategy 已启动. 回放模式: '" + replayMode + "', 间隔: " + intervalMillis + " 毫秒.");
    }

    @Override
    public void stop() {
        // 停止定时任务
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        // 停止协议处理器
        protocolHandler.stop();
        sendLog(Level.INFO, "RawReplayStrategy 已停止。");
    }

    /**
     * 根据回放模式选择消息并发送。
     */
    private void selectAndSendData() {
        try {
            Message message; // 待发送的消息
            // 根据回放模式选择消息
            if ("random".equalsIgnoreCase(replayMode)) {
                // 随机模式：从消息列表中随机选择一条
                message = messages.get(new Random().nextInt(messages.size()));
            } else { // sequential
                // 顺序模式：按顺序选择消息，循环回放
                message = messages.get(sequentialCounter.getAndIncrement() % messages.size());
            }

            // 解码消息内容为字节数组
            byte[] rawData = message.decode();
            sendLog(Level.DEBUG, "正在回放消息 (" + rawData.length + " 字节)");
            // 通过协议处理器发送原始数据
            protocolHandler.send(rawData);

        } catch (Exception e) {
            // 捕获并记录回放执行过程中的错误
            sendLog(Level.ERROR, "原始数据回放执行过程中发生错误." + e.getMessage());
            log.error("原始数据回放执行过程中发生错误.", e); // 传统日志保留堆栈信息
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

    /**
     * 内部类，表示一个待回放的消息及其编码方式。
     */
    private static class Message {
        final String encoding; // 消息编码方式 (hex, base64, utf8/string)
        final String data; // 消息的原始数据字符串

        /**
         * 构造函数，从JsonNode解析消息配置。
         * @param node 包含消息编码和数据的JsonNode
         */
        Message(JsonNode node) {
            this.encoding = node.path("encoding").asText("utf8");
            this.data = node.path("data").asText("");
        }

        /**
         * 根据编码方式将消息数据解码为字节数组。
         * @return 解码后的字节数组
         */
        byte[] decode() {
            String lowerCaseEncoding = encoding.toLowerCase();
            if ("hex".equals(lowerCaseEncoding)) {
                // 十六进制字符串解码
                return hexStringToByteArray(data);
            } else if ("base64".equals(lowerCaseEncoding)) {
                // Base64字符串解码
                return Base64.getDecoder().decode(data);
            } else if ("utf8".equals(lowerCaseEncoding) || "string".equals(lowerCaseEncoding)) {
                // UTF-8 或普通字符串解码
                return data.getBytes(StandardCharsets.UTF_8);
            } else {
                // 不支持的编码方式，记录警告并按普通字符串处理
//                sendLog(Level.WARN, "不支持的编码方式 '" + encoding + "'. 将按普通字符串处理.");
                return data.getBytes(StandardCharsets.UTF_8);
            }
        }

        /**
         * 将十六进制字符串转换为字节数组。
         * @param s 十六进制字符串
         * @return 字节数组
         */
        private static byte[] hexStringToByteArray(String s) {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                     + Character.digit(s.charAt(i+1), 16));
            }
            return data;
        }
    }
}
