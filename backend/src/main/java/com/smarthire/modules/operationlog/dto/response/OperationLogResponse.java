package com.smarthire.modules.operationlog.dto.response;

import com.smarthire.modules.operationlog.entity.OperationLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.util.StringUtils;

public record OperationLogResponse(
    Long id,
    Long operatorUserId,
    String operatorEmail,
    String operatorName,
    List<String> operatorRoles,
    String action,
    String targetType,
    Long targetId,
    String details,
    LocalDateTime createdAt
) {

    public static OperationLogResponse fromEntity(OperationLogEntity entity) {
        List<String> roles = StringUtils.hasText(entity.getOperatorRoles())
            ? List.of(entity.getOperatorRoles().split(","))
            : List.of();
        return new OperationLogResponse(
            entity.getId(),
            entity.getOperatorUserId(),
            entity.getOperatorEmail(),
            entity.getOperatorName(),
            roles,
            entity.getAction(),
            entity.getTargetType(),
            entity.getTargetId(),
            entity.getDetails(),
            entity.getCreatedAt()
        );
    }
}
