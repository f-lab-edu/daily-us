package com.jaeychoi.dailyus.auth.service;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.auth.dto.LogoutRequest;
import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.common.jwt.RefreshTokenDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  public void logout(CurrentUser currentUser, LogoutRequest request) {
    RefreshTokenDetails refreshTokenDetails =
        jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken());

    if (!currentUser.userId().equals(refreshTokenDetails.user().userId())) {
      throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    if (!refreshTokenRepository.revoke(currentUser.userId(), refreshTokenDetails)) {
      throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
  }
}
