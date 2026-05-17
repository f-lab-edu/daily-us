package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.group.dto.GroupListItemResponse;
import com.jaeychoi.dailyus.group.dto.GroupListResponse;
import com.jaeychoi.dailyus.group.dto.GroupListRow;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupListService {

  private static final long DEFAULT_SIZE = 10L;

  private final GroupMapper groupMapper;

  public GroupListResponse getGroups(LocalDateTime createdAt, Long groupId, Long size) {
    validateCursor(createdAt, groupId);
    long pageSize = resolvePageSize(size);
    List<GroupListRow> rows = groupMapper.findGroupList(pageSize + 1, createdAt, groupId);
    boolean hasNext = rows.size() > pageSize;
    List<GroupListRow> pageRows = hasNext ? rows.subList(0, (int) pageSize) : rows;
    GroupListRow lastRow = hasNext ? pageRows.get(pageRows.size() - 1) : null;

    return new GroupListResponse(
        pageRows.stream()
            .map(row -> new GroupListItemResponse(
                row.groupId(),
                row.name(),
                row.groupImage(),
                row.createdAt()
            ))
            .toList(),
        lastRow == null ? null : lastRow.createdAt(),
        lastRow == null ? null : lastRow.groupId(),
        hasNext,
        pageSize
    );
  }

  private void validateCursor(LocalDateTime createdAt, Long groupId) {
    if ((createdAt == null) != (groupId == null)) {
      throw new BaseException(ErrorCode.GROUP_INVALID_CURSOR);
    }
  }

  private long resolvePageSize(Long size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return size;
  }
}
