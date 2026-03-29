package com.example.nextjsbackendadmin.supabase;

import com.example.nextjsbackendadmin.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Supabase REST client:
 * - PostgREST: {url}/rest/v1
 * - Storage:   {url}/storage/v1
 *
 * Uses service-role key for admin operations.
 */
@Component
public class SupabaseClient {

    private final AppProperties props;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseClient(AppProperties props, WebClient webClient) {
        this.props = props;
        this.webClient = webClient;
    }

    private String restUrl(String path) {
        return props.url() + "/rest/v1" + path;
    }

    private String storageUrl(String path) {
        return props.url() + "/storage/v1" + path;
    }

    private Mono<SupabaseException> toSupabaseException(ClientResponse resp) {
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new SupabaseException("Supabase error (" + resp.statusCode().value() + "): " + body, resp.statusCode().value()));
    }

    /**
     * Query the gallery table using PostgREST (GET).
     */
    public Mono<JsonNode> getGalleryRows(String table, String query) {
        String url = restUrl("/" + table + (query == null ? "" : query));
        return webClient.get()
                .uri(url)
                .header("apikey", props.serviceRoleKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(String.class).map(this::readJson);
                    }
                    return toSupabaseException(resp).flatMap(Mono::error);
                });
    }

    /**
     * Insert a row (POST) and return created row(s) as JSON.
     */
    public Mono<JsonNode> insertRow(String table, String jsonBody) {
        String url = restUrl("/" + table);
        return webClient.post()
                .uri(url)
                .header("apikey", props.serviceRoleKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                // Return inserted rows
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(String.class).map(this::readJson);
                    }
                    return toSupabaseException(resp).flatMap(Mono::error);
                });
    }

    /**
     * Delete rows by PostgREST filter query (e.g. "?id=eq.<uuid>").
     */
    public Mono<Void> deleteRows(String table, String filterQuery) {
        String url = restUrl("/" + table + filterQuery);
        return webClient.delete()
                .uri(url)
                .header("apikey", props.serviceRoleKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    return toSupabaseException(resp).flatMap(Mono::error);
                });
    }

    /**
     * Upload file to Supabase Storage using multipart/form-data.
     *
     * Returns JSON response from storage API (not strictly needed; public URL can be derived).
     */
    public Mono<JsonNode> uploadToBucket(String bucket, String objectPath, byte[] bytes, String contentType, boolean upsert) {
        String encodedPath = encodePath(objectPath);
        String url = storageUrl("/object/" + bucket + "/" + encodedPath);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        // Supabase ignores filename in many cases, but provide something.
                        return "upload";
                    }
                })
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType));

        MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipart = builder.build();

        return webClient.post()
                .uri(url)
                .header("apikey", props.serviceRoleKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                .header("x-upsert", upsert ? "true" : "false")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(multipart))
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(String.class).map(this::readJson);
                    }
                    return toSupabaseException(resp).flatMap(Mono::error);
                });
    }

    /**
     * Delete an object from Supabase Storage.
     */
    public Mono<Void> deleteObject(String bucket, String objectPath) {
        String encodedPath = encodePath(objectPath);
        String url = storageUrl("/object/" + bucket + "/" + encodedPath);

        return webClient.delete()
                .uri(url)
                .header("apikey", props.serviceRoleKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.serviceRoleKey())
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    return toSupabaseException(resp).flatMap(Mono::error);
                });
    }

    /**
     * Derive a public URL for an object.
     *
     * Note: bucket must be public OR you must use signed URLs (not implemented here).
     */
    public String publicObjectUrl(String bucket, String objectPath) {
        return props.url() + "/storage/v1/object/public/" + bucket + "/" + objectPath;
    }

    private JsonNode readJson(String s) {
        try {
            if (s == null || s.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(s);
        } catch (Exception ex) {
            // If Supabase returns non-JSON success body, wrap it.
            return objectMapper.createObjectNode().put("raw", s);
        }
    }

    private String encodePath(String path) {
        // Encode each segment but keep slashes
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("/");
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
