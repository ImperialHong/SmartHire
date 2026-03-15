package com.smarthire.modules.operationlog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.operationlog.dto.request.OperationLogListRequest;
import com.smarthire.modules.operationlog.dto.response.OperationLogResponse;
import com.smarthire.modules.operationlog.entity.OperationLogEntity;
import com.smarthire.modules.operationlog.mapper.OperationLogMapper;
import com.smarthire.modules.operationlog.service.OperationLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void record(
        AuthenticatedUser currentUser,
        String action,
        String targetType,
        Long targetId,
        String details
    ) {
        if (currentUser == null) {
            return;
        }

        OperationLogEntity operationLog = new OperationLogEntity();
        operationLog.setOperatorUserId(currentUser.userId());
        operationLog.setOperatorEmail(currentUser.email());
        operationLog.setOperatorName(currentUser.fullName());
        operationLog.setOperatorRoles(String.join(",", currentUser.roles()));
        operationLog.setAction(action.trim().toUpperCase());
        operationLog.setTargetType(targetType.trim().toUpperCase());
        operationLog.setTargetId(targetId);
        operationLog.setDetails(details.trim());
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
    }

    @Override
    public PageResponse<OperationLogResponse> listLogs(
        AuthenticatedUser currentUser,
        OperationLogListRequest request
    ) {
        validateAdminRole(currentUser);

        var query = Wrappers.<OperationLogEntity>lambdaQuery()
            .orderByDesc(OperationLogEntity::getCreatedAt);

        if (StringUtils.hasText(request.action())) {
            query.eq(OperationLogEntity::getAction, request.action().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.targetType())) {
            query.eq(OperationLogEntity::getTargetType, request.targetType().trim().toUpperCase());
        }
        if (request.operatorUserId() != null) {
            query.eq(OperationLogEntity::getOperatorUserId, request.operatorUserId());
        }

        Page<OperationLogEntity> page = operationLogMapper.selectPage(
            new Page<>(request.page(), request.size()),
            query
        );
        List<OperationLogResponse> records = page.getRecords().stream()
            .map(OperationLogResponse::fromEntity)
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private void validateAdminRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null || !currentUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only admin can view operation logs");
        }
    }
}
