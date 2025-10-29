package com.xinsec.devicesimulator.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // Enable a simple in-memory broker for broadcasting messages
        config.setApplicationDestinationPrefixes("/app"); // Prefix for messages from clients to the server
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register a STOMP endpoint for clients to connect to
        registry.addEndpoint("/ws").withSockJS(); // Use SockJS for browser compatibility
    }
}
