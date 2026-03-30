package com.jaeychoi.dailyus.hashtag.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hashtag {

  private Long hashtagId;
  private String name;
  private LocalDateTime createdAt;
}
