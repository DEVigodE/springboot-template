package com.template.auth.application.user.command;

public record RegisterUserCommand(String name, String email, String password) {
}
