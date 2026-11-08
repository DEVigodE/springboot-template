package com.template.auth.application.passwordreset.command;

import com.template.auth.application.passwordreset.PasswordResetTokenRepository;
import com.template.auth.application.shared.EmailQueuePort;
import com.template.auth.application.shared.EmailQueuePort.EmailJob;
import com.template.auth.application.shared.TokenHasherPort;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.passwordreset.PasswordResetTokenEntity;
import com.template.auth.domain.user.UserEntity;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RequestPasswordResetHandler {

    public static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofHours(1);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TokenHasherPort tokenHasher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailQueuePort emailQueue;

    public RequestPasswordResetHandler(UserRepository userRepository, TokenHasherPort tokenHasher,
            PasswordResetTokenRepository passwordResetTokenRepository, EmailQueuePort emailQueue) {
        this.userRepository = userRepository;
        this.tokenHasher = tokenHasher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailQueue = emailQueue;
    }

    public void execute(RequestPasswordResetCommand command) {
        UserEntity user = userRepository.findByEmail(command.email()).orElse(null);

        // Silently no-op when the email doesn't match anyone: the controller
        // always returns 200 either way, so a real vs. fake email must behave
        // identically here too, or timing/behavior would leak which is which.
        if (user == null) {
            return;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);
        Instant now = Instant.now();

        passwordResetTokenRepository.create(PasswordResetTokenEntity.create(UUID.randomUUID(), user.getId(),
                tokenHasher.hash(rawToken), now.plus(PASSWORD_RESET_TOKEN_TTL), now));

        emailQueue.enqueue(new EmailJob(user.getEmail(), "Redefinição de senha solicitada",
                "Use este token para redefinir sua senha (válido por 1 hora): " + rawToken));
    }
}
