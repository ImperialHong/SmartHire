package com.smarthire.modules.job.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobCreateRequest;
import com.smarthire.modules.job.dto.request.JobSearchRequest;
import com.smarthire.modules.job.dto.request.JobUpdateRequest;
import com.smarthire.modules.job.dto.response.JobResponse;

public interface JobService {

    JobResponse createJob(AuthenticatedUser currentUser, JobCreateRequest request);

    JobResponse updateJob(AuthenticatedUser currentUser, Long jobId, JobUpdateRequest request);

    void deleteJob(AuthenticatedUser currentUser, Long jobId);

    JobResponse getJob(Long jobId);

    PageResponse<JobResponse> listJobs(JobSearchRequest request);
}
