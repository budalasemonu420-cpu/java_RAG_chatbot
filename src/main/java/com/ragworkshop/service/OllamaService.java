package com.ragworkshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragworkshop.exception.OllamaConnectionException;
import com.ragworkshop.model.OllamaModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class OllamaService {
    private final ObjectMapper mapper; private final HttpClient client; private final String baseUrl;
    public OllamaService(ObjectMapper mapper, @Value("${ollama.base-url:http://localhost:11434}") String baseUrl) { this.mapper = mapper; this.baseUrl = baseUrl.replaceAll("/$", ""); this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(); }
    public List<OllamaModel> models() {
        try { HttpResponse<String> response = client.send(request("/api/tags", "GET", null), HttpResponse.BodyHandlers.ofString()); if (response.statusCode() >= 300) return List.of(); JsonNode root = mapper.readTree(response.body()); List<OllamaModel> result = new ArrayList<>(); for (JsonNode node : root.path("models")) result.add(new OllamaModel(node.path("name").asText(), node.path("details").path("family").asText(""))); return result; }
        catch (Exception exception) { return List.of(); }
    }
    public boolean connected() { try { return client.send(request("/api/tags", "GET", null), HttpResponse.BodyHandlers.discarding()).statusCode() < 500; } catch (Exception exception) { return false; } }
    public String generate(String model, String prompt, double temperature) {
        try { String body = mapper.writeValueAsString(Map.of("model", model, "prompt", prompt, "stream", false, "options", Map.of("temperature", temperature))); HttpResponse<String> response = client.send(request("/api/generate", "POST", body), HttpResponse.BodyHandlers.ofString()); if (response.statusCode() >= 300) throw new OllamaConnectionException("Ollama generation failed with HTTP " + response.statusCode()); return mapper.readTree(response.body()).path("response").asText(); }
        catch (OllamaConnectionException exception) { throw exception; } catch (Exception exception) { throw new OllamaConnectionException("Ollama is not reachable or timed out.", exception); }
    }
    private HttpRequest request(String path, String method, String body) { HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofMinutes(3)); if ("POST".equals(method)) builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)); else builder.GET(); return builder.build(); }
}
