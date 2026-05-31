package com.jaeychoi.dailyus.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateResponse;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentUpdateServiceTest {

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentUpdateService commentUpdateService;

  @Test
  void updateCommentUpdatesContentWhenAuthorMatches() {
    Comment existingComment = Comment.builder()
        .commentId(10L)
        .userId(1L)
        .content("old")
        .createdAt(LocalDateTime.of(2026, 5, 18, 11, 0))
        .updatedAt(LocalDateTime.of(2026, 5, 18, 11, 0))
        .build();
    Comment updatedComment = Comment.builder()
        .commentId(10L)
        .userId(1L)
        .content("new")
        .createdAt(LocalDateTime.of(2026, 5, 18, 11, 0))
        .updatedAt(LocalDateTime.of(2026, 5, 18, 12, 0))
        .build();
    when(commentMapper.findActiveCommentById(10L))
        .thenReturn(existingComment)
        .thenReturn(updatedComment);

    CommentUpdateResponse response =
        commentUpdateService.update(1L, 10L, new CommentUpdateRequest("new"));

    verify(commentMapper).updateContent(10L, "new");
    assertThat(response.commentId()).isEqualTo(10L);
    assertThat(response.content()).isEqualTo("new");
    assertThat(response.edited()).isTrue();
    assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 18, 12, 0));
  }

  @Test
  void updateCommentThrowsWhenCommentDoesNotExist() {
    when(commentMapper.findActiveCommentById(10L)).thenReturn(null);

    assertThatThrownBy(() -> commentUpdateService.update(1L, 10L, new CommentUpdateRequest("new")))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

    verify(commentMapper, never()).updateContent(10L, "new");
  }

  @Test
  void updateCommentThrowsWhenUserIsNotAuthor() {
    when(commentMapper.findActiveCommentById(10L))
        .thenReturn(Comment.builder().commentId(10L).userId(2L).content("old").build());

    assertThatThrownBy(() -> commentUpdateService.update(1L, 10L, new CommentUpdateRequest("new")))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);

    verify(commentMapper, never()).updateContent(10L, "new");
  }
}
