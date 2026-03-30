package com.jaeychoi.dailyus.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostCreateRequest(
    @NotEmpty
    @Size(max = 10)
    List<@NotBlank @Size(max = 500) String> imageUrls,
    @Size(max = 2200) String content
) {

}
