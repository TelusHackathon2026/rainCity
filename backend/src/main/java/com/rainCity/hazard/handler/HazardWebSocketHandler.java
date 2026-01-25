package com.rainCity.hazard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainCity.hazard.model.HazardModels.HazardResponse;
import com.rainCity.hazard.model.HazardModels.LocationRequest;
import com.rainCity.hazard.service.ProcessingService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class HazardWebSocketHandler extends TextWebSocketHandler {

  private final ProcessingService processingService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // store each session for list
  private final Map<WebSocketSession, List<String>> sessionSubscriptions =
      new ConcurrentHashMap<>();

  public HazardWebSocketHandler(ProcessingService processingService) {
    this.processingService = processingService;
  }

  // ok so read json val convert ot locReq class then send off to processing
  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    LocationRequest request = objectMapper.readValue(message.getPayload(), LocationRequest.class);
    sessionSubscriptions.put(session, request.getLocations());

    processAndSend(session, request.getLocations());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessionSubscriptions.remove(session);
  }

  // do it automatiically go throug sesion see if open then ssend processing
  @Scheduled(fixedRateString = "${hazard.refresh-rate-ms}")
  public void scheduledUpdate() {
    sessionSubscriptions.forEach(
        (session, locations) -> {
          if (session.isOpen()) {
            processAndSend(session, locations);
          }
        });
  }

  // for each loc process it get it back then send as json obj.

  private void processAndSend(WebSocketSession session, List<String> locations) {
    for (String loc : locations) {
      try {
        HazardResponse response = processingService.processLocation(loc);
        if (response != null) {
          String json = objectMapper.writeValueAsString(response);
          session.sendMessage(new TextMessage(json));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
