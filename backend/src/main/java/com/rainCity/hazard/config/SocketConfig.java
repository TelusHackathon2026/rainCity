package com.rainCity.hazard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class SocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // This matches your frontend SockJS URL: http://localhost:8080/ws/hazards
    registry.addEndpoint("/ws/hazards").setAllowedOriginPatterns("*").withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Enables /topic for the frontend to subscribe to
    config.enableSimpleBroker("/topic");
    // Enables /app for the frontend to send data to the backend
    config.setApplicationDestinationPrefixes("/app");
  }
}
