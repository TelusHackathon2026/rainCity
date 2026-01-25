package com.rainCity.hazard.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

public class HazardModels {

  // 1. Message from Frontend
  @Data
  public static class LocationRequest {
    private List<String> locations; // e.g., ["Granville_12th", "Cambie_Broadway"]
  }

  // 2. Response to Frontend
  @Data
  @Builder
  public static class HazardResponse {
    private String id;
    private String locationString;
    private Coordinates coords;
    private double score;
    private double avg;
    private double delta;
    private boolean spike;
    private String description;
    private String timestamp;
    private String imageBase64; // The image with circles drawn
    private DetailedTags info;
  }

  @Data
  @Builder
  public static class Coordinates {
    private double lat;
    private double lng;
  }

  @Data
  @Builder
  public static class DetailedTags {
    private boolean personLaying;
    private boolean cones;
    private boolean accident; // crash occurred
    private int numberOfDebrisItems;
    private int pedestrianAmount;
    private boolean fallenTree;
    // Raw tags from YOLO for internal logic
    private List<String> rawTags;
  }

  // 4. Supabase Record
  @Data
  @Builder
  public static class HazardRecord {
    private String location_id;
    private double score;
    private String timestamp;
  }
}
