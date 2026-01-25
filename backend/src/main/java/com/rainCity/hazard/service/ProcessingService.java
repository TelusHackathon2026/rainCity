package com.rainCity.hazard.service;

import com.rainCity.hazard.model.HazardModels.*;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessingService {

  private final ExternalApiService apiService;

  public ProcessingService(ExternalApiService apiService) {
    this.apiService = apiService;
  }

  public HazardResponse processLocation(String locationStr) {
    System.out.println("🚀 Processing location: " + locationStr);

    // 1. Fetch historical average FIRST (before current detection)
    double averageScore = apiService.fetchHistoricalAverage(locationStr);
    System.out.println("📊 Historical average score: " + averageScore);

    // 2. Fetch images from the Vancouver camera or custom images
    List<byte[]> images = apiService.fetchCameraImages(locationStr);
    if (images.isEmpty()) {
      System.out.println("⚠️ No images found for: " + locationStr);
      return null;
    }

    // 3. Use the first image for analysis
    byte[] rawImage = images.get(0);
    System.out.println("📸 Got raw image: " + rawImage.length + " bytes");

    // 4. Get detections AND labeled image from Hugging Face Gradio Space
    ExternalApiService.HazardDetectionResult result = apiService.detectHazards(rawImage);
    List<ExternalApiService.HazardTag> detections = result.detections();
    byte[] labeledImage = result.labeledImage();

    System.out.println("🎯 Got " + detections.size() + " detections");
    System.out.println("🖼️ Labeled image size: " + labeledImage.length + " bytes");

    // 5. Transform tags and calculate current score
    DetailedTags details = analyzeTags(detections);
    double currentScore = calculateHazardScore(details);
    System.out.println("📊 Current hazard score: " + currentScore);

    // 6. Calculate delta and spike detection
    double delta = Math.max(0, currentScore - averageScore);
    boolean isSpike = delta > (averageScore * 0.2) && currentScore > 10;

    System.out.println("📈 Score Analysis:");
    System.out.println("   Current: " + currentScore);
    System.out.println("   Average: " + averageScore);
    System.out.println("   Delta: " + delta);
    System.out.println("   Spike: " + isSpike);

    // 7. AI Description logic (DeepSeek)
    String description = apiService.generateDescription(detections, isSpike, currentScore);

    // 8. Prepare LABELED Image for Frontend (with bounding boxes)
    String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(labeledImage);
    System.out.println("✅ Base64 image prepared for frontend");

    // 9. Build final response
    HazardResponse response = HazardResponse.builder()
        .id(locationStr)
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

    // 10. DATABASE UPDATE: Persist the results to Supabase
    apiService.saveHazardRecord(response);

    System.out.println("✅ Processing complete for: " + locationStr);
    System.out.println(
        "   Final response - Score: "
            + response.getScore()
            + ", Avg: "
            + response.getAvg()
            + ", Delta: "
            + response.getDelta());

    return response;
  }

  private DetailedTags analyzeTags(List<ExternalApiService.HazardTag> detections) {
    DetailedTags tags = DetailedTags.builder()
        .rawTags(detections.stream().map(ExternalApiService.HazardTag::label).toList())
        .numberOfDebrisItems(0)
        .pedestrianAmount(0)
        .build();

    for (ExternalApiService.HazardTag d : detections) {
      String label = d.label().toLowerCase();

      if (label.contains("person") && label.contains("lay"))
        tags.setPersonLaying(true);
      if (label.contains("cone"))
        tags.setCones(true);
      if (label.contains("debris"))
        tags.setNumberOfDebrisItems(tags.getNumberOfDebrisItems() + 1);
      if (label.contains("person") && !label.contains("lay"))
        tags.setPedestrianAmount(tags.getPedestrianAmount() + 1);
      if (label.contains("tree"))
        tags.setFallenTree(true);
      if (label.contains("crash") || label.contains("accident") || label.contains("wreck"))
        tags.setAccident(true);
    }

    System.out.println("🏷️ Analyzed tags: " + tags.getRawTags());
    return tags;
  }

  private double calculateHazardScore(DetailedTags tags) {
    double score = 0;
    if (tags.isPersonLaying())
      score += 80;
    if (tags.isAccident())
      score += 100;
    if (tags.isFallenTree())
      score += 60;
    if (tags.isCones())
      score += 10;
    score += (tags.getNumberOfDebrisItems() * 5);
    score += (tags.getPedestrianAmount() * 0.5);

    System.out.println(
        "🔢 Score breakdown - PersonLaying: "
            + tags.isPersonLaying()
            + ", Accident: "
            + tags.isAccident()
            + ", Tree: "
            + tags.isFallenTree()
            + ", Cones: "
            + tags.isCones()
            + ", Debris: "
            + tags.getNumberOfDebrisItems()
            + ", Pedestrians: "
            + tags.getPedestrianAmount());

    return score;
  }
}
