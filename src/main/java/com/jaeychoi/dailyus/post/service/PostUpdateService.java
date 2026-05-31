package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.hashtag.domain.Hashtag;
import com.jaeychoi.dailyus.hashtag.mapper.HashtagMapper;
import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostUpdateRequest;
import com.jaeychoi.dailyus.post.dto.PostUpdateResponse;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.utils.HashtagExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostUpdateService {

  private static final int MAX_HASHTAG_COUNT = 10;

  private final PostMapper postMapper;
  private final HashtagMapper hashtagMapper;

  @Transactional
  public PostUpdateResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
    validateImages(request.imageUrls());

    Post post = getActivePost(postId);
    validateOwner(userId, post);

    List<String> hashtags = extractAndValidateHashtags(request.content());
    updatePostContent(postId, request.content());
    replaceImages(postId, request.imageUrls());
    replaceHashtags(postId, hashtags);

    return new PostUpdateResponse(
        postId,
        post.getUserId(),
        request.content(),
        request.imageUrls(),
        hashtags,
        post.getLikeCount()
    );
  }

  private void validateImages(List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      throw new BaseException(ErrorCode.POST_IMAGE_REQUIRED);
    }
  }

  private Post getActivePost(Long postId) {
    Post post = postMapper.findById(postId);
    if (post == null) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
    return post;
  }

  private void validateOwner(Long userId, Post post) {
    if (!post.getUserId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }
  }

  private List<String> extractAndValidateHashtags(String content) {
    List<String> hashtags = HashtagExtractor.extract(content);
    if (hashtags.size() > MAX_HASHTAG_COUNT) {
      throw new BaseException(ErrorCode.POST_HASHTAG_LIMIT_EXCEEDED);
    }
    return hashtags;
  }

  private void updatePostContent(Long postId, String content) {
    postMapper.updateContent(postId, content);
  }

  private void replaceImages(Long postId, List<String> imageUrls) {
    postMapper.deleteImagesByPostId(postId);
    postMapper.insertImages(postId, imageUrls);
  }

  private void replaceHashtags(Long postId, List<String> hashtags) {
    hashtagMapper.deletePostHashtags(postId);
    if (hashtags.isEmpty()) {
      return;
    }

    Map<String, Hashtag> existingHashtags = findExistingHashtags(hashtags);
    List<Long> hashtagIds = resolveHashtagIds(hashtags, existingHashtags);
    hashtagMapper.insertPostHashtags(postId, hashtagIds);
  }

  private Map<String, Hashtag> findExistingHashtags(List<String> hashtags) {
    return hashtagMapper.findByNames(hashtags).stream()
        .collect(Collectors.toMap(
            hashtag -> HashtagExtractor.normalize(hashtag.getName()),
            Function.identity()
        ));
  }

  private List<Long> resolveHashtagIds(
      List<String> hashtags,
      Map<String, Hashtag> existingHashtags
  ) {
    List<Long> hashtagIds = new ArrayList<>();
    for (String hashtagName : hashtags) {
      hashtagIds.add(findOrCreateHashtag(hashtagName, existingHashtags).getHashtagId());
    }
    return hashtagIds;
  }

  private Hashtag findOrCreateHashtag(String hashtagName, Map<String, Hashtag> existingHashtags) {
    String normalizedName = HashtagExtractor.normalize(hashtagName);
    Hashtag hashtag = existingHashtags.get(normalizedName);
    if (hashtag != null) {
      return hashtag;
    }

    Hashtag newHashtag = Hashtag.builder()
        .name(normalizedName)
        .build();
    hashtagMapper.insert(newHashtag);
    existingHashtags.put(normalizedName, newHashtag);
    return newHashtag;
  }
}
