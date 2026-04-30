package com.jaeychoi.dailyus.post.mapper;

import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {

  void insert(Post post);

  void insertImages(Long postId, List<String> imageUrls);

  boolean existsActiveById(@Param("postId") Long postId);

  int countLikesByPostIdAndUserId(@Param("postId") Long postId,
      @Param("userId") Long userId);

  void insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

  int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);

  void incrementLikeCount(@Param("postId") Long postId);

  void decrementLikeCount(@Param("postId") Long postId);

  Long findLikeCountByPostId(@Param("postId") Long postId);

  boolean existsFeedPosts(Long userId);

  List<PostFeedRow> findFeedPosts(Long userId, Long size, LocalDateTime createdAt, Long postId);

  List<PostFeedRow> findRecentFeedPosts(Long size, LocalDateTime createdAt, Long postId);

  List<PostImageRow> findImagesByPostIds(List<Long> postIds);
}
