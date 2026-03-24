package com.jaeychoi.dailyus.common.fixture;

import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.user.domain.User;

public class UserFixture {

  private Long userId = 1L;
  private String email = "user@example.com";
  private String password = "Password1!";
  private String nickname = "dailyus";
  private String encodedPassword = "encoded-password";
  private Long followerCount = 0L;
  private Long followeeCount = 0L;
  private String profileImage = null;

  public static UserFixture user() {
    return new UserFixture();
  }

  public UserFixture withUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public UserFixture withEmail(String email) {
    this.email = email;
    return this;
  }

  public UserFixture withPassword(String password) {
    this.password = password;
    return this;
  }

  public UserFixture withNickname(String nickname) {
    this.nickname = nickname;
    return this;
  }

  public UserFixture withEncodedPassword(String encodedPassword) {
    this.encodedPassword = encodedPassword;
    return this;
  }

  public SignUpRequest toSignUpRequest() {
    return new SignUpRequest(email, password, nickname);
  }

  public SignUpResponse toSignUpResponse() {
    return new SignUpResponse(userId, email, nickname);
  }

  public User toUser() {
    return User.builder()
        .userId(userId)
        .email(email)
        .password(encodedPassword)
        .nickname(nickname)
        .followerCount(followerCount)
        .followeeCount(followeeCount)
        .profileImage(profileImage)
        .build();
  }

  public void assignGeneratedId(User user) {
    user.setUserId(userId);
  }

  public String email() {
    return email;
  }

  public String password() {
    return password;
  }

  public String nickname() {
    return nickname;
  }

  public String encodedPassword() {
    return encodedPassword;
  }

  public static String repeat(char value, int count) {
    return String.valueOf(value).repeat(Math.max(0, count));
  }
}
