package com.jaeychoi.dailyus.common.web;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String API_PREFIX = "/api/";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String AUTHORIZATION_HEADER = "Authorization";

  private final JwtTokenProvider jwtTokenProvider;
  private final RequestMappingHandlerMapping requestMappingHandlerMapping;
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path == null || !path.startsWith(API_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      AuthRequired authRequired = findAuthRequired(request);
      if (authRequired == null) {
        filterChain.doFilter(request, response);
        return;
      }

      String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
      if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
        throw new BaseException(ErrorCode.UNAUTHORIZED);
      }

      String token = authorizationHeader.substring(BEARER_PREFIX.length());
      CurrentUser user = jwtTokenProvider.parseAccessToken(token);

      request.setAttribute(AuthRequestAttributes.CURRENT_USER, user);
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      handlerExceptionResolver.resolveException(request, response, null, e);
    }
  }

  private AuthRequired findAuthRequired(HttpServletRequest request) throws Exception {
    HandlerExecutionChain executionChain = requestMappingHandlerMapping.getHandler(request);
    if (executionChain == null || !(executionChain.getHandler() instanceof HandlerMethod handlerMethod)) {
      return null;
    }

    AuthRequired authRequired = handlerMethod.getMethodAnnotation(AuthRequired.class);
    if (authRequired != null) {
      return authRequired;
    }
    return handlerMethod.getBeanType().getAnnotation(AuthRequired.class);
  }
}
