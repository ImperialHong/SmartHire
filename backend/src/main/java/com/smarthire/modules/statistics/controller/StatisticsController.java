package com.smarthire.modules.statistics.controller;

import com.smarthire.common.api.ApiResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.statistics.dto.response.StatisticsOverviewResponse;
import com.smarthire.modules.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics", description = "Basic recruiting overview for HR and admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "Get the recruiting overview for the current role scope")
    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> overview(
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(statisticsService.getOverview(currentUser));
    }
}
