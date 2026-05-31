package com.jaeychoi.dailyus.comment.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateResponse;
import com.jaeychoi.dailyus.comment.service.CommentDeleteService;
import com.jaeychoi.dailyus.comment.service.CommentGetService;
import com.jaeychoi.dailyus.comment.service.CommentUpdateService;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentDeleteService commentDeleteService;
  private final CommentUpdateService commentUpdateService;
  private final CommentGetService commentGetService;

  @DeleteMapping("/{commentId}")
  @AuthRequired
  public ApiResponse<Void> delete(
      @AuthenticatedUser CurrentUser user,
      @PathVariable Long commentId
  ) {
    commentDeleteService.delete(user.userId(), commentId);
    return ApiResponse.success(null);
  }

  @PatchMapping("/{commentId}")
  @AuthRequired
  public ApiResponse<CommentUpdateResponse> updateComment(
      @AuthenticatedUser CurrentUser user,
      @PathVariable Long commentId,
      @Valid @RequestBody CommentUpdateRequest request
  ) {
    return ApiResponse.success(commentUpdateService.update(user.userId(), commentId, request));
  }

  @GetMapping("/{commentId}/replies")
  @AuthRequired
  public ApiResponse<CommentResponse> getReplies(
      @PathVariable Long commentId,
      @AuthenticatedUser CurrentUser user,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long replyId,
      @RequestParam(required = false, defaultValue = "3") Long size
  ) {
    return ApiResponse.success(
        commentGetService.getReplies(commentId, user.userId(), createdAt, replyId, size)
    );
  }
}
