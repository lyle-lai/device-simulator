package com.xinsec.devicesimulator.service.websocket;

import com.xinsec.devicesimulator.service.websocket.dto.LogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketLogService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendLogMessage(LogMessage logMessage) {
        log.debug("正在为画像 {} 发送日志消息: {}", logMessage.getProfileName(), logMessage.getMessage());
        // 发送到特定画像的专用主题，例如 /topic/logs/profileName
        messagingTemplate.convertAndSend("/topic/logs/" + logMessage.getProfileName(), logMessage);
        // 同时发送到一个通用的主题，用于广播所有日志，例如 /topic/logs/all
        messagingTemplate.convertAndSend("/topic/logs/all", logMessage);
    }
}
