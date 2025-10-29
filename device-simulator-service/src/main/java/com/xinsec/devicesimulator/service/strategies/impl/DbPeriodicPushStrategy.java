package com.xinsec.devicesimulator.service.strategies.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.codecs.impl.HexCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.entity.PayloadRepositoryEntity;
import com.xinsec.devicesimulator.service.mapper.PayloadRepositoryMapper;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 数据库驱动的周期性上报策略。
 * 从数据库中加载指定分组的报文，并周期性地随机选择一条进行发送。
 */
@Slf4j
@ComponentType("db-periodic-push")
@RequiredArgsConstructor
public class DbPeriodicPushStrategy implements SimulationStrategy {

    private final PayloadRepositoryMapper payloadMapper;
    private final ObjectMapper objectMapper;
    private MessageCodec messageCodec;

    private long intervalMillis;
    private String payloadGroup;
    private List<Map<String, Object>> payloadsCache;
    private final Random random = new Random();

    private ProtocolHandler protocolHandler;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator) {
        // 1. 解析策略自身配置
        JsonNode propertiesNode = config.path("properties");
        if (propertiesNode.isMissingNode()) {
            throw new IllegalArgumentException("DbPeriodicPushStrategy requires 'properties' configuration.");
        }
        this.intervalMillis = propertiesNode.path("intervalMillis").asLong(5000);
        this.payloadGroup = propertiesNode.path("payload_group").asText(null);

        if (this.payloadGroup == null || this.payloadGroup.isEmpty()) {
            throw new IllegalArgumentException("DbPeriodicPushStrategy requires a 'payload_group' in its properties.");
        }

        // 2. 保存所需组件的引用
        this.protocolHandler = protocol;
        this.messageCodec = codec;

        // 3. 从数据库加载报文，并构建缓存
        loadPayloads();
    }

    private void loadPayloads() {
        log.info("Loading payloads for group: {}", payloadGroup);
        QueryWrapper<PayloadRepositoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_key", payloadGroup);
        List<PayloadRepositoryEntity> entities = payloadMapper.selectList(queryWrapper);

        if (entities.isEmpty()) {
            log.warn("No payloads found for group: {}. DbPeriodicPushStrategy will be inactive.", payloadGroup);
            payloadsCache = Collections.emptyList();
            return;
        }

        payloadsCache = entities.stream()
                .map(this::parsePayloadContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Loaded {} payloads for group '{}'.", payloadsCache.size(), payloadGroup);
    }

    private Map<String, Object> parsePayloadContent(PayloadRepositoryEntity payloadEntity) {
        try {
            // 根据payloadType使用ObjectMapper解析内容
            if ("json".equalsIgnoreCase(payloadEntity.getPayloadType())) {
                return objectMapper.readValue(payloadEntity.getContent(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } else if ("hex".equalsIgnoreCase(payloadEntity.getPayloadType())) {
                // 对于hex类型，我们将其包装成一个Map，以便与MessageCodec兼容
                return Collections.singletonMap(HexCodec.HEX_DATA_KEY, payloadEntity.getContent());
            } else {
                log.error("Unsupported payload type: {} for payload key: {}", payloadEntity.getPayloadType(), payloadEntity.getPayloadKey());
                return null;
            }
        } catch (IOException e) {
            log.error("Error parsing payload content for payload key: {}", payloadEntity.getPayloadKey(), e);
            return null;
        }
    }

    @Override
    public void execute() {
        if (payloadsCache.isEmpty()) {
            log.warn("DbPeriodicPushStrategy for group '{}' has no payloads loaded. Not starting push task.", payloadGroup);
            return;
        }
        log.info("Starting DbPeriodicPushStrategy for group '{}'. Interval: {} ms.", payloadGroup, intervalMillis);

        // 启动协议层，准备发送数据
        // (对于推送策略，我们不关心接收数据，所以listener是null)
        protocolHandler.start(null);

        // 创建并启动定时任务
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(this::selectAndSendData, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        log.info("Stopping DbPeriodicPushStrategy for group '{}'.", payloadGroup);
        // 停止定时任务
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        // 停止协议层
        protocolHandler.stop();
    }

    private void selectAndSendData() {
        try {
            // 1. 随机选择一个报文
            Map<String, Object> selectedPayload = payloadsCache.get(random.nextInt(payloadsCache.size()));
            log.debug("Randomly selected payload for push: {}", selectedPayload);

            // 2. 编码数据
            byte[] encodedData = messageCodec.encode(selectedPayload, null); // metadata暂时为null
            if (encodedData == null || encodedData.length == 0) {
                log.warn("Encoded data is null or empty for payload: {}. Skipping send.", selectedPayload);
                return;
            }

            // 3. 发送数据
            protocolHandler.send(encodedData);

        } catch (Exception e) {
            // 捕获所有异常，防止定时任务中断
            log.error("DbPeriodicPushStrategy push task execution failed for group '{}'.", payloadGroup, e);
        }
    }
}
