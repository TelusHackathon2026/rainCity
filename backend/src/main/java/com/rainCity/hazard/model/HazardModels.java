package com.rainCity.hazard.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

public class HazardModels {

  // 1. Message from Frontend
  @Data
  public static class LocationRequest {
    private List<String> locations;

    // Add this manually
    public List<String> getLocations() {
      return locations;
    }
  }

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
    private String imageBase64;
    private DetailedTags info;

    // Constructor
    public HazardResponse() {
    }

    // Getters
    public String getId() {
      return id;
    }

    public String getLocationString() {
      return locationString;
    }

    public Coordinates getCoords() {
      return coords;
    }

    public double getScore() {
      return score;
    }

    public double getAvg() {
      return avg;
    }

    public double getDelta() {
      return delta;
    }

    public boolean isSpike() {
      return spike;
    }

    public String getDescription() {
      return description;
    }

    public String getTimestamp() {
      return timestamp;
    }

    public String getImageBase64() {
      return imageBase64;
    }

    public DetailedTags getInfo() {
      return info;
    }

    // Setters
    public void setId(String id) {
      this.id = id;
    }

    public void setLocationString(String locationString) {
      this.locationString = locationString;
    }

    public void setCoords(Coordinates coords) {
      this.coords = coords;
    }

    public void setScore(double score) {
      this.score = score;
    }

    public void setAvg(double avg) {
      this.avg = avg;
    }

    public void setDelta(double delta) {
      this.delta = delta;
    }

    public void setSpike(boolean spike) {
      this.spike = spike;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void setTimestamp(String timestamp) {
      this.timestamp = timestamp;
    }

    public void setImageBase64(String imageBase64) {
      this.imageBase64 = imageBase64;
    }

    public void setInfo(DetailedTags info) {
      this.info = info;
    }

    // Builder
    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String id;
      private String locationString;
      private Coordinates coords;
      private double score;
      private double avg;
      private double delta;
      private boolean spike;
      private String description;
      private String timestamp;
      private String imageBase64;
      private DetailedTags info;

      public Builder id(String id) {
        this.id = id;
        return this;
      }

      public Builder locationString(String locationString) {
        this.locationString = locationString;
        return this;
      }

      public Builder coords(Coordinates coords) {
        this.coords = coords;
        return this;
      }

      public Builder score(double score) {
        this.score = score;
        return this;
      }

      public Builder avg(double avg) {
        this.avg = avg;
        return this;
      }

      public Builder delta(double delta) {
        this.delta = delta;
        return this;
      }

      public Builder spike(boolean spike) {
        this.spike = spike;
        return this;
      }

      public Builder description(String description) {
        this.description = description;
        return this;
      }

      public Builder timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public Builder imageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
        return this;
      }

      public Builder info(DetailedTags info) {
        this.info = info;
        return this;
      }

      public HazardResponse build() {
        HazardResponse response = new HazardResponse();
        response.id = this.id;
        response.locationString = this.locationString;
        response.coords = this.coords;
        response.score = this.score;
        response.avg = this.avg;
        response.delta = this.delta;
        response.spike = this.spike;
        response.description = this.description;
        response.timestamp = this.timestamp;
        response.imageBase64 = this.imageBase64;
        response.info = this.info;
        return response;
      }
    }
  }

  public static class Coordinates {
    private double lat;
    private double lng;

    public Coordinates() {
    }

    public Coordinates(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
    }

    // Getters
    public double getLat() {
      return lat;
    }

    public double getLng() {
      return lng;
    }

    // Setters
    public void setLat(double lat) {
      this.lat = lat;
    }

    public void setLng(double lng) {
      this.lng = lng;
    }

    // Builder
    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private double lat;
      private double lng;

      public Builder lat(double lat) {
        this.lat = lat;
        return this;
      }

      public Builder lng(double lng) {
        this.lng = lng;
        return this;
      }

      public Coordinates build() {
        return new Coordinates(lat, lng);
      }
    }
  }

  public static class DetailedTags {
    private boolean personLaying = false;
    private boolean cones = false;
    private boolean accident = false;
    private int numberOfDebrisItems = 0;
    private int pedestrianAmount = 0;
    private boolean fallenTree = false;
    private List<String> rawTags;

    // Getters
    public boolean isPersonLaying() {
      return personLaying;
    }

    public boolean isCones() {
      return cones;
    }

    public boolean isAccident() {
      return accident;
    }

    public int getNumberOfDebrisItems() {
      return numberOfDebrisItems;
    }

    public int getPedestrianAmount() {
      return pedestrianAmount;
    }

    public boolean isFallenTree() {
      return fallenTree;
    }

    public List<String> getRawTags() {
      return rawTags;
    }

    // Setters
    public void setPersonLaying(boolean personLaying) {
      this.personLaying = personLaying;
    }

    public void setCones(boolean cones) {
      this.cones = cones;
    }

    public void setAccident(boolean accident) {
      this.accident = accident;
    }

    public void setNumberOfDebrisItems(int numberOfDebrisItems) {
      this.numberOfDebrisItems = numberOfDebrisItems;
    }

    public void setPedestrianAmount(int pedestrianAmount) {
      this.pedestrianAmount = pedestrianAmount;
    }

    public void setFallenTree(boolean fallenTree) {
      this.fallenTree = fallenTree;
    }

    public void setRawTags(List<String> rawTags) {
      this.rawTags = rawTags;
    }

    // Builder
    public static DetailedTagsBuilder builder() {
      return new DetailedTagsBuilder();
    }

    public static class DetailedTagsBuilder {
      private boolean personLaying = false;
      private boolean cones = false;
      private boolean accident = false;
      private int numberOfDebrisItems = 0;
      private int pedestrianAmount = 0;
      private boolean fallenTree = false;
      private List<String> rawTags;

      public DetailedTagsBuilder personLaying(boolean personLaying) {
        this.personLaying = personLaying;
        return this;
      }

      public DetailedTagsBuilder cones(boolean cones) {
        this.cones = cones;
        return this;
      }

      public DetailedTagsBuilder accident(boolean accident) {
        this.accident = accident;
        return this;
      }

      public DetailedTagsBuilder numberOfDebrisItems(int numberOfDebrisItems) {
        this.numberOfDebrisItems = numberOfDebrisItems;
        return this;
      }

      public DetailedTagsBuilder pedestrianAmount(int pedestrianAmount) {
        this.pedestrianAmount = pedestrianAmount;
        return this;
      }

      public DetailedTagsBuilder fallenTree(boolean fallenTree) {
        this.fallenTree = fallenTree;
        return this;
      }

      public DetailedTagsBuilder rawTags(List<String> rawTags) {
        this.rawTags = rawTags;
        return this;
      }

      public DetailedTags build() {
        DetailedTags tags = new DetailedTags();
        tags.personLaying = this.personLaying;
        tags.cones = this.cones;
        tags.accident = this.accident;
        tags.numberOfDebrisItems = this.numberOfDebrisItems;
        tags.pedestrianAmount = this.pedestrianAmount;
        tags.fallenTree = this.fallenTree;
        tags.rawTags = this.rawTags;
        return tags;
      }
    }
  } // 4. Supabase Record

  @Data
  @Builder
  public static class HazardRecord {
    private String location_id;
    private double score;
    private String timestamp;
  }
}
