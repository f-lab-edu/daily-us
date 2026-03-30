package com.jaeychoi.dailyus.post.mapper;

import com.jaeychoi.dailyus.post.domain.Post;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {

  void insert(Post post);

  void insertImages(Long postId, List<String> imageUrls);
}
