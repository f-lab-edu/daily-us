package com.jaeychoi.dailyus.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.dto.CommentRow;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentGetServiceTest {

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentGetService commentGetService;

  @Test
  void getCommentsReturnsHierarchicalCommentsWithCursor() {
    Long postId = 10L;
    Long userId = 1L;
    CommentRow firstComment = new CommentRow(
        101L,
        2L,
        "author-1",
        "https://example.com/profile-1.png",
        "first comment",
        3L,
        true,
        LocalDateTime.of(2026, 4, 6, 10, 0),
        null
    );
    CommentRow secondComment = new CommentRow(
        100L,
        3L,
        "author-2",
        "https://example.com/profile-2.png",
        "second comment",
        1L,
        false,
        LocalDateTime.of(2026, 4, 6, 9, 0),
        null
    );
    CommentRow thirdComment = new CommentRow(
        99L,
        4L,
        "author-3",
        null,
        "third comment",
        0L,
        false,
        LocalDateTime.of(2026, 4, 6, 8, 0),
        null
    );
    CommentRow reply = new CommentRow(
        201L,
        5L,
        "reply-author",
        null,
        "reply",
        2L,
        true,
        LocalDateTime.of(2026, 4, 6, 10, 30),
        101L
    );

    when(commentMapper.existsActivePostById(postId)).thenReturn(true);
    when(commentMapper.findComments(postId, userId, 3L, null, null))
        .thenReturn(List.of(firstComment, secondComment, thirdComment));
    when(commentMapper.findRepliesByParentIds(List.of(101L, 100L), userId))
        .thenReturn(List.of(reply));

    CommentResponse response = commentGetService.getComments(postId, userId, null, null, 2L);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).commentId()).isEqualTo(101L);
    assertThat(response.items().get(0).likedByMe()).isTrue();
    assertThat(response.items().get(0).replies()).hasSize(1);
    assertThat(response.items().get(0).replies().get(0).commentId()).isEqualTo(201L);
    assertThat(response.items().get(1).commentId()).isEqualTo(100L);
    assertThat(response.items().get(1).replies()).isEmpty();
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 6, 9, 0));
    assertThat(response.lastCommentId()).isEqualTo(100L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.size()).isEqualTo(2L);
  }

  @Test
  void getCommentsReturnsEmptyItemsWhenNoCommentsExist() {
    Long postId = 10L;
    Long userId = 1L;
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 4, 6, 9, 0);

    when(commentMapper.existsActivePostById(postId)).thenReturn(true);
    when(commentMapper.findComments(postId, userId, 11L, cursorCreatedAt, 50L)).thenReturn(List.of());

    CommentResponse response =
        commentGetService.getComments(postId, userId, cursorCreatedAt, 50L, 10L);

    verify(commentMapper).findComments(postId, userId, 11L, cursorCreatedAt, 50L);
    assertThat(response.items()).isEmpty();
    assertThat(response.lastCreatedAt()).isNull();
    assertThat(response.lastCommentId()).isNull();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.size()).isEqualTo(10L);
  }

  @Test
  void getCommentsThrowsWhenPostDoesNotExist() {
    when(commentMapper.existsActivePostById(99L)).thenReturn(false);

    assertThatThrownBy(() -> commentGetService.getComments(99L, 1L, null, null, 10L))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
  }

  @Test
  void getCommentsThrowsWhenCursorIsIncomplete() {
    assertThatThrownBy(() -> commentGetService.getComments(
        10L,
        1L,
        LocalDateTime.of(2026, 4, 6, 9, 0),
        null,
        10L
    ))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(
                ErrorCode.COMMENT_INVALID_CURSOR));
  }
}
