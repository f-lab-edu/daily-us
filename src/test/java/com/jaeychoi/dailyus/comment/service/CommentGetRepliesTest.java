package com.jaeychoi.dailyus.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.dto.CommentRow;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentGetRepliesTest {

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentGetService commentGetService;

  @Test
  void getRepliesReturnsRepliesWithCursor() {
    Long parentCommentId = 101L;
    Long userId = 1L;
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 4, 6, 11, 0);

    CommentRow newestReply = new CommentRow(
        205L,
        2L,
        "reply-author-1",
        null,
        "reply-1",
        3L,
        true,
        LocalDateTime.of(2026, 4, 6, 10, 30),
        false,
        parentCommentId
    );
    CommentRow olderReply = new CommentRow(
        204L,
        3L,
        "reply-author-2",
        null,
        "reply-2",
        1L,
        false,
        LocalDateTime.of(2026, 4, 6, 10, 0),
        false,
        parentCommentId
    );
    CommentRow nextReply = new CommentRow(
        203L,
        4L,
        "reply-author-3",
        null,
        "reply-3",
        0L,
        false,
        LocalDateTime.of(2026, 4, 6, 9, 30),
        false,
        parentCommentId
    );

    when(commentMapper.findReplies(parentCommentId, userId, 3L, cursorCreatedAt, 300L))
        .thenReturn(List.of(newestReply, olderReply, nextReply));

    CommentResponse response =
        commentGetService.getReplies(parentCommentId, userId, cursorCreatedAt, 300L, 2L);

    verify(commentMapper).findReplies(parentCommentId, userId, 3L, cursorCreatedAt, 300L);
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).commentId()).isEqualTo(205L);
    assertThat(response.items().get(1).commentId()).isEqualTo(204L);
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 6, 10, 0));
    assertThat(response.lastCommentId()).isEqualTo(204L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.size()).isEqualTo(2L);
  }
}
