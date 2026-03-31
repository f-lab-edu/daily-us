package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupCreateRequest;
import com.jaeychoi.dailyus.group.dto.GroupCreateResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupCreateServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupCreateService groupCreateService;

  @Test
  void createPersistsGroupAndAddsOwnerAsMember() {
    // given
    Long ownerId = 1L;
    GroupCreateRequest request = new GroupCreateRequest(
        "daily-us",
        "group intro",
        "https://example.com/group.png"
    );
    doAnswer(invocation -> {
      Group group = invocation.getArgument(0);
      group.setGroupId(10L);
      return null;
    }).when(groupMapper).insert(any(Group.class));

    // when
    GroupCreateResponse response = groupCreateService.create(ownerId, request);

    // then
    ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
    verify(groupMapper).insert(groupCaptor.capture());
    verify(groupMapper).insertMember(10L, ownerId);

    Group savedGroup = groupCaptor.getValue();
    assertThat(savedGroup.getName()).isEqualTo(request.name());
    assertThat(savedGroup.getIntro()).isEqualTo(request.intro());
    assertThat(savedGroup.getGroupImage()).isEqualTo(request.groupImage());
    assertThat(savedGroup.getOwnerId()).isEqualTo(ownerId);

    assertThat(response.groupId()).isEqualTo(10L);
    assertThat(response.name()).isEqualTo(request.name());
    assertThat(response.intro()).isEqualTo(request.intro());
    assertThat(response.groupImage()).isEqualTo(request.groupImage());
    assertThat(response.ownerId()).isEqualTo(ownerId);
  }
}
