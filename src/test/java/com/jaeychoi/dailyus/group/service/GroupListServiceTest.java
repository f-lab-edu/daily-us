package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.group.dto.GroupListResponse;
import com.jaeychoi.dailyus.group.dto.GroupListRow;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupListServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupListService groupListService;

  @Test
  void getGroupsReturnsCursorPageResponse() {
    when(groupMapper.findGroupList(3L, null, null)).thenReturn(List.of(
        new GroupListRow(30L, "group-30", "https://example.com/30.png",
            LocalDateTime.of(2026, 5, 5, 12, 0)),
        new GroupListRow(29L, "group-29", "https://example.com/29.png",
            LocalDateTime.of(2026, 5, 5, 11, 0)),
        new GroupListRow(28L, "group-28", "https://example.com/28.png",
            LocalDateTime.of(2026, 5, 5, 10, 0))
    ));

    GroupListResponse response = groupListService.getGroups(null, null, 2L);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).groupId()).isEqualTo(30L);
    assertThat(response.items().get(1).groupId()).isEqualTo(29L);
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 5, 11, 0));
    assertThat(response.lastGroupId()).isEqualTo(29L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.size()).isEqualTo(2L);
  }

  @Test
  void getGroupsReturnsEmptyResponseWhenNoGroupsExist() {
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 5, 5, 9, 0);
    when(groupMapper.findGroupList(11L, cursorCreatedAt, 15L)).thenReturn(List.of());

    GroupListResponse response = groupListService.getGroups(cursorCreatedAt, 15L, 10L);

    assertThat(response.items()).isEmpty();
    assertThat(response.lastCreatedAt()).isNull();
    assertThat(response.lastGroupId()).isNull();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.size()).isEqualTo(10L);
  }
}
