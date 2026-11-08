package com.template.auth.application.user.query;

import com.template.auth.application.shared.CachePort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserDto;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.UserNotFoundException;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;

/** Cache-aside: Redis first, DB on miss, every user write invalidates the key. */
@Service
public class GetUserHandler {

    private final UserRepository userRepository;
    private final CachePort cache;

    public GetUserHandler(UserRepository userRepository, CachePort cache) {
        this.userRepository = userRepository;
        this.cache = cache;
    }

    public UserDto execute(GetUserQuery query) {
        String cacheKey = UserCacheKeys.user(query.userId());

        return cache.get(cacheKey, UserDto.class).orElseGet(() -> {
            UserEntity user = userRepository.findById(query.userId())
                    .orElseThrow(() -> new UserNotFoundException(query.userId()));
            UserDto dto = UserDto.from(user);
            cache.set(cacheKey, dto, UserCacheKeys.USER_CACHE_TTL);
            return dto;
        });
    }
}
