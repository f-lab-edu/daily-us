package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.hashtag.domain.Hashtag;
import com.jaeychoi.dailyus.hashtag.mapper.HashtagMapper;
import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostCreateRequest;
import com.jaeychoi.dailyus.post.dto.PostCreateResponse;
import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import com.jaeychoi.dailyus.post.event.PostCreatedEventPublisher;
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
public class PostCreateService {

  private static final int MAX_HASHTAG_COUNT = 10;

  private final PostMapper postMapper;
  private final HashtagMapper hashtagMapper;
  private final PostCreatedEventPublisher postCreatedEventPublisher;

  @Transactional
  public PostCreateResponse createPost(Long userId, PostCreateRequest request) {
    if (request.imageUrls() == null || request.imageUrls().isEmpty()) {
      throw new BaseException(ErrorCode.POST_IMAGE_REQUIRED);
    }

    List<String> hashtags = HashtagExtractor.extract(request.content());
    validateHashtagCount(hashtags);
    Post post = Post.builder()
        .userId(userId)
        .content(request.content())
        .build();

    postMapper.insert(post);
    postMapper.insertImages(post.getPostId(), request.imageUrls());
    saveHashtags(post.getPostId(), hashtags);
    postCreatedEventPublisher.publish(new PostCreatedEvent(
        post.getPostId(),
        userId
    ));

    return new PostCreateResponse(
        post.getPostId(),
        userId,
        request.content(),
        request.imageUrls(),
        hashtags,
        post.getLikeCount()
    );
  }

  private void validateHashtagCount(List<String> hashtags) {
    if (hashtags.size() > MAX_HASHTAG_COUNT) {
      throw new BaseException(ErrorCode.POST_HASHTAG_LIMIT_EXCEEDED);
    }
  }

  private void saveHashtags(Long postId, List<String> hashtags) {
    if (hashtags.isEmpty()) {
      return;
    }
    Map<String, Hashtag> existingHashtags = findExistingHashtags(hashtags);
    List<Long> hashtagIds = resolveHashtagIds(hashtags, existingHashtags);

    if (!hashtagIds.isEmpty()) {
      hashtagMapper.insertPostHashtags(postId, hashtagIds);
    }
  }

  private Map<String, Hashtag> findExistingHashtags(List<String> hashtags) {
    return hashtagMapper.findByNames(hashtags).stream()
        .collect(Collectors.toMap(
            hashtag -> HashtagExtractor.normalize(hashtag.getName()),
            Function.identity()
        ));
  }

  private List<Long> resolveHashtagIds(List<String> hashtags,
      Map<String, Hashtag> existingHashtags) {
    List<Long> hashtagIds = new ArrayList<>();

    for (String hashtagName : hashtags) {
      Hashtag hashtag = findOrCreateHashtag(hashtagName, existingHashtags);
      hashtagIds.add(hashtag.getHashtagId());
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
        .name(hashtagName)
        .build();
    hashtagMapper.insert(newHashtag);
    existingHashtags.put(normalizedName, newHashtag);
    return newHashtag;
  }
}
