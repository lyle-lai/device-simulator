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
        // 启用一个简单的基于内存的消息代理，用于向客户端广播消息，目的地前缀为 /topic
        config.enableSimpleBroker("/topic"); 
        // 定义从客户端发送到服务器的消息的目标前缀，例如 @MessageMapping("/hello") 的完整路径是 /app/hello
        config.setApplicationDestinationPrefixes("/app"); 
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册一个STOMP端点，客户端将通过这个路径与服务器建立WebSocket连接
        registry.addEndpoint("/ws").withSockJS(); // 启用SockJS后备选项，以便在浏览器不支持WebSocket时提供兼容性
    }
}
