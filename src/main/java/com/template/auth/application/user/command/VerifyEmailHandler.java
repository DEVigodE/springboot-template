package com.template.auth.application.user.command;

import com.template.auth.application.emailverification.EmailVerificationTokenRepository;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.shared.TokenHasherPort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.emailverification.EmailVerificationTokenEntity;
import com.template.auth.domain.shared.exception.InvalidEmailVerificationTokenException;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class VerifyEmailHandler {

    private final TokenHasherPort tokenHasher;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final CachePort cache;
    private final TransactionOperations transaction;

    public VerifyEmailHandler(TokenHasherPort tokenHasher,
            EmailVerificationTokenRepository emailVerificationTokenRepository, UserRepository userRepository,
            CachePort cache, TransactionOperations transaction) {
        this.tokenHasher = tokenHasher;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userRepository = userRepository;
        this.cache = cache;
        this.transaction = transaction;
    }

    public void execute(VerifyEmailCommand command) {
        String tokenHash = tokenHasher.hash(command.token());
        EmailVerificationTokenEntity existingToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .filter(EmailVerificationTokenEntity::isValid)
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        UserEntity user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        user.verifyEmail();
        user.registerUpdate();

        transaction.executeWithoutResult(status -> {
            userRepository.save(user);
            emailVerificationTokenRepository.markAsUsed(existingToken.getId());
        });

        cache.delete(UserCacheKeys.user(user.getId()));
    }
}
