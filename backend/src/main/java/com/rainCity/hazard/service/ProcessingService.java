package com.rainCity.hazard.service;

import com.rainCity.hazard.model.HazardModels.*;
import java.time.Instant;
import java.util.Base64; // Added for image conversion
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessingService {

  private final ExternalApiService apiService;

  public ProcessingService(ExternalApiService apiService) {
    this.apiService = apiService;
  }

  public HazardResponse processLocation(String locationStr) {
    // 1. Fetch images from the Vancouver camera
    List<byte[]> images = apiService.fetchCameraImages(locationStr);
    if (images.isEmpty()) return null; 

    // 2. Use the first image for analysis
    byte[] rawImage = images.get(0);

    // 3. Get detections from Hugging Face
    List<ExternalApiService.HazardTag> detections = apiService.detectHazards(rawImage);

    // 4. Transform tags and calculate scores
    DetailedTags details = analyzeTags(detections);
    double currentScore = calculateHazardScore(details);

    // 5. Historical Analysis
    double averageScore = apiService.fetchHistoricalAverage(locationStr);
    double delta = currentScore - averageScore;
    boolean isSpike = delta > (averageScore * 0.2) && currentScore > 10;

    // 6. AI Description logic (now handles spike vs no-spike prompts)
    String description = apiService.generateDescription(detections, isSpike, currentScore);

    // 7. Prepare Image for Frontend
    // Since HF/Vancouver provides the image, we just encode the bytes to Base64
    String base64Image = Base64.getEncoder().encodeToString(rawImage);

    // 8. Build final response
    return HazardResponse.builder()
        .id(UUID.randomUUID().toString())
        .locationString(locationStr)
        .coords(apiService.getCameraCoordinates(locationStr)) 
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

  private DetailedTags analyzeTags(List<ExternalApiService.HazardTag> detections) {
    DetailedTags tags = DetailedTags.builder()
            .rawTags(detections.stream().map(ExternalApiService.HazardTag::label).toList())
            .build();

    for (ExternalApiService.HazardTag d : detections) {
      String label = d.label().toLowerCase();
      // Scoring Logic
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
    score += (tags.getPedestrianAmount() * 0.5); 
    return score;
  }
}