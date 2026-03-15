package com.smarthire.modules.operationlog.service;

import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.operationlog.dto.request.OperationLogListRequest;
import com.smarthire.modules.operationlog.dto.response.OperationLogResponse;

public interface OperationLogService {

    void record(
        AuthenticatedUser currentUser,
        String action,
        String targetType,
        Long targetId,
        String details
    );

    PageResponse<OperationLogResponse> listLogs(
        AuthenticatedUser currentUser,
        OperationLogListRequest request
    );
}
