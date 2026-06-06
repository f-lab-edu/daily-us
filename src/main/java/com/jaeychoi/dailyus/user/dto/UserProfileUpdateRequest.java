package com.jaeychoi.dailyus.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @Size(max = 100)
    @Pattern(regexp = "^(?=.*\\S).+$", message = "Nickname must not be blank.")
    String nickname,

    @Size(max = 500)
    String intro,

    @Size(max = 500)
    @Pattern(regexp = "^$|^https?://.+$", message = "Profile image must be an HTTP or HTTPS URL.")
    String profileImage
) {

}
