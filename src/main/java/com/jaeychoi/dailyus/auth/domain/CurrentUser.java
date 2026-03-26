package com.jaeychoi.dailyus.auth.domain;

public record CurrentUser(Long userId, String email, String nickname) {
}
