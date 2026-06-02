package com.jaeychoi.dailyus.comment.mapper;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper {

  void insert(Comment comment);

  boolean existsActivePostById(Long postId);

  Comment findActiveById(Long commentId);

  Comment findActiveCommentById(Long commentId);

  List<CommentRow> findComments(Long postId, Long userId, Long size, LocalDateTime createdAt,
      Long commentId);

  List<CommentRow> findRepliesByParentIds(List<Long> parentIds, Long userId, Long limit);

  List<CommentRow> findReplies(Long parentCommentId, Long userId, Long size,
      LocalDateTime createdAt, Long replyId);

  int updateContent(Long commentId, String content);

  void insertLike(Long commentId, Long userId);

  int deleteLike(Long commentId, Long userId);

  int applyLikeCountDelta(Long commentId, Long delta);

  Long findLikeCountByCommentId(Long commentId);

  int deleteCommentLikes(Long commentId, boolean includeReplies);

  int delete(Long commentId, boolean includeReplies);
}
