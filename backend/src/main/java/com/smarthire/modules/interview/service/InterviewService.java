package com.smarthire.modules.interview.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.interview.dto.request.InterviewCreateRequest;
import com.smarthire.modules.interview.dto.request.InterviewListRequest;
import com.smarthire.modules.interview.dto.request.InterviewUpdateRequest;
import com.smarthire.modules.interview.dto.response.InterviewResponse;

public interface InterviewService {

    InterviewResponse scheduleInterview(AuthenticatedUser currentUser, InterviewCreateRequest request);

    InterviewResponse updateInterview(
        AuthenticatedUser currentUser,
        Long interviewId,
        InterviewUpdateRequest request
    );

    PageResponse<InterviewResponse> listMyInterviews(
        AuthenticatedUser currentUser,
        InterviewListRequest request
    );

    PageResponse<InterviewResponse> listJobInterviews(
        AuthenticatedUser currentUser,
        Long jobId,
        InterviewListRequest request
    );
}
