package com.jaeychoi.dailyus.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.AccessTokenDetails;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private RequestMappingHandlerMapping requestMappingHandlerMapping;

  @Mock
  private HandlerExceptionResolver handlerExceptionResolver;

  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    jwtAuthenticationFilter = new JwtAuthenticationFilter(
        jwtTokenProvider,
        refreshTokenRepository,
        requestMappingHandlerMapping,
        handlerExceptionResolver
    );
  }

  @Test
  void doFilterReturnsUnauthorizedWhenAccessTokenIsBlacklisted() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.addHeader("Authorization", "Bearer access-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    HandlerMethod handlerMethod = new HandlerMethod(new SecuredTestController(), "secured");
    AccessTokenDetails accessTokenDetails = new AccessTokenDetails(
        new CurrentUser(1L, "user@example.com", "user"),
        "access-token-id",
        Instant.parse("2026-06-18T00:00:00Z")
    );

    when(requestMappingHandlerMapping.getHandler(request))
        .thenReturn(new HandlerExecutionChain(handlerMethod));
    when(jwtTokenProvider.parseAccessTokenDetails("access-token")).thenReturn(accessTokenDetails);
    when(refreshTokenRepository.isAccessTokenBlacklisted("access-token-id")).thenReturn(true);
    when(handlerExceptionResolver.resolveException(
        eq(request),
        eq(response),
        eq(null),
        any(BaseException.class)
    )).thenAnswer(invocation -> {
      response.setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
      return new ModelAndView();
    });

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(ErrorCode.INVALID_TOKEN.getStatus().value());
    verify(filterChain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    verify(handlerExceptionResolver).resolveException(
        eq(request),
        eq(response),
        eq(null),
        any(BaseException.class)
    );
  }

  static class SecuredTestController {

    @AuthRequired
    public void secured() {
    }
  }
}
