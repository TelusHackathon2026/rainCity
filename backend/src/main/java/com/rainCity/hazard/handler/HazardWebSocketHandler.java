package com.rainCity.hazard.handler;

import com.rainCity.hazard.model.HazardModels.HazardResponse;
import com.rainCity.hazard.model.HazardModels.LocationRequest;
import com.rainCity.hazard.service.ProcessingService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
public class HazardWebSocketHandler {

  private final ProcessingService processingService;
  private final SimpMessagingTemplate messagingTemplate;

  // Stores unique locations to monitor across all connected users
  private final Set<String> monitoredLocations = ConcurrentHashMap.newKeySet();

  public HazardWebSocketHandler(
      ProcessingService processingService, SimpMessagingTemplate messagingTemplate) {
    this.processingService = processingService;
    this.messagingTemplate = messagingTemplate;
  }

  @MessageMapping("/monitor-intersections")
  public void handleMonitorRequest(@Payload LocationRequest request) {
    if (request != null && request.getLocations() != null) {
      monitoredLocations.addAll(request.getLocations());
      System.out.println("Backend is now tracking: " + monitoredLocations);
    }
  }

  @Scheduled(fixedRateString = "${hazard.refresh-rate-ms}")
  public void scheduledUpdate() {
    if (monitoredLocations.isEmpty())
      return;

    for (String loc : monitoredLocations) {
      try {
        HazardResponse response = processingService.processLocation(loc);
        if (response != null) {
          messagingTemplate.convertAndSend("/topic/traffic-alerts", response);
        }
      } catch (Exception e) {
        System.err.println("Error updating location " + loc + ": " + e.getMessage());
      }
    }
  }
}
