package com.template.auth.application.auditlog.query;

import com.template.auth.application.auditlog.AuditLogDto;
import com.template.auth.application.auditlog.AuditLogRepository;
import com.template.auth.application.shared.PaginatedResult;
import org.springframework.stereotype.Service;

@Service
public class ListAuditLogsHandler {

    private final AuditLogRepository auditLogRepository;

    public ListAuditLogsHandler(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PaginatedResult<AuditLogDto> execute(ListAuditLogsQuery query) {
        return auditLogRepository.findAll(query.pagination()).map(AuditLogDto::from);
    }
}
