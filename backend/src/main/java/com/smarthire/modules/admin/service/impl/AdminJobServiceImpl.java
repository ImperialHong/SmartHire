package com.smarthire.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthire.common.api.PageResponse;
import com.smarthire.common.exception.BusinessException;
import com.smarthire.modules.admin.dto.request.AdminJobListRequest;
import com.smarthire.modules.admin.dto.response.AdminJobResponse;
import com.smarthire.modules.admin.service.AdminJobService;
import com.smarthire.modules.auth.entity.UserEntity;
import com.smarthire.modules.auth.mapper.UserMapper;
import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.entity.JobEntity;
import com.smarthire.modules.job.mapper.JobMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminJobServiceImpl implements AdminJobService {

    private static final Set<String> JOB_STATUSES = Set.of("OPEN", "CLOSED", "EXPIRED");

    private final JobMapper jobMapper;
    private final UserMapper userMapper;

    public AdminJobServiceImpl(JobMapper jobMapper, UserMapper userMapper) {
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResponse<AdminJobResponse> listJobs(AuthenticatedUser currentUser, AdminJobListRequest request) {
        validateAdminRole(currentUser);

        List<Long> ownerFilteredIds = resolveOwnerFilteredIds(request.ownerKeyword());
        if (ownerFilteredIds != null && ownerFilteredIds.isEmpty()) {
            return PageResponse.of(List.of(), request.page(), request.size(), 0);
        }

        var query = Wrappers.<JobEntity>lambdaQuery()
            .orderByDesc(JobEntity::getUpdatedAt)
            .orderByDesc(JobEntity::getCreatedAt);

        if (StringUtils.hasText(request.keyword())) {
            String keyword = request.keyword().trim();
            query.and(wrapper -> wrapper
                .like(JobEntity::getTitle, keyword)
                .or()
                .like(JobEntity::getDescription, keyword)
                .or()
                .like(JobEntity::getCity, keyword)
                .or()
                .like(JobEntity::getCategory, keyword)
            );
        }

        if (StringUtils.hasText(request.status())) {
            query.eq(JobEntity::getStatus, normalizeStatus(request.status()));
        }

        if (ownerFilteredIds != null) {
            query.in(JobEntity::getCreatedBy, ownerFilteredIds);
        }

        Page<JobEntity> pageRequest = new Page<>(request.page(), request.size());
        Page<JobEntity> page = jobMapper.selectPage(pageRequest, query);
        Map<Long, UserEntity> ownersById = loadOwnersById(page.getRecords().stream()
            .map(JobEntity::getCreatedBy)
            .distinct()
            .toList());

        List<AdminJobResponse> records = page.getRecords().stream()
            .map(job -> AdminJobResponse.fromEntities(job, ownersById.get(job.getCreatedBy())))
            .toList();

        return PageResponse.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private List<Long> resolveOwnerFilteredIds(String ownerKeyword) {
        if (!StringUtils.hasText(ownerKeyword)) {
            return null;
        }

        String keyword = ownerKeyword.trim();
        return userMapper.selectList(
            Wrappers.<UserEntity>lambdaQuery()
                .and(wrapper -> wrapper
                    .like(UserEntity::getEmail, keyword)
                    .or()
                    .like(UserEntity::getFullName, keyword)
                )
        ).stream()
            .map(UserEntity::getId)
            .distinct()
            .toList();
    }

    private Map<Long, UserEntity> loadOwnersById(List<Long> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }

        return userMapper.selectBatchIds(ownerIds).stream()
            .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String normalizeStatus(String rawStatus) {
        String normalizedStatus = rawStatus.trim().toUpperCase();
        if (!JOB_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException("INVALID_JOB_STATUS", "Invalid job status");
        }
        return normalizedStatus;
    }

    private void validateAdminRole(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null || !currentUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only admin can review jobs");
        }
    }
}
