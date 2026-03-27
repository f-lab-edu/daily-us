package com.jaeychoi.dailyus.auth.dto;

public record SignUpResponse(
    Long userId,
    String email,
    String nickname
) {

}
