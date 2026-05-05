package com.jaeychoi.dailyus.post.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.service.CommentGetService;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.post.dto.PostCreateRequest;
import com.jaeychoi.dailyus.post.dto.PostCreateResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.service.PostCreateService;
import com.jaeychoi.dailyus.post.service.PostFeedService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostCreateService postCreateService;
  private final PostFeedService postFeedService;
  private final CommentGetService commentGetService;

  @GetMapping
  @AuthRequired
  public ApiResponse<PostFeedResponse> getFeed(@AuthenticatedUser CurrentUser user,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long postId,
      @RequestParam(required = false, defaultValue = "10") Long size) {
    return ApiResponse.success(postFeedService.getFeed(user.userId(), createdAt, postId, size));
  }

  @GetMapping("/{postId}/comments")
  @AuthRequired
  public ApiResponse<CommentResponse> getComments(
      @PathVariable Long postId,
      @AuthenticatedUser CurrentUser user,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long commentId,
      @RequestParam(required = false, defaultValue = "10") Long size
  ) {
    return ApiResponse.success(
        commentGetService.getComments(postId, user.userId(), createdAt, commentId, size)
    );
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @AuthRequired
  public ApiResponse<PostCreateResponse> createPost(@AuthenticatedUser CurrentUser user,
      @Valid @RequestBody PostCreateRequest request) {
    return ApiResponse.success(postCreateService.createPost(user.userId(), request));
  }
}
