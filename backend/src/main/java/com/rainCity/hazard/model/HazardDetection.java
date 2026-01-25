package com.rainCity.hazard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreatedDate;

@Entity
@Table(name = "hazard_detections")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
  private String severity; // CRITICAL, HIGH, MEDIUM, LOW

  @Embedded private DetectedObjects detectedObjects;

  @Column(length = 1000)
  private String description;

  @Column(length = 2000)
  private String severeDescription;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime timestamp;

  @Column private String imageUrl; // Optional: URL to traffic cam image

  @PrePersist
  protected void onCreate() {
    timestamp = LocalDateTime.now();
  }
}
