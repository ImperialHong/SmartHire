package com.smarthire.modules.statistics.service;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.statistics.dto.response.StatisticsOverviewResponse;

public interface StatisticsService {

    StatisticsOverviewResponse getOverview(AuthenticatedUser currentUser);
}
