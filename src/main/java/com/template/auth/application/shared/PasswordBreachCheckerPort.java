package com.template.auth.application.shared;

public interface PasswordBreachCheckerPort {

    boolean isBreached(String password);
}
