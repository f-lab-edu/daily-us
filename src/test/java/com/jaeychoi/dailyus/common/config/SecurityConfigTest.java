package com.jaeychoi.dailyus.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderHashesAndMatchesPassword() {
        String rawPassword = "Password1!";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword).isNotBlank();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", encodedPassword)).isFalse();
    }

    @Test
    void passwordEncoderProducesDifferentHashesForSamePassword() {
        String rawPassword = "Password1!";

        String firstEncoded = passwordEncoder.encode(rawPassword);
        String secondEncoded = passwordEncoder.encode(rawPassword);

        assertThat(firstEncoded).isNotEqualTo(secondEncoded);
        assertThat(passwordEncoder.matches(rawPassword, firstEncoded)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, secondEncoded)).isTrue();
    }
}
