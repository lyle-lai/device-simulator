package com.xinsec.devicesimulator.service.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogMessage {
    private String profileName;
    private LocalDateTime timestamp;
    private String level; // e.g., INFO, WARN, ERROR
    private String message;
}
