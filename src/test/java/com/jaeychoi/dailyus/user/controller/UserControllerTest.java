package com.jaeychoi.dailyus.user.controller;

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
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.dto.UserGroupItemResponse;
import com.jaeychoi.dailyus.user.dto.UserGroupResponse;
import com.jaeychoi.dailyus.user.service.UserFollowService;
import com.jaeychoi.dailyus.user.service.UserMyGroupService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserFollowService userFollowService;

  @Mock
  private UserMyGroupService userMyGroupService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userFollowService, userMyGroupService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
  }

  @Test
  void getMyGroupsReturnsOkResponse() throws Exception {
    UserGroupResponse response = new UserGroupResponse(List.of(
        new UserGroupItemResponse(10L, "daily-us", "https://example.com/group.png"),
        new UserGroupItemResponse(11L, "backend-study", null)
    ));
    when(userMyGroupService.getMyGroups(1L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/me/groups")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.items[0].groupId").value(10L))
        .andExpect(jsonPath("$.data.items[0].name").value("daily-us"))
        .andExpect(jsonPath("$.data.items[0].groupImage").value("https://example.com/group.png"))
        .andExpect(jsonPath("$.data.items[1].groupId").value(11L))
        .andExpect(jsonPath("$.data.items[1].name").value("backend-study"));
  }

  @Test
  void getMyGroupsReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/me/groups"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void followReturnsCreatedResponse() throws Exception {
    UserFollowResponse response = new UserFollowResponse(2L, true, 3L, 1L);
    when(userFollowService.follow(1L, 2L)).thenReturn(response);

    mockMvc.perform(post("/api/v1/users/2/follow")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.followee").value(2L))
        .andExpect(jsonPath("$.data.following").value(true))
        .andExpect(jsonPath("$.data.followerCount").value(3L))
        .andExpect(jsonPath("$.data.followeeCount").value(1L));
  }

  @Test
  void unfollowReturnsOkResponse() throws Exception {
    UserFollowResponse response = new UserFollowResponse(2L, false, 2L, 1L);
    when(userFollowService.unfollow(1L, 2L)).thenReturn(response);

    mockMvc.perform(delete("/api/v1/users/2/follow")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.followee").value(2L))
        .andExpect(jsonPath("$.data.following").value(false))
        .andExpect(jsonPath("$.data.followerCount").value(2L))
        .andExpect(jsonPath("$.data.followeeCount").value(1L));
  }

  @Test
  void followReturnsConflictWhenRelationshipAlreadyExists() throws Exception {
    when(userFollowService.follow(anyLong(), anyLong()))
        .thenThrow(new BaseException(ErrorCode.FOLLOW_ALREADY_EXISTS));

    mockMvc.perform(post("/api/v1/users/2/follow")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.FOLLOW_ALREADY_EXISTS.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.FOLLOW_ALREADY_EXISTS.getMessage()));
  }
}
