package com.jaeychoi.dailyus.comment.mapper;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper {

  boolean existsActivePostById(Long postId);

  Comment findActiveById(Long commentId);

  List<CommentRow> findComments(Long postId, Long userId, Long size, LocalDateTime createdAt,
      Long commentId);

  List<CommentRow> findRepliesByParentIds(List<Long> parentIds, Long userId, Long limit);

  int deleteCommentLikes(Long commentId, boolean includeReplies);

  int delete(Long commentId, boolean includeReplies);
}
