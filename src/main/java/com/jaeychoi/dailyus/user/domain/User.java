package com.jaeychoi.dailyus.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long userId;
    private String email;
    private String password;
    private String nickname;
    @Builder.Default
    private Long followerCount = 0L;
    @Builder.Default
    private Long followeeCount = 0L;
    @Builder.Default
    private String profileImage = null;
    @Builder.Default
    private LocalDateTime deletedAt = null;
}
