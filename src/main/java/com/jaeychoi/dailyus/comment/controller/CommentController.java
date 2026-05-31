package com.jaeychoi.dailyus.comment.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateResponse;
import com.jaeychoi.dailyus.comment.service.CommentUpdateService;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentUpdateService commentUpdateService;

  @PatchMapping("/{commentId}")
  @AuthRequired
  public ApiResponse<CommentUpdateResponse> updateComment(
      @AuthenticatedUser CurrentUser user,
      @PathVariable Long commentId,
      @Valid @RequestBody CommentUpdateRequest request
  ) {
    return ApiResponse.success(commentUpdateService.update(user.userId(), commentId, request));
  }
}
