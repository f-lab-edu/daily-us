package com.jaeychoi.dailyus.group.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupDetailRow;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest
class GroupMapperTest {

  @Autowired
  private GroupMapper groupMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void insertPersistsGroupAndSetsGeneratedKey() {
    // given
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Group group = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();

    // when
    groupMapper.insert(group);

    // then
    assertThat(group.getGroupId()).isNotNull();
    assertThat(countById(group.getGroupId())).isEqualTo(1);
    assertThat(findOwnerId(group.getGroupId())).isEqualTo(ownerId);
  }

  @Test
  void insertMemberPersistsGroupMembership() {
    // given
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Long memberId = insertUser(uniqueEmail("member"), uniqueNickname("member"));
    Group group = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();
    groupMapper.insert(group);

    // when
    groupMapper.insertMember(group.getGroupId(), memberId);

    // then
    assertThat(countGroupMember(group.getGroupId(), memberId)).isEqualTo(1);
  }

  @Test
  void findActiveByIdReturnsActiveGroup() {
    // given
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Group group = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();
    groupMapper.insert(group);
    jdbcTemplate.update(
        "UPDATE user_groups SET member_count = ? WHERE group_id = ?",
        3,
        group.getGroupId()
    );

    // when
    Group found = groupMapper.findActiveById(group.getGroupId());

    // then
    assertThat(found).isNotNull();
    assertThat(found.getGroupId()).isEqualTo(group.getGroupId());
    assertThat(found.getOwnerId()).isEqualTo(ownerId);
    assertThat(found.getMemberCount()).isEqualTo(3);
  }

  @Test
  void existsMemberByIdAndMemberIdReturnsTrueWhenMemberExists() {
    // given
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Long memberId = insertUser(uniqueEmail("member"), uniqueNickname("member"));
    Group group = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();
    groupMapper.insert(group);
    groupMapper.insertMember(group.getGroupId(), memberId);

    // when
    boolean joined = groupMapper.existsMemberByIdAndMemberId(group.getGroupId(), memberId);

    // then
    assertThat(joined).isTrue();
  }

  @Test
  void countJoinedGroupsByMemberIdReturnsJoinedGroupCount() {
    // given
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Long memberId = insertUser(uniqueEmail("member"), uniqueNickname("member"));
    Long secondOwnerId = insertUser(uniqueEmail("owner2"), uniqueNickname("owner2"));
    Group firstGroup = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();
    Group secondGroup = Group.builder()
        .name("daily-us-2")
        .intro("group intro 2")
        .groupImage("https://example.com/group2.png")
        .ownerId(secondOwnerId)
        .build();
    groupMapper.insert(firstGroup);
    groupMapper.insert(secondGroup);
    groupMapper.insertMember(firstGroup.getGroupId(), memberId);
    groupMapper.insertMember(secondGroup.getGroupId(), memberId);

    // when
    int joinedGroupCount = groupMapper.countJoinedGroupsByMemberId(memberId);

    // then
    assertThat(joinedGroupCount).isEqualTo(2);
  }

  @Test
  void increaseMemberCountUpdatesGroupMemberCount() {
    // given
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Group group = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();
    groupMapper.insert(group);

    // when
    groupMapper.increaseMemberCount(group.getGroupId());

    // then
    assertThat(findMemberCount(group.getGroupId())).isEqualTo(1);
  }

  @Test
  void findDetailByIdReturnsActiveGroupDetailWithOwnerNickname() {
    Long ownerId = insertUser(uniqueEmail("owner"), uniqueNickname("owner"));
    Group group = Group.builder()
        .name("daily-us")
        .intro("group intro")
        .groupImage("https://example.com/group.png")
        .ownerId(ownerId)
        .build();
    groupMapper.insert(group);
    jdbcTemplate.update(
        "UPDATE user_groups SET member_count = ? WHERE group_id = ?",
        3,
        group.getGroupId()
    );

    GroupDetailRow found = groupMapper.findDetailById(group.getGroupId());

    assertThat(found).isNotNull();
    assertThat(found.groupId()).isEqualTo(group.getGroupId());
    assertThat(found.name()).isEqualTo("daily-us");
    assertThat(found.ownerId()).isEqualTo(ownerId);
    assertThat(found.ownerNickname()).isNotBlank();
    assertThat(found.memberCount()).isEqualTo(3);
  }

  private Long insertUser(String email, String nickname) {
    jdbcTemplate.update(
        """
            INSERT INTO users (
                email,
                password,
                nickname,
                follower_count,
                followee_count,
                profile_image,
                deleted_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
        email,
        "encoded-password",
        nickname,
        0L,
        0L,
        null,
        null
    );

    return jdbcTemplate.queryForObject(
        "SELECT user_id FROM users WHERE email = ?",
        Long.class,
        email
    );
  }

  private int countById(Long id) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_groups WHERE group_id = ?",
        Integer.class,
        id
    );
    assertThat(count).isNotNull();
    return count;
  }

  private Long findOwnerId(Long groupId) {
    return jdbcTemplate.queryForObject(
        "SELECT owner_id FROM user_groups WHERE group_id = ?",
        Long.class,
        groupId
    );
  }

  private Integer findMemberCount(Long groupId) {
    return jdbcTemplate.queryForObject(
        "SELECT member_count FROM user_groups WHERE group_id = ?",
        Integer.class,
        groupId
    );
  }

  private int countGroupMember(Long groupId, Long userId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?",
        Integer.class,
        groupId,
        userId
    );
    assertThat(count).isNotNull();
    return count;
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueNickname(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }
}
