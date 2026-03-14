package com.smarthire.modules.interview.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record InterviewUpdateRequest(
    LocalDateTime interviewAt,

    @Size(max = 255, message = "location length must be less than or equal to 255")
    String location,

    @Size(max = 255, message = "meetingLink length must be less than or equal to 255")
    String meetingLink,

    String status,
    String result,

    @Size(max = 500, message = "remark length must be less than or equal to 500")
    String remark
) {
}
