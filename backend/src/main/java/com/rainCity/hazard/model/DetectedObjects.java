package com.rainCity.hazard.model;

public record DetectedObjects(
    Integer personLaying,
    Integer trafficCones,
    Integer stoppedVehicles,
    Integer movingVehicles,
    Integer debris,
    Integer trashCanOpen,
    Integer roadDamage) {
  public DetectedObjects() {
    this(0, 0, 0, 0, 0, 0, 0);
  }
}
