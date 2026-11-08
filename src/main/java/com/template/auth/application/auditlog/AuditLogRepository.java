package com.template.auth.application.auditlog;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.auditlog.AuditLogEntity;

public interface AuditLogRepository {

    AuditLogEntity create(AuditLogEntity log);

    /** Newest first. */
    PaginatedResult<AuditLogEntity> findAll(PaginationParams params);
}
