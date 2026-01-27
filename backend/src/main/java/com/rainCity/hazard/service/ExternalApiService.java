package com.rainCity.hazard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainCity.hazard.model.HazardModels.*;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.Random;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    private final String CUSTOM_IMAGES_FOLDER = "custom-images";

    public record HazardTag(String label, double confidence) {
    }

    public record HazardDetectionResult(byte[] labeledImage, List<HazardTag> detections) {
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
                cameraCache.put(info.mapId().toLowerCase(), info);
            }
            System.out.println("Loaded " + cameraCache.size() + " camera entries");
        } catch (Exception e) {
            System.err.println(" Load Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<byte[]> fetchCameraImages(String locationId) {
        CameraInfo camera = cameraCache.get(locationId.toLowerCase());

        if (camera == null) {
            System.out.println(" Camera not found in data.json: " + locationId);
            System.out.println(" Fetching random custom image...");
            return fetchCustomImages(locationId);
        }

        try {
            System.out.println(" Fetching camera: " + camera.name() + " from " + camera.url());

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

                System.out.println(" Downloading image from: " + src);

                byte[] img = webClient
                        .get()
                        .uri(src)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://trafficcams.vancouver.ca/")
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();

                if (img != null && img.length > 2000) {
                    images.add(img);
                    System.out.println(" Image downloaded: " + img.length + " bytes");
                }
            }

            System.out.println(" Fetched " + images.size() + " image(s)");
            return images;
        } catch (Exception e) {
            System.err.println(" Fetch error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private List<byte[]> fetchCustomImages(String locationId) {
        try {
            System.out.println(
                    " Location '" + locationId + "' not in data.json - fetching random custom image");

            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:custom-images/*");

            System.out.println(" Found " + resources.length + " total files in custom-images folder");

            if (resources.length == 0) {
                System.err.println(" No files found in custom-images folder");
                System.err.println(" Place images in: src/main/resources/custom-images/");
                System.err.println(" Supported formats: .jpg, .jpeg, .png, .webp");
                return Collections.emptyList();
            }

            List<Resource> imageResources = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                System.out.println("   🔍 Checking file: " + filename);

                if (filename != null) {
                    String lowerFilename = filename.toLowerCase();
                    if (lowerFilename.endsWith(".jpg")
                            || lowerFilename.endsWith(".jpeg")
                            || lowerFilename.endsWith(".png")
                            || lowerFilename.endsWith(".webp")) {
                        imageResources.add(resource);
                        System.out.println("    Added image: " + filename);
                    }
                }
            }

            if (imageResources.isEmpty()) {
                System.err.println(" No image files found in custom-images folder");
                System.err.println(
                        " Found "
                                + resources.length
                                + " files, but none were images (.jpg, .jpeg, .png, .webp)");

                for (Resource r : resources) {
                    System.err.println("   - " + r.getFilename());
                }

                return Collections.emptyList();
            }

            System.out.println(" Found " + imageResources.size() + " custom images");

            Random random = new Random();
            Resource randomResource = imageResources.get(random.nextInt(imageResources.size()));

            System.out.println(" Randomly selected: " + randomResource.getFilename());

            byte[] imageBytes = randomResource.getInputStream().readAllBytes();

            System.out.println(" Loaded random custom image (" + imageBytes.length + " bytes)");
            return List.of(imageBytes);

        } catch (Exception e) {
            System.err.println(" Error fetching random custom image: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public HazardDetectionResult detectHazards(byte[] imageBytes) {
        try {
            System.out.println(" Starting Gradio API call...");
            System.out.println(" Image size: " + imageBytes.length + " bytes");

            if (imageBytes.length > 500000) {
                System.out.println(
                        " WARNING: Image is large (" + imageBytes.length + " bytes), this might cause issues");
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:image/jpeg;base64," + base64Image;
            System.out.println(" Base64 encoded (length: " + base64Image.length() + ")");

            Map<String, Object> imageDict = new HashMap<>();
            imageDict.put("path", null);
            imageDict.put("url", dataUrl);
            imageDict.put("size", imageBytes.length);
            imageDict.put("orig_name", "camera_image.jpg");
            imageDict.put("mime_type", "image/jpeg");
            imageDict.put("is_stream", false);
            imageDict.put("meta", Map.of("_type", "gradio.FileData"));

            Map<String, Object> requestBody = Map.of("data", List.of(imageDict));

            System.out.println(" Sending POST request to Gradio...");

            String postResponse = webClient
                    .post()
                    .uri("https://sdl11-intersection-hazard-api.hf.space/gradio_api/call/detect_hazards")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse
                                    .bodyToMono(String.class)
                                    .flatMap(
                                            errorBody -> {
                                                System.err.println(
                                                        "❌ Gradio POST Error ("
                                                                + clientResponse.statusCode()
                                                                + "): "
                                                                + errorBody);
                                                return Mono.error(
                                                        new RuntimeException("Gradio API error: " + errorBody));
                                            }))
                    .bodyToMono(String.class)
                    .block();

            if (postResponse == null || postResponse.trim().isEmpty()) {
                System.err.println("❌ Empty response from Gradio POST");
                return new HazardDetectionResult(imageBytes, Collections.emptyList());
            }

            System.out.println(" POST Response: " + postResponse);

            JsonNode postJson = objectMapper.readTree(postResponse);

            if (!postJson.has("event_id")) {
                System.err.println("❌ No event_id in response: " + postResponse);
                return new HazardDetectionResult(imageBytes, Collections.emptyList());
            }

            String eventId = postJson.get("event_id").asText();
            System.out.println(" Got event ID: " + eventId);

            System.out.println("🤖 Waiting for processing...");

            String getResponse = null;
            int maxRetries = 10;
            int retryCount = 0;

            while (retryCount < maxRetries) {
                Thread.sleep(1000);

                System.out.println("🔄 Attempt " + (retryCount + 1) + "/" + maxRetries);

                try {
                    getResponse = webClient
                            .get()
                            .uri(
                                    "https://sdl11-intersection-hazard-api.hf.space/gradio_api/call/detect_hazards/"
                                            + eventId)
                            .retrieve()
                            .onStatus(
                                    status -> status.is4xxClientError() || status.is5xxServerError(),
                                    clientResponse -> clientResponse
                                            .bodyToMono(String.class)
                                            .flatMap(
                                                    errorBody -> {
                                                        System.err.println(
                                                                "❌ Gradio GET Error ("
                                                                        + clientResponse.statusCode()
                                                                        + "): "
                                                                        + errorBody);
                                                        return Mono.error(
                                                                new RuntimeException("Gradio GET error: " + errorBody));
                                                    }))
                            .bodyToMono(String.class)
                            .block();

                    if (getResponse != null && getResponse.contains("data:")) {
                        System.out.println("✅ Got response with data!");
                        break;
                    }

                    System.out.println(
                            "⏳ Still processing... (response: "
                                    + (getResponse != null
                                            ? getResponse.substring(0, Math.min(100, getResponse.length()))
                                            : "null")
                                    + ")");

                } catch (Exception e) {
                    System.err.println("⚠️ GET attempt failed: " + e.getMessage());
                }

                retryCount++;
            }

            if (getResponse == null || !getResponse.contains("data:")) {
                System.err.println("❌ No valid response after " + maxRetries + " attempts");
                return new HazardDetectionResult(imageBytes, Collections.emptyList());
            }

            System.out.println(" Full GET Response length: " + getResponse.length());
            System.out.println(
                    " First 500 chars: " + getResponse.substring(0, Math.min(500, getResponse.length())));

            List<HazardTag> detections = new ArrayList<>();
            byte[] labeledImage = imageBytes;
            String[] lines = getResponse.split("\n");

            System.out.println("📋 Processing " + lines.length + " response lines...");

            for (String line : lines) {
                if (line.startsWith("event: error")) {
                    System.err.println("❌❌❌ GRADIO SPACE ERROR ❌❌❌");
                    System.err.println("The Gradio Space encountered an error processing the image.");
                    System.err.println("Possible causes:");
                    System.err.println(
                            "  1. Space is sleeping - Visit https://sdl11-intersection-hazard-api.hf.space/ to"
                                    + " wake it");
                    System.err.println("  2. Model file (best.pt) is missing or corrupted");
                    System.err.println("  3. Image is too large or in wrong format");
                    System.err.println("  4. Python code has a bug");
                    System.err.println("Full error response: " + getResponse);
                    System.err.println("❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌");
                    return new HazardDetectionResult(imageBytes, Collections.emptyList());
                }

                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);

                    if (jsonData.trim().equals("null")) {
                        System.out.println("⚠️ Skipping null data line");
                        continue;
                    }

                    System.out.println(
                            "🔍 Processing data line: "
                                    + jsonData.substring(0, Math.min(200, jsonData.length())));

                    try {
                        JsonNode eventData = objectMapper.readTree(jsonData);

                        if (eventData.isArray() && eventData.size() >= 2) {
                            System.out.println("✅ Found array with " + eventData.size() + " elements");

                            JsonNode imageNode = eventData.get(0);
                            System.out.println("🖼️ Image node type: " + imageNode.getNodeType());
                            System.out.println(
                                    "🖼️ Image node content: "
                                            + imageNode
                                                    .toString()
                                                    .substring(0, Math.min(300, imageNode.toString().length())));

                            if (imageNode != null && imageNode.has("url")) {
                                String imageUrl = imageNode.get("url").asText();
                                System.out.println("📥 Downloading labeled image from: " + imageUrl);

                                byte[] downloadedImage = webClient.get().uri(imageUrl).retrieve()
                                        .bodyToMono(byte[].class).block();

                                if (downloadedImage != null && downloadedImage.length > 0) {
                                    labeledImage = downloadedImage;
                                    System.out.println(
                                            "✅ Downloaded labeled image (" + labeledImage.length + " bytes)");
                                }
                            } else if (imageNode != null && imageNode.has("path")) {
                                String path = imageNode.get("path").asText();
                                System.out.println(
                                        "🖼️ Image path: " + path.substring(0, Math.min(100, path.length())));

                                if (path.startsWith("data:image")) {
                                    String base64Data = path.split(",")[1];
                                    labeledImage = Base64.getDecoder().decode(base64Data);
                                    System.out.println(
                                            "✅ Extracted labeled image from base64 (" + labeledImage.length
                                                    + " bytes)");
                                }
                            }

                            JsonNode detectionData = eventData.get(1);
                            System.out.println("🎯 Detection node type: " + detectionData.getNodeType());
                            System.out.println("🎯 Detection data: " + detectionData.toString());

                            if (detectionData.isObject()) {
                                parseDetectionResults(detectionData, detections);
                            } else if (detectionData.isArray()) {
                                System.out.println("📊 Processing " + detectionData.size() + " detection items");
                                for (JsonNode item : detectionData) {
                                    parseDetection(item, detections);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Error parsing line: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            System.out.println(
                    "✅ Final result: "
                            + detections.size()
                            + " detections, image size: "
                            + labeledImage.length);
            return new HazardDetectionResult(labeledImage, detections);

        } catch (Exception e) {
            System.err.println("❌ CRITICAL ERROR in detectHazards: " + e.getMessage());
            e.printStackTrace();
            return new HazardDetectionResult(imageBytes, Collections.emptyList());
        }
    }

    private void parseDetectionResults(JsonNode node, List<HazardTag> detections) {
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
            System.out.println("   ➡️ Detected: " + label + " (confidence: " + confidence + ")");
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
        if (detections.isEmpty()) {
            return "No hazards detected at this location. Traffic conditions appear normal.";
        }

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
                return "Detected: " + tagsStr + ". Manual assessment recommended.";
            }

            System.out.println("📥 DeepSeek response received");

            JsonNode root = objectMapper.readTree(response);

            if (root.has("choices") && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("text")) {
                    return choice.get("text").asText().trim();
                }
            }

            return "Detected: " + tagsStr + ". Manual assessment recommended.";

        } catch (Exception e) {
            System.err.println("DeepSeek error: " + e.getMessage());
            String tagsStr = detections.stream().map(HazardTag::label).reduce((a, b) -> a + ", " + b).orElse("none");
            return "Detected: " + tagsStr + ". Score: " + score + (isSpike ? " (SPIKE ALERT)" : "");
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
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("location_id", hazard.getLocationString());
            payload.put("score", hazard.getScore());
            payload.put("description", hazard.getDescription());
            payload.put("timestamp", java.time.OffsetDateTime.now().toString());

            System.out.println(
                    "💾 Syncing: " + hazard.getLocationString() + " with score: " + hazard.getScore());

            String response = webClient
                    .post()
                    .uri(supabaseUrl + "/hazards?on_conflict=location_id")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=representation")
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> clientResponse
                                    .bodyToMono(String.class)
                                    .flatMap(
                                            errorBody -> Mono.error(
                                                    new RuntimeException(
                                                            "Supabase rejected data: " + errorBody))))
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                if (responseJson.isArray() && !responseJson.isEmpty()) {
                    String dbUuid = responseJson.get(0).get("id").asText();
                    hazard.setId(dbUuid);
                    System.out.println("✅ DB Synced! UUID: " + dbUuid + ", Score: " + hazard.getScore());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ CRITICAL SYNC ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
