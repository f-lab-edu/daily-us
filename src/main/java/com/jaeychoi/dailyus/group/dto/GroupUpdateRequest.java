package com.jaeychoi.dailyus.group.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupUpdateRequest(
    @Size(max = 100)
    @Pattern(regexp = "^(?=.*\\S).+$", message = "Group name must not be blank.")
    String name,

    @Size(max = 500)
    String intro,

    @Size(max = 500)
    String groupImage
) {

}
