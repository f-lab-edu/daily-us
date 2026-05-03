package com.jaeychoi.dailyus.user.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.user.dto.UserActivityResponse;
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.service.UserFollowService;
import com.jaeychoi.dailyus.user.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserFollowService userFollowService;
  private final UserActivityService userActivityService;

  @GetMapping("/me/activities")
  @AuthRequired
  public ApiResponse<UserActivityResponse> getMyActivities(
      @AuthenticatedUser CurrentUser user,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month) {
    UserActivityResponse response = userActivityService.getMyActivities(user.userId(), year, month);
    return ApiResponse.success(response);
  }

  @PostMapping("/{userId}/follow")
  @ResponseStatus(HttpStatus.CREATED)
  @AuthRequired
  public ApiResponse<UserFollowResponse> follow(
      @AuthenticatedUser CurrentUser user,
      @PathVariable("userId") Long targetUserId) {
    UserFollowResponse response = userFollowService.follow(user.userId(), targetUserId);
    return ApiResponse.success(response);
  }

  @DeleteMapping("/{userId}/follow")
  @AuthRequired
  public ApiResponse<UserFollowResponse> unfollow(
      @AuthenticatedUser CurrentUser user,
      @PathVariable("userId") Long targetUserId) {
    UserFollowResponse response = userFollowService.unfollow(user.userId(), targetUserId);
    return ApiResponse.success(response);
  }
}
