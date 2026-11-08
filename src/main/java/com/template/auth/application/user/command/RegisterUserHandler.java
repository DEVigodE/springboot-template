package com.template.auth.application.user.command;

import com.template.auth.application.emailverification.EmailVerificationTokenRepository;
import com.template.auth.application.shared.EmailQueuePort;
import com.template.auth.application.shared.EmailQueuePort.EmailJob;
import com.template.auth.application.shared.PasswordBreachCheckerPort;
import com.template.auth.application.shared.TokenHasherPort;
import com.template.auth.application.user.PasswordHasherPort;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.emailverification.EmailVerificationTokenEntity;
import com.template.auth.domain.shared.exception.EmailAlreadyRegisteredException;
import com.template.auth.domain.shared.exception.InvalidPasswordException;
import com.template.auth.domain.user.Email;
import com.template.auth.domain.user.Password;
import com.template.auth.domain.user.UserEntity;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class RegisterUserHandler {

    public static final Duration EMAIL_VERIFICATION_TOKEN_TTL = Duration.ofHours(48);
    static final String BREACHED_PASSWORD_MESSAGE =
            "This password has appeared in a known data breach - choose a different one";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final EmailQueuePort emailQueue;
    private final TokenHasherPort tokenHasher;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordBreachCheckerPort passwordBreachChecker;
    private final TransactionOperations transaction;

    public RegisterUserHandler(UserRepository userRepository, PasswordHasherPort passwordHasher,
            EmailQueuePort emailQueue, TokenHasherPort tokenHasher,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordBreachCheckerPort passwordBreachChecker, TransactionOperations transaction) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.emailQueue = emailQueue;
        this.tokenHasher = tokenHasher;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordBreachChecker = passwordBreachChecker;
        this.transaction = transaction;
    }

    public record Result(UUID id) {
    }

    public Result execute(RegisterUserCommand command) {
        Email email = Email.of(command.email());

        if (userRepository.emailExists(email.value(), null)) {
            throw new EmailAlreadyRegisteredException(email.value());
        }

        Password.validate(command.password());
        if (passwordBreachChecker.isBreached(command.password())) {
            throw new InvalidPasswordException(BREACHED_PASSWORD_MESSAGE);
        }
        String passwordHash = passwordHasher.hash(command.password());

        Instant now = Instant.now();
        UserEntity user = UserEntity.create(UUID.randomUUID(), command.name(), email.value(), passwordHash, now,
                null);

        String rawVerificationToken = randomToken();

        UserEntity saved = transaction.execute(status -> {
            UserEntity savedUser = userRepository.save(user);

            EmailVerificationTokenEntity verificationToken = EmailVerificationTokenEntity.create(
                    UUID.randomUUID(), savedUser.getId(), tokenHasher.hash(rawVerificationToken),
                    now.plus(EMAIL_VERIFICATION_TOKEN_TTL), now);
            emailVerificationTokenRepository.create(verificationToken);

            return savedUser;
        });

        // Queued only after the transaction above commits - a rollback (e.g. the
        // token insert failing) must not leave a "verify your email" job
        // enqueued for a user that was never actually persisted.
        emailQueue.enqueue(new EmailJob(saved.getEmail(), "Seu cadastro foi realizado com sucesso!",
                "Olá " + saved.getName() + ", seu cadastro foi realizado com sucesso! "
                        + "Use este token para verificar seu e-mail (válido por 48 horas): "
                        + rawVerificationToken));

        return new Result(saved.getId());
    }

    static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
