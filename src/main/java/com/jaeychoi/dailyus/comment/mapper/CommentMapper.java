package com.jaeychoi.dailyus.comment.mapper;

import com.jaeychoi.dailyus.comment.dto.CommentRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {

  boolean existsActivePostById(@Param("postId") Long postId);

  List<CommentRow> findComments(
      @Param("postId") Long postId,
      @Param("userId") Long userId,
      @Param("size") Long size,
      @Param("createdAt") LocalDateTime createdAt,
      @Param("commentId") Long commentId
  );

  List<CommentRow> findRepliesByParentIds(
      @Param("parentIds") List<Long> parentIds,
      @Param("userId") Long userId
  );
}
