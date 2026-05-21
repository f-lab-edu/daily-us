package com.jaeychoi.dailyus.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentCreateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentCreateResponse;
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
class CommentCreateServiceTest {

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentCreateService commentCreateService;

  @Test
  void createCommentInsertsTopLevelComment() {
    when(commentMapper.existsActivePostById(10L)).thenReturn(true);
    doAnswer(invocation -> {
      Comment comment = invocation.getArgument(0);
      comment.setCommentId(101L);
      return null;
    }).when(commentMapper).insert(any(Comment.class));
    when(commentMapper.findActiveCommentById(101L)).thenReturn(Comment.builder()
        .commentId(101L)
        .postId(10L)
        .userId(1L)
        .content("comment")
        .parentId(null)
        .likeCount(0L)
        .createdAt(LocalDateTime.of(2026, 4, 6, 10, 0))
        .build());

    CommentCreateResponse response =
        commentCreateService.create(10L, 1L, new CommentCreateRequest("comment", null));

    verify(commentMapper).insert(any(Comment.class));
    assertThat(response.commentId()).isEqualTo(101L);
    assertThat(response.parentId()).isNull();
  }

  @Test
  void createReplyInsertsReplyWhenParentIsValid() {
    when(commentMapper.existsActivePostById(10L)).thenReturn(true);
    when(commentMapper.findActiveCommentById(100L)).thenReturn(Comment.builder()
        .commentId(100L)
        .postId(10L)
        .userId(2L)
        .parentId(null)
        .build());
    doAnswer(invocation -> {
      Comment comment = invocation.getArgument(0);
      comment.setCommentId(201L);
      return null;
    }).when(commentMapper).insert(any(Comment.class));
    when(commentMapper.findActiveCommentById(201L)).thenReturn(Comment.builder()
        .commentId(201L)
        .postId(10L)
        .userId(1L)
        .content("reply")
        .parentId(100L)
        .likeCount(0L)
        .createdAt(LocalDateTime.of(2026, 4, 6, 10, 30))
        .build());

    CommentCreateResponse response =
        commentCreateService.create(10L, 1L, new CommentCreateRequest("reply", 100L));

    assertThat(response.commentId()).isEqualTo(201L);
    assertThat(response.parentId()).isEqualTo(100L);
  }

  @Test
  void createThrowsWhenPostDoesNotExist() {
    when(commentMapper.existsActivePostById(10L)).thenReturn(false);

    assertThatThrownBy(() ->
        commentCreateService.create(10L, 1L, new CommentCreateRequest("comment", null)))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_NOT_FOUND);

    verify(commentMapper, never()).insert(any(Comment.class));
  }

  @Test
  void createReplyThrowsWhenParentCommentDoesNotExist() {
    when(commentMapper.existsActivePostById(10L)).thenReturn(true);
    when(commentMapper.findActiveCommentById(100L)).thenReturn(null);

    assertThatThrownBy(() ->
        commentCreateService.create(10L, 1L, new CommentCreateRequest("reply", 100L)))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

    verify(commentMapper, never()).insert(any(Comment.class));
  }

  @Test
  void createReplyThrowsWhenParentBelongsToAnotherPost() {
    when(commentMapper.existsActivePostById(10L)).thenReturn(true);
    when(commentMapper.findActiveCommentById(100L)).thenReturn(Comment.builder()
        .commentId(100L)
        .postId(20L)
        .parentId(null)
        .build());

    assertThatThrownBy(() ->
        commentCreateService.create(10L, 1L, new CommentCreateRequest("reply", 100L)))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_PARENT_MISMATCH);

    verify(commentMapper, never()).insert(any(Comment.class));
  }

  @Test
  void createReplyThrowsWhenParentIsAlreadyAReply() {
    when(commentMapper.existsActivePostById(10L)).thenReturn(true);
    when(commentMapper.findActiveCommentById(100L)).thenReturn(Comment.builder()
        .commentId(100L)
        .postId(10L)
        .parentId(50L)
        .build());

    assertThatThrownBy(() ->
        commentCreateService.create(10L, 1L, new CommentCreateRequest("reply", 100L)))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED);

    verify(commentMapper, never()).insert(any(Comment.class));
  }
}
