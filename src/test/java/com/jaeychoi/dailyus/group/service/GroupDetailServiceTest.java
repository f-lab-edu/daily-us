package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.dto.GroupDetailResponse;
import com.jaeychoi.dailyus.group.dto.GroupDetailRow;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupDetailServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupDetailService groupDetailService;

  @Test
  void getDetailReturnsGroupDetailResponse() {
    when(groupMapper.findDetailById(10L)).thenReturn(new GroupDetailRow(
        10L,
        "daily-us",
        "group intro",
        "https://example.com/group.png",
        2L,
        "owner",
        5
    ));

    GroupDetailResponse response = groupDetailService.getDetail(10L);

    assertThat(response.groupId()).isEqualTo(10L);
    assertThat(response.name()).isEqualTo("daily-us");
    assertThat(response.ownerNickname()).isEqualTo("owner");
    assertThat(response.memberCount()).isEqualTo(5);
  }

  @Test
  void getDetailThrowsWhenGroupDoesNotExist() {
    when(groupMapper.findDetailById(10L)).thenReturn(null);

    assertThatThrownBy(() -> groupDetailService.getDetail(10L))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_NOT_FOUND));
  }
}
