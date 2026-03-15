package com.smarthire.modules.admin.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.admin.dto.request.AdminJobListRequest;
import com.smarthire.modules.admin.dto.response.AdminJobResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;

public interface AdminJobService {

    PageResponse<AdminJobResponse> listJobs(AuthenticatedUser currentUser, AdminJobListRequest request);
}
