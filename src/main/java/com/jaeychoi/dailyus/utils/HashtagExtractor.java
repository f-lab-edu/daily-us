package com.jaeychoi.dailyus.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HashtagExtractor {

  private static final Pattern HASHTAG_PATTERN = Pattern.compile(
      "(?<!\\S)#([\\p{L}\\p{N}_]{1,30})");

  private HashtagExtractor() {
  }

  public static String normalize(String hashtag) {
    return hashtag.toLowerCase(Locale.ROOT);
  }

  public static List<String> extract(String content) {
    if (content == null || content.isBlank()) {
      return List.of();
    }

    Matcher matcher = HASHTAG_PATTERN.matcher(content);
    List<String> hashtags = new ArrayList<>();
    Set<String> normalizedNames = new LinkedHashSet<>();

    while (matcher.find()) {
      String hashtag = matcher.group(1);
      String normalized = normalize(hashtag);
      if (normalizedNames.add(normalized)) {
        hashtags.add(normalized);
      }
    }

    return hashtags;
  }
}
