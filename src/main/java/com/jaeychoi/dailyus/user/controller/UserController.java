package com.jaeychoi.dailyus.user.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.user.dto.UserActivityResponse;
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.dto.UserGroupResponse;
import com.jaeychoi.dailyus.user.dto.UserMyProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileUpdateRequest;
import com.jaeychoi.dailyus.user.service.UserActivityService;
import com.jaeychoi.dailyus.user.service.UserFollowService;
import com.jaeychoi.dailyus.user.service.UserMyGroupService;
import com.jaeychoi.dailyus.user.service.UserPostService;
import com.jaeychoi.dailyus.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserFollowService userFollowService;
  private final UserProfileService userProfileService;
  private final UserActivityService userActivityService;
  private final UserMyGroupService userMyGroupService;
  private final UserPostService userPostService;

  @GetMapping("/me")
  @AuthRequired
  public ApiResponse<UserMyProfileResponse> getMyProfile(@AuthenticatedUser CurrentUser user) {
    return ApiResponse.success(userProfileService.getMyProfile(user.userId()));
  }

  @GetMapping("/{userId}")
  @AuthRequired
  public ApiResponse<UserProfileResponse> getUserProfile(
      @AuthenticatedUser CurrentUser user,
      @PathVariable("userId") Long targetUserId) {
    return ApiResponse.success(userProfileService.getProfile(user.userId(), targetUserId));
  }

  @GetMapping("/me/activities")
  @AuthRequired
  public ApiResponse<UserActivityResponse> getMyActivities(
      @AuthenticatedUser CurrentUser user,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month) {
    UserActivityResponse response = userActivityService.getMyActivities(user.userId(), year, month);
    return ApiResponse.success(response);
  }

  @GetMapping("/me/groups")
  @AuthRequired
  public ApiResponse<UserGroupResponse> getMyGroups(@AuthenticatedUser CurrentUser user) {
    UserGroupResponse response = userMyGroupService.getMyGroups(user.userId());
    return ApiResponse.success(response);
  }

  @PatchMapping("/me")
  @AuthRequired
  public ApiResponse<UserProfileResponse> updateMyProfile(
      @AuthenticatedUser CurrentUser user,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    return ApiResponse.success(userProfileService.updateProfile(user.userId(), request));
  }

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

  @GetMapping("/{userId}/posts")
  @AuthRequired
  public ApiResponse<PostFeedResponse> getUserPosts(
      @AuthenticatedUser CurrentUser user,
      @PathVariable("userId") Long targetUserId,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long postId,
      @RequestParam(required = false, defaultValue = "10") Long size) {
    return ApiResponse.success(userPostService.getPosts(targetUserId, createdAt, postId, size));
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
