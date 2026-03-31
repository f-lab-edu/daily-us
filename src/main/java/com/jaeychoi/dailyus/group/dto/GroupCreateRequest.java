package com.jaeychoi.dailyus.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupCreateRequest(
    @NotBlank
    @Size(max = 100)
    String name,

    @Size(max = 500)
    String intro,

    @Size(max = 500)
    String groupImage
) {

}
