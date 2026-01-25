package com.rainCity.hazard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainCity.hazard.model.HazardModels.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

// TODO: prob gonan import this one from xml thing
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ExternalApiService {
  @Value("${app.ai-model.hf-api-url}")
  private String hfUrl;

  @Value("${app.ai-model.hf-token}")
  private String hfToken;

  @Value("${app.deepseek-api.url}")
  private String deepSeekUrl;

  @Value("${app.deepseek-api.access-token}")
  private String deepSeekToken;

  @Value("${app.supabase.rest-url}")
  private String supabaseUrl;

  @Value("${app.supabase.anon-key}")
  private String supabaseKey;

  private final WebClient webClient = WebClient.create();
  private final ObjectMapper objectMapper = new ObjectMapper();

  // dont thing u need this but may
  private Map<String, CameraInfo> cameraCache = new HashMap<>();

  @PostConstruct
  public void loadCameraData() {
    try {
      ClassPathResource resource = new ClassPathResource("vancouver-cameras.json");
      InputStream inputStream = resource.getInputStream();

      JsonNode root = objectMapper.readTree(inputStream);

      // TODO:basically dont think u need to do all this cause they are objs so
      // basically search through JSON thing by the name from this fucntion call -> go
      // to
      // other file espcially the process one that called the past thing it passed in
      // lcoation id cahnge that to wathever u make the name of this.
      // so this function should go into json thing find it by namestring u have, then
      // save thd id and the spceifc stuff needed jsut temp for now
      if (root.isArray()) {
        for (JsonNode node : root) {
          String name = node.get("name").asText();
          String url = node.get("url").asText();
          String mapId = node.get("mapid").asText();

          double lat = node.get("geo_point_2d").get("lat").asDouble();
          double lon = node.get("geo_point_2d").get("lon").asDouble();

          CameraInfo info = CameraInfo.builder().name(name).url(url).mapId(mapId).lat(lat).lon(lon).build();

          // Store by name (lowercase for easy lookup)
          cameraCache.put(name.toLowerCase(), info);
          cameraCache.put(mapId.toLowerCase(), info);
        }
      }

      System.out.println("Loaded " + cameraCache.size() + " camera locations");
    } catch (Exception e) {
      System.err.println("Failed to load camera data: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // TODO:1. Fetch all 4 images from camera URL, ok so itll go to t
  // to the url then get all 4 by reading HTML dont think I did this right, but
  // right in theory
  public List<byte[]> fetchCameraImages(String locationId) {
    try {
      // Look up camera info from cache
      CameraInfo camera = cameraCache.get(locationId.toLowerCase());

      if (camera == null) {
        System.err.println("Camera not found: " + locationId);
        return new ArrayList<>();
      }

      System.out.println("Fetching images from: " + camera.getUrl());

      // Fetch the HTML page
      String htmlContent = webClient.get().uri(camera.getUrl()).retrieve().bodyToMono(String.class).block();

      if (htmlContent == null) {
        return new ArrayList<>();
      }

      // Extract image URLs from HTML
      List<String> imageUrls = extractImageUrls(htmlContent, camera.getUrl());

      System.out.println("Found " + imageUrls.size() + " images");

      // Download up to 4 images
      List<byte[]> images = new ArrayList<>();
      for (int i = 0; i < Math.min(4, imageUrls.size()); i++) {
        try {
          byte[] imageBytes = webClient.get().uri(imageUrls.get(i)).retrieve().bodyToMono(byte[].class).block();

          if (imageBytes != null && imageBytes.length > 0) {
            images.add(imageBytes);
            System.out.println("Downloaded image " + (i + 1) + ": " + imageBytes.length + " bytes");
          }
        } catch (Exception e) {
          System.err.println("Failed to download image: " + imageUrls.get(i));
        }
      }

      return images;
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  // Helper: Extract image URLs from HTML
  private List<String> extractImageUrls(String html, String baseUrl) {
    List<String> urls = new ArrayList<>();

    // Get base URL for relative paths
    String base = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1);

    // Find all img tags
    String[] parts = html.split("<img");
    for (String part : parts) {
      if (part.contains("src=")) {
        int start = part.indexOf("src=\"") + 5;
        if (start > 4) {
          int end = part.indexOf("\"", start);
          if (end > start) {
            String url = part.substring(start, end);

            // Filter for image files
            if (url.endsWith(".jpg")
                || url.endsWith(".jpeg")
                || url.endsWith(".png")
                || url.endsWith(".JPG")) {

              // Handle relative URLs
              if (!url.startsWith("http")) {
                if (url.startsWith("/")) {
                  url = "https://trafficcams.vancouver.ca" + url;
                } else {
                  url = base + url;
                }
              }

              urls.add(url);
            }
          }
        }
      }
    }

    return urls;
  }

  // 2. Send to Hugging Face - returns BOTH labeled image AND detection data
  public HFResponse detectHazardsWithLabeledImage(byte[] imageBytes) {
    try {
      // TODO: the HF is very fucked up, in model file that basically where the
      // objects
      // are defined i deleted teh HF one so we should make custom one
      // prob one that saves the tags into thing we can caonvert to json to sned, and
      // save the img. Will have to mod db to save img if want to.

      // HF returns JSON with detections
      //
      // TODO: all this func should really do is fetch essentilaly fuck the box shit i
      // dont think we need it
      String jsonResponse = webClient
          .post()
          .uri(hfUrl)
          .header("Authorization", "Bearer " + hfToken)
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .bodyValue(imageBytes)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (jsonResponse == null) {
        return null;
      }

      // Parse detections
      JsonNode root = objectMapper.readTree(jsonResponse);
      List<HFDetection> detections = new ArrayList<>();

      if (root.isArray()) {
        for (JsonNode node : root) {
          HFDetection detection = HFDetection.builder()
              .label(node.get("label").asText())
              .confidence(node.get("score").asDouble())
              .box(
                  Box.builder()
                      .xmin((int) node.get("box").get("xmin").asDouble())
                      .ymin((int) node.get("box").get("ymin").asDouble())
                      .xmax((int) node.get("box").get("xmax").asDouble())
                      .ymax((int) node.get("box").get("ymax").asDouble())
                      .build())
              .build();
          detections.add(detection);
        }
      }

      String labeledImageBase64 = drawDetections(imageBytes, detections);

      return HFResponse.builder()
          .labeledImageBase64(labeledImageBase64)
          .detections(detections)
          .build();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // TODO:garbage get rid of this
  private String drawDetections(byte[] imageBytes, List<HFDetection> detections) {
    try {
      java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
      java.awt.Graphics2D g2d = img.createGraphics();

      g2d.setColor(java.awt.Color.RED);
      g2d.setStroke(new java.awt.BasicStroke(3));
      g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));

      for (HFDetection d : detections) {
        Box b = d.getBox();
        int width = b.getXmax() - b.getXmin();
        int height = b.getYmax() - b.getYmin();

        // Draw rectangle
        g2d.drawRect(b.getXmin(), b.getYmin(), width, height);

        // Draw label with background
        String label = d.getLabel() + " " + String.format("%.2f", d.getConfidence());
        g2d.setColor(java.awt.Color.RED);
        g2d.fillRect(b.getXmin(), b.getYmin() - 20, 150, 20);
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(label, b.getXmin() + 5, b.getYmin() - 5);
        g2d.setColor(java.awt.Color.RED);
      }

      g2d.dispose();

      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(img, "jpg", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  // TODO: dont need to do all this, once id is saved search json with id whihc
  // will contain the coords
  public Coordinates getCameraCoordinates(String locationId) {
    CameraInfo camera = cameraCache.get(locationId.toLowerCase());
    if (camera != null) {
      return Coordinates.builder().lat(camera.getLat()).lng(camera.getLon()).build();
    }
    // Default to downtown Vancouver
    return Coordinates.builder().lat(49.2827).lng(-123.1207).build();
  }

  // 5. Fetch History from Supabase
  public double fetchHistoricalAverage(String locationId) {
    try {
      String response = webClient
          .get()
          .uri(
              supabaseUrl
                  + "/hazards?location_id=eq."
                  + locationId
                  + "&order=timestamp.desc&limit=50")
          .header("apikey", supabaseKey)
          .header("Authorization", "Bearer " + supabaseKey)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (response == null)
        return 0.0;

      JsonNode records = objectMapper.readTree(response);
      if (!records.isArray() || records.size() == 0)
        return 0.0;

      double sum = 0;
      int count = 0;
      for (JsonNode record : records) {
        if (record.has("score")) {
          sum += record.get("score").asDouble();
          count++;
        }
      }

      return count > 0 ? sum / count : 0.0;
    } catch (Exception e) {
      e.printStackTrace();
      return 0.0;
    }
  }

  // TODO:THis is ok but we need 2 propms one if spiek true or not, so just make 2
  // prompts then send the one when SPike is true.
  public String generateDescription(DetailedTags tags, boolean isSpike, double score) {
    String prompt = "Analyze these traffic hazard tags: "
        + tags.toString()
        + ". Hazard Score: "
        + score
        + ". Spike detected: "
        + isSpike
        + ". Return 2-3 sentences describing the situation. Be concise and factual.";

    Map<String, Object> body = Map.of(
        "messages",
        List.of(Map.of("role", "user", "content", prompt)),
        "model",
        "deepseek-chat",
        "max_tokens",
        150);

    try {
      String response = webClient
          .post()
          .uri(deepSeekUrl)
          .header("Authorization", "Bearer " + deepSeekToken)
          .header("Content-Type", "application/json")
          .bodyValue(body)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (response == null)
        return "AI Analysis unavailable.";

      JsonNode root = objectMapper.readTree(response);
      if (root.has("choices") && root.get("choices").size() > 0) {
        return root.get("choices").get(0).get("message").get("content").asText();
      }

      return "AI Analysis unavailable.";
    } catch (Exception e) {
      e.printStackTrace();
      return "AI Analysis unavailable.";
    }
  }
}
