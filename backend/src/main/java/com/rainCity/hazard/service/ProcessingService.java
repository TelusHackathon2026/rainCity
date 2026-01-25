package com.rainCity.hazard.service;

import com.rainCity.hazard.model.HazardModels.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessingService {

  private final ExternalApiService apiService;

  // getter
  public ProcessingService(ExternalApiService apiService) {
    this.apiService = apiService;
  }

  public HazardResponse processLocation(String locationStr) {
    // we get location -> send locationStr to api to fetch image
    byte[] rawImage = apiService.fetchCameraImage(locationStr);
    if (rawImage.length == 0) return null; // erorr

    // return new arr of features of the image
    List<HFDetection> detections = apiService.detectHazards(rawImage);

    // analyze the tags then add it to score
    DetailedTags details = analyzeTags(detections);
    double currentScore = calculateHazardScore(details);

    // Get Historical Stats
    double averageScore = apiService.fetchHistoricalAverage(locationStr);
    double delta = currentScore - averageScore;

    // Spike Detection (Simple StdDev logic approximation)
    boolean isSpike = delta > (averageScore * 0.2) && currentScore > 10;

    // Generate Description via DeepSeek
    String description = apiService.generateDescription(details, isSpike, currentScore);

    // 7. Draw Circles
    String base64Image = apiService.drawDetections(rawImage, detections);

    // 8. Build Response
    return HazardResponse.builder()
        .id(UUID.randomUUID().toString())
        .locationString(locationStr)
        .coords(getCoords(locationStr)) // Helper method to map loc string to lat/long
        .score(currentScore)
        .avg(averageScore)
        .delta(delta)
        .spike(isSpike)
        .description(description)
        .timestamp(Instant.now().toString())
        .imageBase64(base64Image)
        .info(details)
        .build();
  }

  private DetailedTags analyzeTags(List<HFDetection> detections) {
    DetailedTags tags =
        DetailedTags.builder()
            .rawTags(detections.stream().map(HFDetection::getLabel).toList())
            .build();

    for (HFDetection d : detections) {
      String label = d.getLabel().toLowerCase();
      if (label.contains("person") && label.contains("lay")) tags.setPersonLaying(true);
      if (label.contains("cone")) tags.setCones(true);
      if (label.contains("debris")) tags.setNumberOfDebrisItems(tags.getNumberOfDebrisItems() + 1);
      if (label.contains("person")) tags.setPedestrianAmount(tags.getPedestrianAmount() + 1);
      if (label.contains("tree")) tags.setFallenTree(true);
      if (label.contains("crash") || label.contains("accident")) tags.setAccident(true);
    }
    return tags;
  }

  private double calculateHazardScore(DetailedTags tags) {
    double score = 0;
    if (tags.isPersonLaying()) score += 80;
    if (tags.isAccident()) score += 100;
    if (tags.isFallenTree()) score += 60;
    if (tags.isCones()) score += 10;
    score += (tags.getNumberOfDebrisItems() * 5);
    score += (tags.getPedestrianAmount() * 0.5); // Small weight for crowds
    return score;
  }

  // do fix here maybe just map of string names
  private Coordinates getCoords(String loc) {
    return Coordinates.builder().lat(49.2827).lng(-123.1207).build();
  }
}
