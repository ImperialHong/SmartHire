package com.smarthire.modules.application.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.application.dto.request.ApplicationCreateRequest;
import com.smarthire.modules.application.dto.request.ApplicationListRequest;
import com.smarthire.modules.application.dto.request.ApplicationStatusUpdateRequest;
import com.smarthire.modules.application.dto.response.ApplicationResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;

public interface ApplicationService {

    ApplicationResponse apply(AuthenticatedUser currentUser, ApplicationCreateRequest request);

    PageResponse<ApplicationResponse> listMyApplications(
        AuthenticatedUser currentUser,
        ApplicationListRequest request
    );

    PageResponse<ApplicationResponse> listJobApplications(
        AuthenticatedUser currentUser,
        Long jobId,
        ApplicationListRequest request
    );

    ApplicationResponse updateStatus(
        AuthenticatedUser currentUser,
        Long applicationId,
        ApplicationStatusUpdateRequest request
    );
}
