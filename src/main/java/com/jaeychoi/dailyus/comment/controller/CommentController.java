package com.jaeychoi.dailyus.comment.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.service.CommentDeleteService;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentDeleteService commentDeleteService;

  @DeleteMapping("/{commentId}")
  @AuthRequired
  public ApiResponse<Void> delete(
      @AuthenticatedUser CurrentUser user,
      @PathVariable Long commentId
  ) {
    commentDeleteService.delete(user.userId(), commentId);
    return ApiResponse.success(null);
  }
}
