package com.jaeychoi.dailyus.post.mapper;

import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {

  void insert(Post post);

  void insertImages(Long postId, List<String> imageUrls);

  void updateContent(Long postId, String content);

  void deleteImagesByPostId(Long postId);

  boolean existsActiveById(Long postId);

  int countLikesByPostIdAndUserId(Long postId, Long userId);

  void insertLike(Long postId, Long userId);

  int deleteLike(Long postId, Long userId);

  void incrementLikeCount(Long postId);

  void decrementLikeCount(Long postId);

  Long findLikeCountByPostId(Long postId);

  Post findById(Long postId);

  boolean existsFeedPosts(Long userId);

  long countActiveByUserId(Long userId);

  List<PostFeedRow> findFeedPosts(Long userId, Long size, LocalDateTime createdAt, Long postId);

  List<PostFeedRow> findRecentFeedPosts(Long size, LocalDateTime createdAt, Long postId);

  List<Integer> findActivityDaysByUserId(Long userId, LocalDateTime startAt, LocalDateTime endAt);

  List<PostFeedRow> findFeedPostsByIds(List<Long> postIds);

  List<PostFeedRow> findPostsByUserId(Long userId, Long size, LocalDateTime createdAt, Long postId);

  List<PostImageRow> findImagesByPostIds(List<Long> postIds);
}
