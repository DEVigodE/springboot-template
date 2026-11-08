package com.template.auth.application.user.query;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.user.UserDto;
import com.template.auth.application.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ListUsersHandler {

    private final UserRepository userRepository;

    public ListUsersHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PaginatedResult<UserDto> execute(ListUsersQuery query) {
        return userRepository.findAll(query.pagination()).map(UserDto::from);
    }
}
