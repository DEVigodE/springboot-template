package com.template.auth.application.role.command;

import com.template.auth.application.auditlog.AuditLogRecorder;
import com.template.auth.application.auditlog.AuditLogRecorder.RecordAuditLogInput;
import com.template.auth.application.role.RoleRepository;
import com.template.auth.domain.role.RoleEntity;
import com.template.auth.domain.shared.exception.RoleNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeleteRoleHandler {

    private final RoleRepository roleRepository;
    private final AuditLogRecorder auditLogRecorder;

    public DeleteRoleHandler(RoleRepository roleRepository, AuditLogRecorder auditLogRecorder) {
        this.roleRepository = roleRepository;
        this.auditLogRecorder = auditLogRecorder;
    }

    public void execute(DeleteRoleCommand command) {
        RoleEntity role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId()));

        roleRepository.softDelete(role.getId());

        auditLogRecorder.recordAsync(new RecordAuditLogInput(command.actorId(), "role.deleted", "Role",
                role.getId().toString(), null));
    }
}
