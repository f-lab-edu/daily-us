package com.jaeychoi.dailyus.auth.dto;

import com.jaeychoi.dailyus.common.fixture.UserFixture;
import com.jaeychoi.dailyus.common.support.ValidationTestSupport;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignUpRequestValidationTest extends ValidationTestSupport {

  @Test
  void signUpRequestIsValidAtMinimumPasswordLengthBoundary() {
    SignUpRequest request = UserFixture.user()
        .withPassword("Abcdef1!x")
        .toSignUpRequest();

    assertValid(request);
  }

  @Test
  void signUpRequestIsValidAtMaximumLengthsBoundary() {
    String maxLengthValidEmail = UserFixture.repeat('a', 64)
        + "@"
        + UserFixture.repeat('b', 63)
        + "."
        + UserFixture.repeat('c', 63)
        + "."
        + UserFixture.repeat('d', 62);

    SignUpRequest request = UserFixture.user()
        .withEmail(maxLengthValidEmail)
        .withPassword("A" + UserFixture.repeat('b', 252) + "1!")
        .withNickname(UserFixture.repeat('n', 100))
        .toSignUpRequest();

    assertValid(request);
  }

  @Test
  void signUpRequestIsInvalidWhenEmailIsBlank() {
    SignUpRequest request = UserFixture.user()
        .withEmail("")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("email");
  }

  @Test
  void signUpRequestIsInvalidWhenEmailFormatIsWrong() {
    SignUpRequest request = UserFixture.user()
        .withEmail("invalid-email")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("email");
  }

  @Test
  void signUpRequestIsInvalidWhenEmailExceedsMaximumLength() {
    String tooLongEmail = UserFixture.repeat('a', 244) + "@x.com";
    SignUpRequest request = UserFixture.user()
        .withEmail(tooLongEmail)
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("email");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordIsBlank() {
    SignUpRequest request = UserFixture.user()
        .withPassword("")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordIsShorterThanMinimumLength() {
    SignUpRequest request = UserFixture.user()
        .withPassword("Abcde1!x")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordExceedsMaximumLength() {
    SignUpRequest request = UserFixture.user()
        .withPassword("A" + UserFixture.repeat('b', 253) + "1!")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordDoesNotContainUppercase() {
    SignUpRequest request = UserFixture.user()
        .withPassword("abcdefg1!")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordDoesNotContainLowercase() {
    SignUpRequest request = UserFixture.user()
        .withPassword("ABCDEFG1!")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordDoesNotContainNumber() {
    SignUpRequest request = UserFixture.user()
        .withPassword("Abcdefgh!")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenPasswordDoesNotContainSpecialCharacter() {
    SignUpRequest request = UserFixture.user()
        .withPassword("Abcdefgh1")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("password");
  }

  @Test
  void signUpRequestIsInvalidWhenNicknameIsBlank() {
    SignUpRequest request = UserFixture.user()
        .withNickname("")
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("nickname");
  }

  @Test
  void signUpRequestIsInvalidWhenNicknameExceedsMaximumLength() {
    SignUpRequest request = UserFixture.user()
        .withNickname(UserFixture.repeat('n', 101))
        .toSignUpRequest();

    Set<String> propertyNames = validatePropertyNames(request);

    assertThat(propertyNames).contains("nickname");
  }
}
