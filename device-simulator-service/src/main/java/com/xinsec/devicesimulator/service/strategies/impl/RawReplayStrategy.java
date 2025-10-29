package com.xinsec.devicesimulator.service.strategies.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinsec.devicesimulator.service.codecs.MessageCodec;
import com.xinsec.devicesimulator.service.core.ComponentType;
import com.xinsec.devicesimulator.service.datagen.DataGenerator;
import com.xinsec.devicesimulator.service.protocols.ProtocolHandler;
import com.xinsec.devicesimulator.service.strategies.SimulationStrategy;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ComponentType("raw-replay")
@Slf4j
public class RawReplayStrategy implements SimulationStrategy {

    private long intervalMillis;
    private String replayMode;
    private List<Message> messages;
    private ProtocolHandler protocolHandler;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private final AtomicInteger sequentialCounter = new AtomicInteger(0);

    @Override
    public void configure(JsonNode config, ProtocolHandler protocol, MessageCodec codec, DataGenerator generator) {
        this.intervalMillis = config.path("intervalMillis").asLong(3000);
        this.replayMode = config.path("replayMode").asText("sequential");
        this.protocolHandler = protocol;

        if (config.has("messages")) {
            this.messages = StreamSupport.stream(config.get("messages").spliterator(), false)
                    .map(Message::new)
                    .collect(Collectors.toList());
        } else {
            this.messages = new ArrayList<>();
            log.warn("RawReplayStrategy configured without 'messages'. It will do nothing.");
        }
    }

    @Override
    public void execute() {
        if (messages.isEmpty()) {
            log.warn("No messages to replay. RawReplayStrategy will not start.");
            return;
        }
        protocolHandler.start(null); // No listener needed

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = scheduler.scheduleAtFixedRate(this::selectAndSendData, 0, intervalMillis, TimeUnit.MILLISECONDS);
        log.info("RawReplayStrategy started. Replay mode: '{}', Interval: {} ms.", replayMode, intervalMillis);
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
        log.info("RawReplayStrategy stopped.");
    }

    private void selectAndSendData() {
        try {
            Message message;
            if ("random".equalsIgnoreCase(replayMode)) {
                message = messages.get(new Random().nextInt(messages.size()));
            } else { // sequential
                message = messages.get(sequentialCounter.getAndIncrement() % messages.size());
            }

            byte[] rawData = message.decode();
            log.debug("Replaying message ({} bytes)", rawData.length);
            protocolHandler.send(rawData);

        } catch (Exception e) {
            log.error("Error during raw replay execution.", e);
        } 
    }

    private static class Message {
        final String encoding;
        final String data;

        Message(JsonNode node) {
            this.encoding = node.path("encoding").asText("utf8");
            this.data = node.path("data").asText("");
        }

        byte[] decode() {
            String lowerCaseEncoding = encoding.toLowerCase();
            if ("hex".equals(lowerCaseEncoding)) {
                return hexStringToByteArray(data);
            } else if ("base64".equals(lowerCaseEncoding)) {
                return Base64.getDecoder().decode(data);
            } else if ("utf8".equals(lowerCaseEncoding) || "string".equals(lowerCaseEncoding)) {
                return data.getBytes(StandardCharsets.UTF_8);
            } else {
                log.warn("Unsupported encoding '{}'. Treating as plain string.", encoding);
                return data.getBytes(StandardCharsets.UTF_8);
            }
        }

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
