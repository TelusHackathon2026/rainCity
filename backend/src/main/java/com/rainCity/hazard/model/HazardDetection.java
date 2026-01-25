package com.rainCity.hazard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hazard_detections")
public class HazardDetection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String location;

  @Column(nullable = false)
  private Integer currentScore;

  @Column(nullable = false)
  private Integer baseline;

  @Column(nullable = false)
  private Integer delta;

  @Column(nullable = false)
  private String severity;

  @Embedded private DetectedObjects detectedObjects;

  @Column(length = 1000)
  private String description;

  @Column(length = 2000)
  private String severeDescription;

  @Column(nullable = false, updatable = false)
  private LocalDateTime timestamp;

  @Lob
  @Column(name = "image", columnDefinition = "bytea") // PostgreSQL specific
  private byte[] image;

  @PrePersist
  protected void onCreate() {
    timestamp = LocalDateTime.now();
  }

  // Constructors
  public HazardDetection() {}

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Integer getCurrentScore() {
    return currentScore;
  }

  public void setCurrentScore(Integer currentScore) {
    this.currentScore = currentScore;
  }

  public Integer getBaseline() {
    return baseline;
  }

  public void setBaseline(Integer baseline) {
    this.baseline = baseline;
  }

  public Integer getDelta() {
    return delta;
  }

  public void setDelta(Integer delta) {
    this.delta = delta;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public DetectedObjects getDetectedObjects() {
    return detectedObjects;
  }

  public void setDetectedObjects(DetectedObjects detectedObjects) {
    this.detectedObjects = detectedObjects;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSevereDescription() {
    return severeDescription;
  }

  public void setSevereDescription(String severeDescription) {
    this.severeDescription = severeDescription;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}
