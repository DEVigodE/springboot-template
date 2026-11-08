package com.template.auth.application.passwordreset.command;

public record ResetPasswordCommand(String token, String newPassword) {
}
