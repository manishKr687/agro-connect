package com.agroconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Calls the crop-normalizer Python microservice to convert Hinglish crop names
 * to their canonical English equivalents before persisting to the database.
 *
 * <p>If the microservice is unavailable the original input is returned unchanged
 * so that the main flow is never blocked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CropNormalizerClient {

    private final RestTemplate restTemplate;

    @Value("${crop.normalizer.url:http://localhost:8000}")
    private String baseUrl;

    record NormalizeRequest(String crop_name) {}
    record NormalizeResponse(String normalized, boolean corrected, boolean valid) {}

    /**
     * Returns the normalized (English) version of the given crop name.
     * Falls back to the original value if the microservice call fails.
     */
    public String normalize(String cropName) {
        if (cropName == null || cropName.isBlank()) {
            return cropName;
        }
        try {
            NormalizeResponse response = restTemplate.postForObject(
                    baseUrl + "/normalize",
                    new NormalizeRequest(cropName.trim()),
                    NormalizeResponse.class
            );
            if (response != null && response.normalized() != null) {
                if (response.corrected()) {
                    log.debug("Crop name normalized: '{}' -> '{}'", cropName, response.normalized());
                }
                return response.normalized();
            }
        } catch (Exception ex) {
            log.warn("Crop normalizer unavailable, using raw input '{}': {}", cropName, ex.getMessage());
        }
        return cropName;
    }

    /**
     * Returns normalized name plus metadata (corrected, valid) for the frontend.
     * Falls back to {normalized: original, corrected: false, valid: true} if unavailable.
     */
    public Map<String, Object> normalizeWithMeta(String cropName) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("normalized", cropName);
        fallback.put("corrected", false);
        fallback.put("valid", true);

        if (cropName == null || cropName.isBlank()) return fallback;

        try {
            NormalizeResponse response = restTemplate.postForObject(
                    baseUrl + "/normalize",
                    new NormalizeRequest(cropName.trim()),
                    NormalizeResponse.class
            );
            if (response != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("normalized", response.normalized());
                result.put("corrected", response.corrected());
                result.put("valid", response.valid());
                return result;
            }
        } catch (Exception ex) {
            log.warn("Crop normalizer unavailable for meta call '{}': {}", cropName, ex.getMessage());
        }
        return fallback;
    }
}
