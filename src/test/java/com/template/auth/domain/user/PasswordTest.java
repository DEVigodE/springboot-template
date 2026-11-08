package com.template.auth.domain.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.template.auth.domain.shared.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PasswordTest {

    @Test
    void acceptsAPasswordMeetingEveryRule() {
        assertThatCode(() -> Password.validate("Senha123")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "Ab1, at least 8",
            "senha123, uppercase",
            "SENHA123, lowercase",
            "SenhaSenha, number",
    })
    void rejectsPasswordsBreakingARule(String password, String expectedMessageFragment) {
        assertThatThrownBy(() -> Password.validate(password))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining(expectedMessageFragment);
    }

    @Test
    void rejectsPasswordsOverTheMaximumLength() {
        assertThatThrownBy(() -> Password.validate("Aa1" + "x".repeat(Password.MAX_LENGTH)))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at most");
    }
}
