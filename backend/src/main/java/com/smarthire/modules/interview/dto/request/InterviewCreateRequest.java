package com.smarthire.modules.interview.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record InterviewCreateRequest(
    @NotNull(message = "applicationId is required")
    Long applicationId,

    @NotNull(message = "interviewAt is required")
    LocalDateTime interviewAt,

    @Size(max = 255, message = "location length must be less than or equal to 255")
    String location,

    @Size(max = 255, message = "meetingLink length must be less than or equal to 255")
    String meetingLink,

    @Size(max = 500, message = "remark length must be less than or equal to 500")
    String remark
) {
}
