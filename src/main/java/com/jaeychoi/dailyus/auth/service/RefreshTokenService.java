package com.jaeychoi.dailyus.auth.service;

import com.jaeychoi.dailyus.auth.dto.RefreshTokenRequest;
import com.jaeychoi.dailyus.auth.dto.TokenResponse;
import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.JwtTokenPair;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.common.jwt.RefreshTokenDetails;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final UserMapper userMapper;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  public TokenResponse refresh(RefreshTokenRequest request) {
    RefreshTokenDetails currentRefreshToken =
        jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken());
    if (refreshTokenRepository.isBlacklisted(currentRefreshToken.tokenId())) {
      throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user = userMapper.findActiveById(currentRefreshToken.user().userId());

    if (user == null) {
      throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    JwtTokenPair tokenPair = jwtTokenProvider.createTokenPair(user);
    RefreshTokenDetails newRefreshToken =
        jwtTokenProvider.parseRefreshTokenDetails(tokenPair.refreshToken());
    if (!refreshTokenRepository.rotate(user.getUserId(), currentRefreshToken, newRefreshToken)) {
      throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    return new TokenResponse(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenExpiresIn(),
        tokenPair.refreshTokenExpiresIn()
    );
  }
}
