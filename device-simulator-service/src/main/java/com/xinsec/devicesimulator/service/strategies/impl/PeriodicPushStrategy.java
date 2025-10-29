package com.xinsec.devicesimulator.service.strategies.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ComponentType("periodic-push")
@Slf4j
public class PeriodicPushStrategy implements SimulationStrategy {

    private long intervalMillis;
    private ProtocolHandler protocolHandler;
    private MessageCodec messageCodec;
    private DataGenerator dataGenerator;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator) {
        this.intervalMillis = config.path("intervalMillis").asLong(5000);
        this.protocolHandler = protocol;
        this.messageCodec = codec;
        this.dataGenerator = generator;
    }

    @Override
    public void execute() {
        // 启动协议层，准备发送数据
        // (对于推送策略，我们不关心接收数据，所以listener是null)
        protocolHandler.start(null);

        // 创建并启动定时任务
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(this::generateAndSendData, 0, intervalMillis, TimeUnit.MILLISECONDS);
        log.info("定时推送策略(PeriodicPushStrategy)已启动。每 {} 毫秒推送一次数据。", intervalMillis);
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
        // 停止协议层
        protocolHandler.stop();
        log.info("定时推送策略(PeriodicPushStrategy)已停止。");
    }

    private void generateAndSendData() {
        try {
            // 1. 生成数据
            Map<String, Object> data = dataGenerator.generate();
            if (data == null || data.isEmpty()) {
                log.trace("数据生成器未返回数据，跳过本次发送。");
                return;
            }
            log.debug("生成数据: {}", data);

            // 2. 编码数据
            byte[] encodedData = messageCodec.encode(data, null); // metadata暂时为null
            if (encodedData == null || encodedData.length == 0) {
                log.trace("编码器未返回数据，跳过本次发送。");
                return;
            }
            log.debug("编码后数据长度: {}", encodedData.length);

            // 3. 发送数据
            protocolHandler.send(encodedData);

        } catch (Exception e) {
            // 捕获所有异常，防止定时任务中断
            log.error("定时推送任务执行时发生错误。", e);
        }
    }
}
