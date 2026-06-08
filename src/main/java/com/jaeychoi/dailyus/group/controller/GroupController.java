package com.jaeychoi.dailyus.group.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.group.dto.GroupCreateRequest;
import com.jaeychoi.dailyus.group.dto.GroupCreateResponse;
import com.jaeychoi.dailyus.group.dto.GroupDetailResponse;
import com.jaeychoi.dailyus.group.dto.GroupJoinResponse;
import com.jaeychoi.dailyus.group.dto.GroupLeaveResponse;
import com.jaeychoi.dailyus.group.dto.GroupListResponse;
import com.jaeychoi.dailyus.group.dto.GroupMemberResponse;
import com.jaeychoi.dailyus.group.dto.GroupRankResponse;
import com.jaeychoi.dailyus.group.dto.GroupUpdateRequest;
import com.jaeychoi.dailyus.group.dto.GroupUpdateResponse;
import com.jaeychoi.dailyus.group.service.GroupCreateService;
import com.jaeychoi.dailyus.group.service.GroupDeleteService;
import com.jaeychoi.dailyus.group.service.GroupDetailService;
import com.jaeychoi.dailyus.group.service.GroupJoinService;
import com.jaeychoi.dailyus.group.service.GroupLeaveService;
import com.jaeychoi.dailyus.group.service.GroupListService;
import com.jaeychoi.dailyus.group.service.GroupMembersService;
import com.jaeychoi.dailyus.group.service.GroupRankService;
import com.jaeychoi.dailyus.group.service.GroupUpdateService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

  private final GroupCreateService groupCreateService;
  private final GroupDetailService groupDetailService;
  private final GroupDeleteService groupDeleteService;
  private final GroupJoinService groupJoinService;
  private final GroupLeaveService groupLeaveService;
  private final GroupListService groupListService;
  private final GroupRankService groupRankService;
  private final GroupMembersService groupMembersService;
  private final GroupUpdateService groupUpdateService;


  @GetMapping
  @AuthRequired
  public ApiResponse<GroupListResponse> getGroups(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long groupId,
      @RequestParam(required = false, defaultValue = "10") Long size
  ) {
    return ApiResponse.success(groupListService.getGroups(createdAt, groupId, size));
  }

  @GetMapping("/{groupId}/members")
  @AuthRequired
  public ApiResponse<List<GroupMemberResponse>> getGroupMembers(@PathVariable Long groupId) {
    return ApiResponse.success(groupMembersService.getMembers(groupId));
  }


  @GetMapping("/{groupId}")
  @AuthRequired
  public ApiResponse<GroupDetailResponse> getGroupDetail(@PathVariable Long groupId) {
    return ApiResponse.success(groupDetailService.getDetail(groupId));
  }

  @DeleteMapping("/{groupId}")
  @AuthRequired
  public ApiResponse<Void> deleteGroup(@PathVariable Long groupId,
      @AuthenticatedUser CurrentUser user) {
    groupDeleteService.delete(groupId, user.userId());
    return ApiResponse.success(null);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @AuthRequired
  public ApiResponse<GroupCreateResponse> createGroup(
      @AuthenticatedUser CurrentUser user,
      @Valid @RequestBody GroupCreateRequest request
  ) {
    GroupCreateResponse response = groupCreateService.create(user.userId(), request);
    return ApiResponse.success(response);
  }

  @PatchMapping("/{groupId}")
  @AuthRequired
  public ApiResponse<GroupUpdateResponse> updateGroup(
      @PathVariable Long groupId,
      @AuthenticatedUser CurrentUser user,
      @Valid @RequestBody GroupUpdateRequest request
  ) {
    GroupUpdateResponse response = groupUpdateService.update(groupId, user.userId(), request);
    return ApiResponse.success(response);
  }

  @PostMapping("/{groupId}/join")
  @AuthRequired
  public ApiResponse<GroupJoinResponse> joinGroup(@PathVariable Long groupId,
      @AuthenticatedUser CurrentUser user) {
    GroupJoinResponse response = groupJoinService.join(groupId, user.userId());
    return ApiResponse.success(response);
  }

  @DeleteMapping("/{groupId}/leave")
  @AuthRequired
  public ApiResponse<GroupLeaveResponse> leaveGroup(@PathVariable Long groupId,
      @AuthenticatedUser CurrentUser user) {
    GroupLeaveResponse response = groupLeaveService.leave(groupId, user.userId());
    return ApiResponse.success(response);
  }

  @GetMapping("/{groupId}/rank")
  @AuthRequired
  public ApiResponse<GroupRankResponse> getRank(@PathVariable Long groupId,
      @AuthenticatedUser CurrentUser user) {
    GroupRankResponse response = groupRankService.getRank(groupId, user.userId());
    return ApiResponse.success(response);
  }
}
