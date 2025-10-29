package com.xinsec.devicesimulator.service.strategies.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.codecs.impl.HexCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.entity.PayloadRepositoryEntity;
import com.xinsec.devicesimulator.service.entity.RequestResponseRuleEntity;
import com.xinsec.devicesimulator.service.mapper.PayloadRepositoryMapper;
import com.xinsec.devicesimulator.service.mapper.RequestResponseRuleMapper;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据库驱动的请求-响应策略。
 * 根据数据库中配置的规则和报文，实现请求匹配和随机响应。
 */
@Slf4j
@ComponentType("db-request-response")
@RequiredArgsConstructor
public class DbRequestResponseStrategy implements SimulationStrategy {

    private final RequestResponseRuleMapper ruleMapper;
    private final PayloadRepositoryMapper payloadMapper;
    private final ObjectMapper objectMapper;
    private MessageCodec messageCodec; // 注入MessageCodec用于报文内容解析

    private String ruleGroup;
    // 存储解析后的规则: 请求报文Map -> 对应的响应报文Map列表
    private final Map<Map<String, Object>, List<Map<String, Object>>> rulesCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private ProtocolHandler protocolHandler;

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator) {
        // 1. 解析策略自身配置
        JsonNode propertiesNode = config.path("properties");
        if (propertiesNode.isMissingNode() || !propertiesNode.has("rule_group")) {
            throw new IllegalArgumentException("DbRequestResponseStrategy requires a 'rule_group' in its properties.");
        }
        this.ruleGroup = propertiesNode.get("rule_group").asText();

        // 2. 保存所需组件的引用
        this.protocolHandler = protocol;
         this.messageCodec = codec; // MessageCodec已通过构造函数注入

        // 3. 从数据库加载规则和报文，并构建缓存
        loadRulesAndPayloads();
    }

    private void loadRulesAndPayloads() {
        rulesCache.clear();
        log.info("Loading request-response rules for group: {}", ruleGroup);

        // 查询所有属于该规则组的规则
        QueryWrapper<RequestResponseRuleEntity> ruleQuery = new QueryWrapper<>();
        ruleQuery.eq("rule_group", ruleGroup);
        List<RequestResponseRuleEntity> rules = ruleMapper.selectList(ruleQuery);

        if (rules.isEmpty()) {
            log.warn("No request-response rules found for group: {}. Strategy will be inactive.", ruleGroup);
            return;
        }

        // 收集所有涉及到的payload keys
        Set<String> payloadKeys = rules.stream()
                .flatMap(rule -> Stream.of(rule.getRequestKey(), rule.getResponseKey()))
                .collect(Collectors.toSet());

        // 批量查询所有payload内容
        QueryWrapper<PayloadRepositoryEntity> payloadQuery = new QueryWrapper<>();
        payloadQuery.in("payload_key", payloadKeys);
        List<PayloadRepositoryEntity> payloads = payloadMapper.selectList(payloadQuery);
        Map<String, PayloadRepositoryEntity> payloadMap = payloads.stream()
                .collect(Collectors.toMap(PayloadRepositoryEntity::getPayloadKey, p -> p));

        // 构建规则缓存
        for (RequestResponseRuleEntity rule : rules) {
            PayloadRepositoryEntity requestPayloadEntity = payloadMap.get(rule.getRequestKey());
            PayloadRepositoryEntity responsePayloadEntity = payloadMap.get(rule.getResponseKey());

            if (requestPayloadEntity == null) {
                log.error("Request payload with key '{}' not found for rule in group '{}'. Skipping rule.", rule.getRequestKey(), ruleGroup);
                continue;
            }
            if (responsePayloadEntity == null) {
                log.error("Response payload with key '{}' not found for rule in group '{}'. Skipping rule.", rule.getResponseKey(), ruleGroup);
                continue;
            }

            try {
                // 将请求报文内容解析为Map<String, Object>
                Map<String, Object> requestMap = parsePayloadContent(requestPayloadEntity);
                // 将响应报文内容解析为Map<String, Object>
                Map<String, Object> responseMap = parsePayloadContent(responsePayloadEntity);

                rulesCache.computeIfAbsent(requestMap, k -> new ArrayList<>()).add(responseMap);
            } catch (IOException e) {
                log.error("Error parsing payload content for rule in group '{}', request key '{}' or response key '{}'. Skipping rule.", ruleGroup, rule.getRequestKey(), rule.getResponseKey(), e);
            }
        }
        log.info("Loaded {} unique request patterns with {} total response options for group '{}'.", rulesCache.size(), rules.size(), ruleGroup);
    }

    private Map<String, Object> parsePayloadContent(PayloadRepositoryEntity payloadEntity) throws IOException {
        // 根据payloadType使用ObjectMapper解析内容
        if ("json".equalsIgnoreCase(payloadEntity.getPayloadType())) {
            return objectMapper.readValue(payloadEntity.getContent(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } else if ("hex".equalsIgnoreCase(payloadEntity.getPayloadType())) {
            // 对于hex类型，我们将其包装成一个Map，以便与MessageCodec兼容
            return Collections.singletonMap(HexCodec.HEX_DATA_KEY, payloadEntity.getContent());
        } else {
            throw new IllegalArgumentException("Unsupported payload type: " + payloadEntity.getPayloadType());
        }
    }

    @Override
    public void execute() {
        if (rulesCache.isEmpty()) {
            log.warn("DbRequestResponseStrategy for group '{}' has no rules loaded. Not starting listener.", ruleGroup);
            return;
        }
        log.info("Starting DbRequestResponseStrategy for group '{}'.", ruleGroup);

        ProtocolHandler.MessageListener listener = (context, data) -> {
            // 1. 使用编解码器将收到的字节解码为业务数据Map
            Map<String, Object> requestMap = messageCodec.decode(data);
            if (requestMap == null || requestMap.isEmpty()) {
                log.warn("Decoded request is null or empty, ignoring.");
                return;
            }
            log.debug("Received and decoded request: {}", requestMap);

            // 2. 在缓存中查找匹配的请求
            List<Map<String, Object>> possibleResponses = rulesCache.get(requestMap);

            if (possibleResponses != null && !possibleResponses.isEmpty()) {
                // 3. 随机选择一个响应
                Map<String, Object> selectedResponse = possibleResponses.get(random.nextInt(possibleResponses.size()));
                log.info("Found matching request: {}. Randomly selected response: {}", requestMap, selectedResponse);

                // 4. 获取对应的响应Map，并编码为字节
                byte[] responseBytes = messageCodec.encode(selectedResponse, null);

                // 5. 通过协议处理器将响应发送回指定的客户端上下文
                this.protocolHandler.send(context, responseBytes);
            } else {
                log.warn("No response mapping found for request: {}", requestMap);
            }
        };

        // 6. 启动协议处理器并传入我们的监听器
        this.protocolHandler.start(listener);
    }

    @Override
    public void stop() {
        log.info("Stopping DbRequestResponseStrategy for group '{}'.", ruleGroup);
        if (this.protocolHandler != null) {
            this.protocolHandler.stop();
        }
    }
}
