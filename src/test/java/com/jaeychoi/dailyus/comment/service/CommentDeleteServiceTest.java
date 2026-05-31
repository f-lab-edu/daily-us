package com.jaeychoi.dailyus.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentDeleteServiceTest {

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentDeleteService commentDeleteService;

  @Test
  void deleteDeletesParentCommentWithReplies() {
    Comment comment = Comment.builder()
        .commentId(10L)
        .userId(1L)
        .parentId(null)
        .build();
    when(commentMapper.findActiveById(10L)).thenReturn(comment);

    commentDeleteService.delete(1L, 10L);

    verify(commentMapper).deleteCommentLikes(10L, true);
    verify(commentMapper).delete(10L, true);
  }

  @Test
  void deleteDeletesReplyWithoutIncludingSiblingReplies() {
    Comment comment = Comment.builder()
        .commentId(11L)
        .userId(1L)
        .parentId(10L)
        .build();
    when(commentMapper.findActiveById(11L)).thenReturn(comment);

    commentDeleteService.delete(1L, 11L);

    verify(commentMapper).deleteCommentLikes(11L, false);
    verify(commentMapper).delete(11L, false);
  }

  @Test
  void deleteThrowsWhenCommentDoesNotExist() {
    when(commentMapper.findActiveById(99L)).thenReturn(null);

    assertThatThrownBy(() -> commentDeleteService.delete(1L, 99L))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND));

    verify(commentMapper, never()).deleteCommentLikes(99L, true);
    verify(commentMapper, never()).delete(99L, true);
  }

  @Test
  void deleteThrowsWhenUserDoesNotOwnComment() {
    Comment comment = Comment.builder()
        .commentId(10L)
        .userId(2L)
        .parentId(null)
        .build();
    when(commentMapper.findActiveById(10L)).thenReturn(comment);

    assertThatThrownBy(() -> commentDeleteService.delete(1L, 10L))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

    verify(commentMapper, never()).deleteCommentLikes(10L, true);
    verify(commentMapper, never()).delete(10L, true);
  }
}
