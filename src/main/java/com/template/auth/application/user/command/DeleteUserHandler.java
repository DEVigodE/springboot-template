package com.template.auth.application.user.command;

import com.template.auth.application.auditlog.AuditLogRecorder;
import com.template.auth.application.auditlog.AuditLogRecorder.RecordAuditLogInput;
import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.UserNotFoundException;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class DeleteUserHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CachePort cache;
    private final TransactionOperations transaction;
    private final AuditLogRecorder auditLogRecorder;

    public DeleteUserHandler(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            CachePort cache, TransactionOperations transaction, AuditLogRecorder auditLogRecorder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cache = cache;
        this.transaction = transaction;
        this.auditLogRecorder = auditLogRecorder;
    }

    public void execute(DeleteUserCommand command) {
        UserEntity user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        transaction.executeWithoutResult(status -> {
            userRepository.softDelete(user.getId(), command.actorId());
            // A deleted account - self-service or admin-initiated - must not leave
            // any existing session usable for as long as its refresh token would
            // otherwise stay valid.
            refreshTokenRepository.revokeAllForUser(user.getId());
        });

        cache.delete(UserCacheKeys.user(user.getId()));

        auditLogRecorder.recordAsync(new RecordAuditLogInput(command.actorId(), "user.deleted", "User",
                user.getId().toString(), null));
    }
}
