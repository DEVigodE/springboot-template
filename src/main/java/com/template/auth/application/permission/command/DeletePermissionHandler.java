package com.template.auth.application.permission.command;

import com.template.auth.application.auditlog.AuditLogRecorder;
import com.template.auth.application.auditlog.AuditLogRecorder.RecordAuditLogInput;
import com.template.auth.application.permission.PermissionRepository;
import com.template.auth.domain.permission.PermissionEntity;
import com.template.auth.domain.shared.exception.PermissionNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeletePermissionHandler {

    private final PermissionRepository permissionRepository;
    private final AuditLogRecorder auditLogRecorder;

    public DeletePermissionHandler(PermissionRepository permissionRepository, AuditLogRecorder auditLogRecorder) {
        this.permissionRepository = permissionRepository;
        this.auditLogRecorder = auditLogRecorder;
    }

    public void execute(DeletePermissionCommand command) {
        PermissionEntity permission = permissionRepository.findById(command.permissionId())
                .orElseThrow(() -> new PermissionNotFoundException(command.permissionId()));

        permissionRepository.softDelete(permission.getId());

        auditLogRecorder.recordAsync(new RecordAuditLogInput(command.actorId(), "permission.deleted",
                "Permission", permission.getId().toString(), null));
    }
}
