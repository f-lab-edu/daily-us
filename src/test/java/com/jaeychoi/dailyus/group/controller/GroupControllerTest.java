package com.jaeychoi.dailyus.group.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
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
import com.jaeychoi.dailyus.group.service.GroupCreateService;
import com.jaeychoi.dailyus.group.service.GroupDetailService;
import com.jaeychoi.dailyus.group.service.GroupJoinService;
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

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(
            new GroupController(groupCreateService, groupDetailService, groupJoinService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setValidator(validator)
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
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
}
