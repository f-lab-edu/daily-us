package com.jaeychoi.dailyus.hashtag.mapper;

import com.jaeychoi.dailyus.hashtag.domain.Hashtag;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HashtagMapper {

  List<Hashtag> findByNames(@Param("names") List<String> names);

  void insert(Hashtag hashtag);

  void insertPostHashtags(@Param("postId") Long postId, @Param("hashtagIds") List<Long> hashtagIds);
}
