package com.jaeychoi.dailyus.comment.service;

import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.dto.CommentResponseItem;
import com.jaeychoi.dailyus.comment.dto.CommentRow;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentGetService {

  private static final long DEFAULT_SIZE = 10L;
  private static final long DEFAULT_REPLY_SIZE = 3L;

  private final CommentMapper commentMapper;

  public CommentResponse getComments(Long postId, Long userId, LocalDateTime createdAt,
      Long commentId, Long size) {
    validateCursor(createdAt, commentId);
    validatePostExists(postId);

    long pageSize = resolvePageSize(size);
    List<CommentRow> rows =
        commentMapper.findComments(postId, userId, pageSize + 1, createdAt, commentId);
    List<CommentRow> pageRows = getPageRows(rows, pageSize);
    Map<Long, List<CommentResponseItem>> repliesByParentId = loadRepliesByParentId(pageRows,
        userId);
    return toCommentResponse(
        rows,
        pageSize,
        currentPageRows -> toItems(currentPageRows, repliesByParentId)
    );
  }

  public CommentResponse getReplies(Long parentCommentId, Long userId, LocalDateTime createdAt,
      Long replyId, Long size) {
    validateCursor(createdAt, replyId);
    long pageSize = resolveReplyPageSize(size);

    List<CommentRow> rows = commentMapper.findReplies(
        parentCommentId,
        userId,
        pageSize + 1,
        createdAt,
        replyId
    );
    return toCommentResponse(rows, pageSize, this::toReplies);
  }

  private void validatePostExists(Long postId) {
    if (!commentMapper.existsActivePostById(postId)) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
  }

  private void validateCursor(LocalDateTime createdAt, Long commentId) {
    if ((createdAt == null) != (commentId == null)) {
      throw new BaseException(ErrorCode.COMMENT_INVALID_CURSOR);
    }
  }

  private long resolvePageSize(Long size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return size;
  }

  private long resolveReplyPageSize(Long size) {
    if (size == null || size <= 0) {
      return DEFAULT_REPLY_SIZE;
    }
    return size;
  }

  private CommentResponse toCommentResponse(
      List<CommentRow> rows,
      long pageSize,
      Function<List<CommentRow>, List<CommentResponseItem>> itemMapper
  ) {
    boolean hasNext = rows.size() > pageSize;
    List<CommentRow> pageRows = getPageRows(rows, pageSize);
    CommentRow lastRow = hasNext ? pageRows.get(pageRows.size() - 1) : null;

    return new CommentResponse(
        itemMapper.apply(pageRows),
        lastRow == null ? null : lastRow.createdAt(),
        lastRow == null ? null : lastRow.commentId(),
        hasNext,
        pageSize
    );
  }

  private List<CommentRow> getPageRows(List<CommentRow> rows, long pageSize) {
    return rows.size() > pageSize ? rows.subList(0, (int) pageSize) : rows;
  }

  private List<CommentResponseItem> toItems(
      List<CommentRow> rows,
      Map<Long, List<CommentResponseItem>> repliesByParentId
  ) {
    return rows.stream()
        .map(row -> new CommentResponseItem(
            row.commentId(),
            row.userId(),
            row.nickname(),
            row.profileImage(),
            row.content(),
            row.likeCount(),
            row.likedByMe(),
            row.createdAt(),
            row.edited(),
            row.parentId(),
            repliesByParentId.getOrDefault(row.commentId(), Collections.emptyList())
        ))
        .toList();
  }

  private List<CommentResponseItem> toReplies(List<CommentRow> rows) {
    return rows.stream()
        .map(this::toReply)
        .toList();
  }

  private Map<Long, List<CommentResponseItem>> loadRepliesByParentId(
      List<CommentRow> rows,
      Long userId
  ) {
    if (rows.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Long> parentIds = rows.stream()
        .map(CommentRow::commentId)
        .toList();

    return commentMapper.findRepliesByParentIds(parentIds, userId, DEFAULT_REPLY_SIZE).stream()
        .collect(Collectors.groupingBy(
            CommentRow::parentId,
            LinkedHashMap::new,
            Collectors.mapping(this::toReply, Collectors.toList())
        ));
  }

  private CommentResponseItem toReply(CommentRow row) {
    return new CommentResponseItem(
        row.commentId(),
        row.userId(),
        row.nickname(),
        row.profileImage(),
        row.content(),
        row.likeCount(),
        row.likedByMe(),
        row.createdAt(),
        row.edited(),
        row.parentId(),
        Collections.emptyList()
    );
  }
}
