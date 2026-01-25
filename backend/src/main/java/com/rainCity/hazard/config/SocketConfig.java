package com.rainCity.hazard.config;

import com.rainCity.hazard.handler.HazardWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class SocketConfig implements WebSocketConfigurer {
  private final HazardWebSocketHandler hazardHandler;

  // getter
  public SocketConfig(HazardWebSocketHandler hazardHandler) {
    this.hazardHandler = hazardHandler;
  }

  // overide og class this maps url to handler fucn
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(hazardHandler, "/ws/hazards").setAllowedOrigins("*");
  }
}
