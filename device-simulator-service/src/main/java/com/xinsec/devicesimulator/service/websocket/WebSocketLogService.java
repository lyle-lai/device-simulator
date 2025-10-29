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
        log.debug("Sending log message for profile {}: {}", logMessage.getProfileName(), logMessage.getMessage());
        // Send to a specific topic for the profile, e.g., /topic/logs/profileName
        messagingTemplate.convertAndSend("/topic/logs/" + logMessage.getProfileName(), logMessage);
        // Also send to a general topic for all logs, e.g., /topic/logs/all
        messagingTemplate.convertAndSend("/topic/logs/all", logMessage);
    }
}
