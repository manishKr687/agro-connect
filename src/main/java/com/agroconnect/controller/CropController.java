package com.agroconnect.controller;

import com.agroconnect.service.CropNormalizerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropNormalizerClient cropNormalizerClient;

    @GetMapping("/normalize")
    public Map<String, Object> normalize(@RequestParam String name) {
        return cropNormalizerClient.normalizeWithMeta(name);
    }
}
