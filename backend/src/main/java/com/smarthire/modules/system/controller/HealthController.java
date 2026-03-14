package com.smarthire.modules.system.controller;

import com.smarthire.common.api.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(
            "Service is up",
            Map.of(
                "service", "smarthire-backend",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
            )
        );
    }
}
