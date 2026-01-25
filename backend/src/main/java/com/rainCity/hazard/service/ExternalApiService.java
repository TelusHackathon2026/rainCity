package com.rainCity.hazard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.*;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ExternalApiService {

    public record HazardTag(String label, double score) {} 
    
    public record CameraInfo(String name, String url, String mapId, double lat, double lon) {
        public static CameraInfoBuilder builder() { return new CameraInfoBuilder(); }
    }

    public record Coordinates(double lat, double lng) {
        public static CoordinatesBuilder builder() { return new CoordinatesBuilder(); }
    }

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
    private final Map<String, CameraInfo> cameraCache = new HashMap<>();

    @PostConstruct
    public void loadCameraData() {
        try {
            ClassPathResource resource = new ClassPathResource("vancouver-cameras.json");
            InputStream inputStream = resource.getInputStream();
            JsonNode root = objectMapper.readTree(inputStream);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    String name = node.get("name").asText();
                    String url = node.get("url").asText();
                    String mapId = node.get("mapid").asText();
                    double lat = node.get("geo_point_2d").get("lat").asDouble();
                    double lon = node.get("geo_point_2d").get("lon").asDouble();

                    CameraInfo info = new CameraInfo(name, url, mapId, lat, lon);
                    cameraCache.put(name.toLowerCase(), info);
                    cameraCache.put(mapId.toLowerCase(), info);
                }
            }
            System.out.println("Loaded " + cameraCache.size() + " camera locations");
        } catch (Exception e) {
            System.err.println("Failed to load camera data: " + e.getMessage());
        }
    }

    public List<byte[]> fetchCameraImages(String locationId) {
        CameraInfo camera = cameraCache.get(locationId.toLowerCase());
        if (camera == null) return new ArrayList<>();

        try {
            String htmlContent = webClient.get().uri(camera.url()).retrieve().bodyToMono(String.class).block();
            if (htmlContent == null) return new ArrayList<>();

            List<String> imageUrls = extractImageUrls(htmlContent, camera.url());
            List<byte[]> images = new ArrayList<>();

            for (int i = 0; i < Math.min(4, imageUrls.size()); i++) {
                try {
                    byte[] imageBytes = webClient.get().uri(imageUrls.get(i)).retrieve().bodyToMono(byte[].class).block();
                    if (imageBytes != null) images.add(imageBytes);
                } catch (Exception e) {
                    System.err.println("Failed to download image: " + imageUrls.get(i));
                }
            }
            return images;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> extractImageUrls(String html, String baseUrl) {
        List<String> urls = new ArrayList<>();
        String base = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1);
        
        // Slightly more robust than simple split
        String[] parts = html.split("<img");
        for (String part : parts) {
            if (part.contains("src=")) {
                try {
                    int start = part.indexOf("src=\"") + 5;
                    int end = part.indexOf("\"", start);
                    String url = part.substring(start, end);

                    if (url.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) {
                        if (!url.startsWith("http")) {
                            url = url.startsWith("/") ? "https://trafficcams.vancouver.ca" + url : base + url;
                        }
                        urls.add(url);
                    }
                } catch (Exception e) { /* skip malformed tags */ }
            }
        }
        return urls;
    }

    public List<HazardTag> detectHazards(byte[] imageBytes) {
        try {
            String jsonResponse = webClient.post()
                .uri(hfUrl) 
                .header("Authorization", "Bearer " + hfToken)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(imageBytes)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // Note: Use "score" in HazardTag because that is standard HF output
            return objectMapper.readValue(jsonResponse, new TypeReference<List<HazardTag>>(){});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Coordinates getCameraCoordinates(String locationId) {
        CameraInfo camera = cameraCache.get(locationId.toLowerCase());
        if (camera != null) return new Coordinates(camera.lat(), camera.lon());
        return new Coordinates(49.2827, -123.1207);
    }

    public double fetchHistoricalAverage(String locationId) {
        try {
            String response = webClient.get()
                .uri(supabaseUrl + "/hazards?location_id=eq." + locationId + "&order=timestamp.desc&limit=50")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .retrieve()
                .bodyToMono(String.class).block();

            JsonNode records = objectMapper.readTree(response);
            if (!records.isArray() || records.isEmpty()) return 0.0;

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
            return 0.0;
        }
    }

    public String generateDescription(List<HazardTag> tags, boolean isSpike, double score) {
        String tagSummary = tags.stream()
                                .map(t -> t.label() + " (" + String.format("%.2f", t.score()) + ")")
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("No specific hazards");

        String prompt = isSpike 
            ? String.format("A significant spike in traffic hazards detected. Tags: %s. Score: %.2f. Identify the cause and impact concisely (2-3 sentences).", tagSummary, score)
            : String.format("Factual summary of traffic conditions. Tags: %s. Score: %.2f. Describe the situation in 2-3 sentences.", tagSummary, score);

        Map<String, Object> body = Map.of(
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "model", "deepseek-chat",
            "max_tokens", 150
        );

        try {
            String response = webClient.post()
                .uri(deepSeekUrl)
                .header("Authorization", "Bearer " + deepSeekToken)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class).block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            return "AI Analysis unavailable.";
        }
    }

    // --- Simple Builders for compatibility ---
    private static class CameraInfoBuilder {
        private String name, url, mapId; double lat, lon;
        public CameraInfoBuilder name(String n) { this.name = n; return this; }
        public CameraInfoBuilder url(String u) { this.url = u; return this; }
        public CameraInfoBuilder mapId(String m) { this.mapId = m; return this; }
        public CameraInfoBuilder lat(double l) { this.lat = l; return this; }
        public CameraInfoBuilder lon(double l) { this.lon = l; return this; }
        public CameraInfo build() { return new CameraInfo(name, url, mapId, lat, lon); }
    }

    private static class CoordinatesBuilder {
        private double lat, lng;
        public CoordinatesBuilder lat(double l) { this.lat = l; return this; }
        public CoordinatesBuilder lng(double l) { this.lng = l; return this; }
        public Coordinates build() { return new Coordinates(lat, lng); }
    }
}