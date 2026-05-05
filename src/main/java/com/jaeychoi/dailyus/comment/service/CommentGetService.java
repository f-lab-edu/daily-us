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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentGetService {

  private static final long DEFAULT_SIZE = 10L;

  private final CommentMapper commentMapper;

  public CommentResponse getComments(
      Long postId,
      Long userId,
      LocalDateTime createdAt,
      Long commentId,
      Long size
  ) {
    validateCursor(createdAt, commentId);
    validatePostExists(postId);

    long pageSize = resolvePageSize(size);
    List<CommentRow> rows = commentMapper.findComments(postId, userId, pageSize + 1, createdAt, commentId);
    boolean hasNext = rows.size() > pageSize;
    List<CommentRow> pageRows = hasNext ? rows.subList(0, (int) pageSize) : rows;
    Map<Long, List<CommentResponseItem>> repliesByParentId = loadRepliesByParentId(pageRows, userId);
    CommentRow lastRow = hasNext ? pageRows.get(pageRows.size() - 1) : null;

    return new CommentResponse(
        toItems(pageRows, repliesByParentId),
        lastRow == null ? null : lastRow.createdAt(),
        lastRow == null ? null : lastRow.commentId(),
        hasNext,
        pageSize
    );
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
            row.parentId(),
            repliesByParentId.getOrDefault(row.commentId(), Collections.emptyList())
        ))
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

    return commentMapper.findRepliesByParentIds(parentIds, userId).stream()
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
        row.parentId(),
        Collections.emptyList()
    );
  }
}
