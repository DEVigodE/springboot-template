package com.template.auth.application.user.command;

import com.template.auth.application.shared.CachePort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserDto;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.EmailAlreadyRegisteredException;
import com.template.auth.domain.shared.exception.UserNotFoundException;
import com.template.auth.domain.user.Email;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserHandler {

    private final UserRepository userRepository;
    private final CachePort cache;

    public UpdateUserHandler(UserRepository userRepository, CachePort cache) {
        this.userRepository = userRepository;
        this.cache = cache;
    }

    public UserDto execute(UpdateUserCommand command) {
        UserEntity user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (command.name() != null) {
            user.rename(command.name());
        }

        if (command.email() != null) {
            Email email = Email.of(command.email());
            if (userRepository.emailExists(email.value(), user.getId())) {
                throw new EmailAlreadyRegisteredException(email.value());
            }
            user.changeEmail(email.value());
        }

        if (command.active() != null) {
            if (command.active()) {
                user.activate();
            } else {
                user.deactivate();
            }
        }

        user.registerUpdate();

        UserEntity saved = userRepository.save(user);
        cache.delete(UserCacheKeys.user(saved.getId()));

        return UserDto.from(saved);
    }
}
