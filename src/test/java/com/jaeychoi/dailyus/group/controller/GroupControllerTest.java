package com.jaeychoi.dailyus.group.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
import com.jaeychoi.dailyus.group.dto.GroupCreateRequest;
import com.jaeychoi.dailyus.group.dto.GroupCreateResponse;
import com.jaeychoi.dailyus.group.dto.GroupDetailResponse;
import com.jaeychoi.dailyus.group.dto.GroupJoinResponse;
import com.jaeychoi.dailyus.group.dto.GroupLeaveResponse;
import com.jaeychoi.dailyus.group.dto.GroupListItemResponse;
import com.jaeychoi.dailyus.group.dto.GroupListResponse;
import com.jaeychoi.dailyus.group.dto.GroupMemberRankRow;
import com.jaeychoi.dailyus.group.dto.GroupMemberResponse;
import com.jaeychoi.dailyus.group.dto.GroupRankResponse;
import com.jaeychoi.dailyus.group.service.GroupCreateService;
import com.jaeychoi.dailyus.group.service.GroupDetailService;
import com.jaeychoi.dailyus.group.service.GroupJoinService;
import com.jaeychoi.dailyus.group.service.GroupLeaveService;
import com.jaeychoi.dailyus.group.service.GroupListService;
import com.jaeychoi.dailyus.group.service.GroupMembersService;
import com.jaeychoi.dailyus.group.service.GroupRankService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

  @Mock
  private GroupCreateService groupCreateService;

  @Mock
  private GroupDetailService groupDetailService;

  @Mock
  private GroupJoinService groupJoinService;

  @Mock
  private GroupLeaveService groupLeaveService;

  @Mock
  private GroupListService groupListService;

  @Mock
  private GroupRankService groupRankService;

  @Mock
  private GroupMembersService groupMembersService;

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(
            new GroupController(groupCreateService, groupDetailService, groupJoinService,
                groupLeaveService,
                groupListService, groupRankService, groupMembersService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setValidator(validator)
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
  }

  @Test
  void getGroupMembersReturnsOkResponse() throws Exception {
    when(groupMembersService.getMembers(1L)).thenReturn(List.of(
        new GroupMemberResponse(2L, "tester", "https://example.com/profile.png"),
        new GroupMemberResponse(3L, "member", null)
    ));

    mockMvc.perform(get("/api/v1/groups/1/members"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data[0].userId").value(2L))
        .andExpect(jsonPath("$.data[0].nickname").value("tester"))
        .andExpect(jsonPath("$.data[1].userId").value(3L));
  }

  @Test
  void getGroupMembersReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
    when(groupMembersService.getMembers(1L)).thenThrow(
        new BaseException(ErrorCode.GROUP_NOT_FOUND));

    mockMvc.perform(get("/api/v1/groups/1/members"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCode.GROUP_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.GROUP_NOT_FOUND.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getGroupsReturnsOkResponse() throws Exception {
    GroupListResponse response = new GroupListResponse(
        List.of(
            new GroupListItemResponse(
                10L,
                "group-10",
                "https://example.com/10.png",
                LocalDateTime.of(2026, 5, 5, 12, 0)
            ),
            new GroupListItemResponse(
                9L,
                "group-9",
                "https://example.com/9.png",
                LocalDateTime.of(2026, 5, 5, 11, 0)
            )
        ),
        LocalDateTime.of(2026, 5, 5, 11, 0),
        9L,
        true,
        2L
    );
    when(groupListService.getGroups(null, null, 2L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/groups")
            .queryParam("size", "2")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.items[0].groupId").value(10L))
        .andExpect(jsonPath("$.data.items[0].name").value("group-10"))
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-05-05T11:00:00"))
        .andExpect(jsonPath("$.data.lastGroupId").value(9L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(2L));
  }

  @Test
  void getGroupsPassesCursorAndSizeQueryParameters() throws Exception {
    GroupListResponse response = new GroupListResponse(
        List.of(),
        LocalDateTime.of(2026, 5, 5, 8, 0),
        7L,
        true,
        5L
    );
    when(groupListService.getGroups(LocalDateTime.of(2026, 5, 5, 9, 0), 8L, 5L)).thenReturn(
        response);

    mockMvc.perform(get("/api/v1/groups")
            .queryParam("createdAt", "2026-05-05T09:00:00")
            .queryParam("groupId", "8")
            .queryParam("size", "5")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-05-05T08:00:00"))
        .andExpect(jsonPath("$.data.lastGroupId").value(7L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(5L));
  }

  @Test
  void getGroupDetailReturnsOkResponse() throws Exception {
    GroupDetailResponse response = new GroupDetailResponse(
        1L,
        "daily-us",
        "group intro",
        "https://example.com/group.png",
        2L,
        "owner",
        10
    );
    when(groupDetailService.getDetail(1L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/groups/1")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.groupId").value(1L))
        .andExpect(jsonPath("$.data.name").value("daily-us"))
        .andExpect(jsonPath("$.data.ownerNickname").value("owner"))
        .andExpect(jsonPath("$.data.memberCount").value(10));
  }

  @Test
  void getGroupDetailReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
    when(groupDetailService.getDetail(1L)).thenThrow(new BaseException(ErrorCode.GROUP_NOT_FOUND));

    mockMvc.perform(get("/api/v1/groups/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCode.GROUP_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.GROUP_NOT_FOUND.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void createGroupReturnsCreatedResponse() throws Exception {
    // given
    GroupCreateRequest request = new GroupCreateRequest(
        "daily-us",
        "group intro",
        "https://example.com/group.png"
    );
    GroupCreateResponse response = new GroupCreateResponse(
        1L,
        request.name(),
        request.intro(),
        request.groupImage(),
        2L,
        1
    );
    when(groupCreateService.create(any(), any(GroupCreateRequest.class))).thenReturn(response);

    // when
    // then
    mockMvc.perform(post("/api/v1/groups")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.message").doesNotExist())
        .andExpect(jsonPath("$.data.groupId").value(1L))
        .andExpect(jsonPath("$.data.name").value(request.name()))
        .andExpect(jsonPath("$.data.intro").value(request.intro()))
        .andExpect(jsonPath("$.data.groupImage").value(request.groupImage()))
        .andExpect(jsonPath("$.data.ownerId").value(2L));
  }

  @Test
  void createGroupReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    // given
    GroupCreateRequest request = new GroupCreateRequest(
        "daily-us",
        "group intro",
        "https://example.com/group.png"
    );

    // when
    // then
    mockMvc.perform(post("/api/v1/groups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void createGroupReturnsBadRequestWhenRequestBodyValidationFails() throws Exception {
    // given
    GroupCreateRequest request = new GroupCreateRequest("", "group intro", null);

    // when
    // then
    mockMvc.perform(post("/api/v1/groups")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.message").value("Invalid input."))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void joinGroupReturnsOkResponse() throws Exception {
    // given
    GroupJoinResponse response = new GroupJoinResponse(1L, 2L);
    when(groupJoinService.join(1L, 2L)).thenReturn(response);

    // when
    // then
    mockMvc.perform(post("/api/v1/groups/1/join")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.message").doesNotExist())
        .andExpect(jsonPath("$.data.groupId").value(1L))
        .andExpect(jsonPath("$.data.userId").value(2L));
  }

  @Test
  void joinGroupReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    // given
    // when
    // then
    mockMvc.perform(post("/api/v1/groups/1/join"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void joinGroupReturnsConflictWhenAlreadyJoined() throws Exception {
    // given
    when(groupJoinService.join(anyLong(), anyLong()))
        .thenThrow(new BaseException(ErrorCode.GROUP_ALREADY_JOINED));

    // when
    // then
    mockMvc.perform(post("/api/v1/groups/1/join")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.GROUP_ALREADY_JOINED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.GROUP_ALREADY_JOINED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void leaveGroupReturnsOkResponse() throws Exception {
    GroupLeaveResponse response = new GroupLeaveResponse(1L, 2L);
    when(groupLeaveService.leave(1L, 2L)).thenReturn(response);

    mockMvc.perform(delete("/api/v1/groups/1/leave")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.message").doesNotExist())
        .andExpect(jsonPath("$.data.groupId").value(1L))
        .andExpect(jsonPath("$.data.userId").value(2L));
  }

  @Test
  void leaveGroupReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(delete("/api/v1/groups/1/leave"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void leaveGroupReturnsForbiddenWhenUserIsNotMember() throws Exception {
    when(groupLeaveService.leave(anyLong(), anyLong()))
        .thenThrow(new BaseException(ErrorCode.GROUP_NOT_JOINED));

    mockMvc.perform(delete("/api/v1/groups/1/leave")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCode.GROUP_NOT_JOINED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.GROUP_NOT_JOINED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getRankReturnsOkResponse() throws Exception {
    GroupRankResponse response = new GroupRankResponse(
        1L,
        java.util.List.of(
            new GroupMemberRankRow(1, 2L, "tester", "https://example.com/tester.png", 3L)
        )
    );
    when(groupRankService.getRank(1L, 2L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/groups/1/rank")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(2L, "tester@example.com", "tester")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.groupId").value(1L))
        .andExpect(jsonPath("$.data.rank[0].ranking").value(1))
        .andExpect(jsonPath("$.data.rank[0].userId").value(2L))
        .andExpect(jsonPath("$.data.rank[0].nickname").value("tester"))
        .andExpect(jsonPath("$.data.rank[0].postCount").value(3L));
  }

  @Test
  void getRankReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/groups/1/rank"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

}
