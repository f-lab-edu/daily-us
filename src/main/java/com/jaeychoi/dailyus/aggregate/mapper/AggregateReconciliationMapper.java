package com.jaeychoi.dailyus.aggregate.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AggregateReconciliationMapper {

  List<Long> findPostIdsWithLikeCountMismatch(@Param("limit") int limit);

  int reconcilePostLikeCount(@Param("postId") Long postId);

  List<Long> findCommentIdsWithLikeCountMismatch(@Param("limit") int limit);

  int reconcileCommentLikeCount(@Param("commentId") Long commentId);

  List<Long> findUserIdsWithFollowerCountMismatch(@Param("limit") int limit);

  int reconcileFollowerCount(@Param("userId") Long userId);

  List<Long> findUserIdsWithFolloweeCountMismatch(@Param("limit") int limit);

  int reconcileFolloweeCount(@Param("userId") Long userId);

  List<Long> findGroupIdsWithMemberCountMismatch(@Param("limit") int limit);

  int reconcileMemberCount(@Param("groupId") Long groupId);
}
