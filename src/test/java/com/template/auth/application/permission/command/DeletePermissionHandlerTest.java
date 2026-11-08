package com.template.auth.application.permission.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.template.auth.application.auditlog.AuditLogRecorder;
import com.template.auth.application.auditlog.AuditLogRecorder.RecordAuditLogInput;
import com.template.auth.application.permission.PermissionRepository;
import com.template.auth.domain.permission.PermissionEntity;
import com.template.auth.domain.shared.exception.PermissionNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletePermissionHandlerTest {

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AuditLogRecorder auditLogRecorder;

    @InjectMocks
    private DeletePermissionHandler handler;

    @Test
    void softDeletesAndRecordsAnAuditEntry() {
        UUID actor = UUID.randomUUID();
        PermissionEntity permission = PermissionEntity.create(UUID.randomUUID(), "users:read", null, Instant.now());
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));

        handler.execute(new DeletePermissionCommand(permission.getId(), actor));

        verify(permissionRepository).softDelete(permission.getId());
        ArgumentCaptor<RecordAuditLogInput> audit = ArgumentCaptor.forClass(RecordAuditLogInput.class);
        verify(auditLogRecorder).recordAsync(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("permission.deleted");
        assertThat(audit.getValue().actorId()).isEqualTo(actor);
        assertThat(audit.getValue().resourceId()).isEqualTo(permission.getId().toString());
    }

    @Test
    void failsForUnknownPermission() {
        UUID id = UUID.randomUUID();
        when(permissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(new DeletePermissionCommand(id, null)))
                .isInstanceOf(PermissionNotFoundException.class);

        verify(permissionRepository, never()).softDelete(any());
        verify(auditLogRecorder, never()).recordAsync(any());
    }
}
