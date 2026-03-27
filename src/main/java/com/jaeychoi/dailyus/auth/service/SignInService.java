package com.jaeychoi.dailyus.auth.service;

import com.jaeychoi.dailyus.auth.dto.SignInRequest;
import com.jaeychoi.dailyus.auth.dto.TokenResponse;
import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.JwtTokenPair;
import com.jaeychoi.dailyus.common.jwt.RefreshTokenDetails;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignInService {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  public TokenResponse signIn(SignInRequest request) {
    User user = userMapper.findActiveByEmail(request.email());
    if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new BaseException(ErrorCode.INVALID_CREDENTIALS);
    }

    JwtTokenPair tokenPair = jwtTokenProvider.createTokenPair(user);
    RefreshTokenDetails refreshTokenDetails =
        jwtTokenProvider.parseRefreshTokenDetails(tokenPair.refreshToken());
    refreshTokenRepository.save(user.getUserId(), refreshTokenDetails);

    return new TokenResponse(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenExpiresIn(),
        tokenPair.refreshTokenExpiresIn()
    );
  }
}
