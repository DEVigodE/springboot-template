package com.template.auth.application.user;

public interface PasswordHasherPort {

    String hash(String plainText);

    boolean matches(String plainText, String hash);
}
