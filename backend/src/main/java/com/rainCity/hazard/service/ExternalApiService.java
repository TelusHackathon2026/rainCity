package com.rainCity.hazard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainCity.hazard.model.HazardModels.*;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CameraInfo> cameraCache = new HashMap<>();

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
            System.out.println("✅ Loaded " + cameraCache.size() + " cameras");
        } catch (Exception e) {
            System.err.println("❌ Load Error: " + e.getMessage());
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

            while (m.find() && images.size() < 1) {
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

            System.out.println("✅ Fetched " + images.size() + " image(s)");
            return images;
        } catch (Exception e) {
            System.err.println("Fetch error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // HF Gradio Space API - 2-step process
    public List<HazardTag> detectHazards(byte[] imageBytes) {
        try {
            System.out.println("🤖 Calling HF Gradio Space API (Step 1: POST)...");

            // Step 1: Convert image to base64 and create Gradio-compatible request
            String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> imageData = Map.of("path", base64Image, "meta", Map.of("_type", "gradio.FileData"));

            Map<String, Object> requestBody = Map.of("data", List.of(imageData));

            // POST to get event ID
            String postResponse = webClient
                    .post()
                    .uri("https://sdl11-intersection-hazard-api.hf.space/gradio_api/call/detect_hazards")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (postResponse == null) {
                System.err.println("❌ No response from HF Space POST");
                return Collections.emptyList();
            }

            System.out.println("📥 POST Response: " + postResponse);

            // Extract event_id from response
            JsonNode postJson = objectMapper.readTree(postResponse);
            String eventId = postJson.get("event_id").asText();

            System.out.println("✅ Got event ID: " + eventId);
            System.out.println("🤖 Step 2: GET results...");

            // Step 2: GET results using event ID
            // Wait a bit for processing
            Thread.sleep(1000);

            String getResponse = webClient
                    .get()
                    .uri(
                            "https://sdl11-intersection-hazard-api.hf.space/gradio_api/call/detect_hazards/"
                                    + eventId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (getResponse == null) {
                System.err.println("❌ No response from HF Space GET");
                return Collections.emptyList();
            }

            System.out.println(
                    "📥 GET Response: " + getResponse.substring(0, Math.min(500, getResponse.length())));

            // Parse the streaming response (it comes as SSE - Server Sent Events)
            List<HazardTag> detections = new ArrayList<>();
            String[] lines = getResponse.split("\n");

            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6); // Remove "data: " prefix
                    try {
                        JsonNode eventData = objectMapper.readTree(jsonData);

                        // Look for the final result
                        if (eventData.isArray() && eventData.size() >= 2) {
                            JsonNode detectionData = eventData.get(1); // Second element contains detection data

                            if (detectionData.isObject()) {
                                parseDetectionResults(detectionData, detections);
                            } else if (detectionData.isArray()) {
                                for (JsonNode item : detectionData) {
                                    parseDetection(item, detections);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Skip invalid JSON lines
                    }
                }
            }

            System.out.println("✅ Parsed " + detections.size() + " detections");
            return detections;

        } catch (Exception e) {
            System.err.println("❌ HF Detection error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void parseDetectionResults(JsonNode node, List<HazardTag> detections) {
        // Check for common detection result formats
        if (node.has("predictions") && node.get("predictions").isArray()) {
            for (JsonNode pred : node.get("predictions")) {
                parseDetection(pred, detections);
            }
        } else if (node.has("detections") && node.get("detections").isArray()) {
            for (JsonNode det : node.get("detections")) {
                parseDetection(det, detections);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                parseDetection(item, detections);
            }
        }
    }

    private void parseDetection(JsonNode node, List<HazardTag> detections) {
        String label = null;
        double confidence = 0.0;

        if (node.has("label"))
            label = node.get("label").asText();
        else if (node.has("class"))
            label = node.get("class").asText();
        else if (node.has("name"))
            label = node.get("name").asText();

        if (node.has("confidence"))
            confidence = node.get("confidence").asDouble();
        else if (node.has("score"))
            confidence = node.get("score").asDouble();
        else if (node.has("probability"))
            confidence = node.get("probability").asDouble();

        if (label != null) {
            detections.add(new HazardTag(label, confidence));
        }
    }

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

    public String generateDescription(List<HazardTag> detections, boolean isSpike, double score) {
        try {
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
                    "model",
                    "deepseek-ai/DeepSeek-V3",
                    "prompt",
                    prompt,
                    "max_tokens",
                    300,
                    "temperature",
                    0.7);

            System.out.println("🧠 Calling DeepSeek...");

            String response = webClient
                    .post()
                    .uri(deepSeekUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekToken)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                System.err.println("❌ No response from DeepSeek");
                return "AI Analysis unavailable.";
            }

            System.out.println("📥 DeepSeek response received");

            JsonNode root = objectMapper.readTree(response);

            if (root.has("choices") && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("text")) {
                    return choice.get("text").asText().trim();
                }
            }

            return "AI Analysis unavailable.";

        } catch (Exception e) {
            System.err.println("DeepSeek error: " + e.getMessage());
            e.printStackTrace();
            return "AI Analysis unavailable.";
        }
    }

    public Coordinates getCameraCoordinates(String locationId) {
        CameraInfo camera = cameraCache.get(locationId.toLowerCase());
        if (camera != null) {
            return Coordinates.builder().lat(camera.lat()).lng(camera.lon()).build();
        }
        return Coordinates.builder().lat(49.2827).lng(-123.1207).build();
    }

    public void saveHazardRecord(HazardResponse hazard) {
        try {
            // Create the payload for Supabase
            // Ensure keys match your Supabase column names exactly
            Map<String, Object> payload = new HashMap<>();
            payload.put("location_id", hazard.getLocationString()); // Using name as ID
            payload.put("score", hazard.getScore());
            payload.put("description", hazard.getDescription());
            payload.put("spike", hazard.isSpike());
            payload.put("timestamp", hazard.getTimestamp());
            payload.put("lat", hazard.getCoords().getLat());
            payload.put("lng", hazard.getCoords().getLng());

            System.out.println("💾 Saving/Updating hazard for: " + hazard.getLocationString());

            webClient
                    .post()
                    .uri(
                            supabaseUrl
                                    + "/hazards?on_conflict=location_id") // location_id must be UNIQUE in Supabase
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates") // This handles the "Update" part
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("✅ Database updated successfully.");
        } catch (Exception e) {
            System.err.println("❌ Database save error: " + e.getMessage());
        }
    }
}
