package com.jaeychoi.dailyus.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank
    @Email
    @Size(max = 255)
    String email,

    @NotBlank
    @Size(min = 9, max = 255)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
        message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String password,

    @NotBlank
    @Size(max = 100)
    String nickname
) {

}
