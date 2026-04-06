package com.jaeychoi.dailyus.post.mapper;

import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {

  void insert(Post post);

  void insertImages(Long postId, List<String> imageUrls);

  boolean existsFeedPosts(Long userId);

  List<PostFeedRow> findFeedPosts(Long userId, Long size, Long offset);

  List<PostFeedRow> findRecentFeedPosts(Long size, Long offset);

  List<PostImageRow> findImagesByPostIds(List<Long> postIds);
}
