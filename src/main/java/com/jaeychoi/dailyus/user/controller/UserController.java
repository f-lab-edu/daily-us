package com.jaeychoi.dailyus.user.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.service.UserFollowService;
import com.jaeychoi.dailyus.user.service.UserPostService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
  private final UserPostService userPostService;

  @GetMapping("/me/posts")
  @AuthRequired
  public ApiResponse<PostFeedResponse> getMyPosts(
      @AuthenticatedUser CurrentUser user,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long postId,
      @RequestParam(required = false, defaultValue = "10") Long size) {
    return ApiResponse.success(userPostService.getMyPosts(user.userId(), createdAt, postId, size));
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
