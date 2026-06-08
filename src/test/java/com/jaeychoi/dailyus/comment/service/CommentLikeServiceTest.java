package com.jaeychoi.dailyus.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentLikeResponse;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentLikeService commentLikeService;

  @Test
  void likeCreatesCommentLikeAndIncrementsCount() {
    when(commentMapper.findActiveById(10L)).thenReturn(Comment.builder().commentId(10L).build());
    when(commentMapper.findLikeCountByCommentId(10L)).thenReturn(1L);

    CommentLikeResponse response = commentLikeService.like(1L, 10L);

    verify(commentMapper).insertLike(10L, 1L);
    verify(commentMapper).applyLikeCountDelta(10L, 1L);
    assertThat(response).isEqualTo(new CommentLikeResponse(10L, true, 1L));
  }

  @Test
  void likeThrowsWhenCommentDoesNotExist() {
    when(commentMapper.findActiveById(10L)).thenReturn(null);

    assertThatThrownBy(() -> commentLikeService.like(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

    verify(commentMapper, never()).insertLike(10L, 1L);
  }

  @Test
  void likeThrowsWhenLikeAlreadyExists() {
    when(commentMapper.findActiveById(10L)).thenReturn(Comment.builder().commentId(10L).build());
    org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate"))
        .when(commentMapper).insertLike(10L, 1L);

    assertThatThrownBy(() -> commentLikeService.like(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_LIKE_ALREADY_EXISTS);

    verify(commentMapper, never()).applyLikeCountDelta(10L, 1L);
  }

  @Test
  void unlikeDeletesCommentLikeAndDecrementsCount() {
    when(commentMapper.findActiveById(10L)).thenReturn(Comment.builder().commentId(10L).build());
    when(commentMapper.deleteLike(10L, 1L)).thenReturn(1);
    when(commentMapper.findLikeCountByCommentId(10L)).thenReturn(0L);

    CommentLikeResponse response = commentLikeService.unlike(1L, 10L);

    verify(commentMapper).deleteLike(10L, 1L);
    verify(commentMapper).applyLikeCountDelta(10L, -1L);
    assertThat(response).isEqualTo(new CommentLikeResponse(10L, false, 0L));
  }

  @Test
  void unlikeThrowsWhenLikeDoesNotExist() {
    when(commentMapper.findActiveById(10L)).thenReturn(Comment.builder().commentId(10L).build());
    when(commentMapper.deleteLike(10L, 1L)).thenReturn(0);

    assertThatThrownBy(() -> commentLikeService.unlike(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_LIKE_NOT_FOUND);

    verify(commentMapper, never()).applyLikeCountDelta(10L, -1L);
  }
}
