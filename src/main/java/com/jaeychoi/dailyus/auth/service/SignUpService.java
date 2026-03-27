package com.jaeychoi.dailyus.auth.service;

import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpService {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public SignUpResponse signUp(SignUpRequest request) {
    if (userMapper.existsActiveByEmail(request.email())) {
      throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    if (userMapper.existsActiveByNickname(request.nickname())) {
      throw new BaseException(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    User user = User.builder()
        .email(request.email())
        .password(passwordEncoder.encode(request.password()))
        .nickname(request.nickname())
        .build();

    userMapper.insert(user);

    return new SignUpResponse(user.getUserId(), user.getEmail(), user.getNickname());
  }
}
