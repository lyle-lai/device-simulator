package com.xinsec.devicesimulator.service.strategies.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * 一个“无操作”的策略。
 */
@ComponentType("passthrough-strategy")
@Slf4j
public class PassthroughStrategy implements SimulationStrategy {
    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator) {
        log.debug("PassthroughStrategy 已配置。无操作。");
    }

    @Override
    public void execute() {
        log.debug("PassthroughStrategy 已执行。无操作。");
    }

    @Override
    public void stop() {
        log.debug("PassthroughStrategy 已停止。无操作。");
    }
}