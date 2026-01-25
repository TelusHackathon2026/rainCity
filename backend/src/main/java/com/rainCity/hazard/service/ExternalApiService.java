package com.rainCity.hazard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainCity.hazard.model.HazardModels.*;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.*;
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

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CameraInfo> cameraCache = new HashMap<>();

    // HazardTag record for detections
    public record HazardTag(String label, double confidence) {
    }

    record CameraInfo(String name, String url, String mapId, double lat, double lon) {
    }

    @PostConstruct
    public void loadCameraData() {
        try {
            JsonNode root = objectMapper.readTree(new ClassPathResource("data.json").getInputStream());
            for (JsonNode node : root) {
                CameraInfo info = new CameraInfo(
                        node.get("name").asText(),
                        node.get("url").asText(),
                        node.get("mapid").asText(),
                        node.get("geo_point_2d").get("lat").asDouble(),
                        node.get("geo_point_2d").get("lon").asDouble());
                cameraCache.put(info.name().toLowerCase(), info);
            }
            System.out.println("Loaded " + cameraCache.size() + " cameras");
        } catch (Exception e) {
            System.err.println("Load Error: " + e.getMessage());
        }
    }

    public List<byte[]> fetchCameraImages(String locationId) {
        CameraInfo camera = cameraCache.get(locationId.toLowerCase());
        if (camera == null)
            return Collections.emptyList();

        try {
            String html = webClient
                    .get()
                    .uri(camera.url())
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<byte[]> images = new ArrayList<>();
            Matcher m = Pattern.compile("<img[^>]+src=['\"]([^'\"]+)['\"]").matcher(html);

            while (m.find() && images.size() < 1) { // Pulling 1 good image for analysis
                String src = m.group(1);
                if (!src.startsWith("http"))
                    src = "https://trafficcams.vancouver.ca" + (src.startsWith("/") ? "" : "/") + src;

                byte[] img = webClient
                        .get()
                        .uri(src)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://trafficcams.vancouver.ca/")
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();

                if (img != null && img.length > 2000)
                    images.add(img);
            }
            return images;
        } catch (Exception e) {
            System.err.println("Fetch error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Detect hazards using Hugging Face
    public List<HazardTag> detectHazards(byte[] imageBytes) {
        try {
            String jsonResponse = webClient
                    .post()
                    .uri(hfUrl)
                    .header("Authorization", "Bearer " + hfToken)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .bodyValue(imageBytes)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonResponse == null)
                return Collections.emptyList();

            JsonNode root = objectMapper.readTree(jsonResponse);
            List<HazardTag> detections = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.has("label") && node.has("score")) {
                        detections.add(new HazardTag(node.get("label").asText(), node.get("score").asDouble()));
                    }
                }
            }

            return detections;
        } catch (Exception e) {
            System.err.println("HF Detection error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Fetch historical average from Supabase
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
            System.err.println("Historical fetch error: " + e.getMessage());
            return 0.0;
        }
    }

    // Generate description using DeepSeek
    public String generateDescription(List<HazardTag> detections, boolean isSpike, double score) {
        String tagsStr = detections.stream()
                .map(t -> t.label() + " (" + String.format("%.2f", t.confidence()) + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        String prompt = "Analyze these traffic hazard detections: "
                + tagsStr
                + ". Hazard Score: "
                + score
                + ". Spike detected: "
                + isSpike
                + ". Provide a 2-3 sentence description for police officers. "
                + "If spike detected, explain urgency. Be concise and actionable.";

        Map<String, Object> body = Map.of(
                "messages",
                List.of(Map.of("role", "user", "content", prompt)),
                "model",
                "deepseek-chat",
                "max_tokens",
                150,
                "temperature",
                0.7);

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
            System.err.println("DeepSeek error: " + e.getMessage());
            return "AI Analysis unavailable.";
        }
    }

    // Get camera coordinates
    public Coordinates getCameraCoordinates(String locationId) {
        CameraInfo camera = cameraCache.get(locationId.toLowerCase());
        if (camera != null) {
            return Coordinates.builder().lat(camera.lat()).lng(camera.lon()).build();
        }
        // Default to downtown Vancouver
        return Coordinates.builder().lat(49.2827).lng(-123.1207).build();
    }
}
