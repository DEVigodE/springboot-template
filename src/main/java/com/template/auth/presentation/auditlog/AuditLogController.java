package com.template.auth.presentation.auditlog;

import com.template.auth.application.auditlog.AuditLogDto;
import com.template.auth.application.auditlog.query.ListAuditLogsHandler;
import com.template.auth.application.auditlog.query.ListAuditLogsQuery;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.presentation.shared.ApiPaths;
import com.template.auth.presentation.shared.security.RequireRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Coarsely gated by role (the simpler mechanism) rather than a permission string. */
@RestController
@RequestMapping(ApiPaths.V1 + "/audit-logs")
@RequireRole("admin")
@Tag(name = "Audit logs")
public class AuditLogController {

    private final ListAuditLogsHandler listAuditLogs;

    public AuditLogController(ListAuditLogsHandler listAuditLogs) {
        this.listAuditLogs = listAuditLogs;
    }

    @GetMapping
    public PaginatedResult<AuditLogDto> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return listAuditLogs.execute(new ListAuditLogsQuery(PaginationParams.of(page, limit)));
    }
}
