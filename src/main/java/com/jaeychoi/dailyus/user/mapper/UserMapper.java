package com.jaeychoi.dailyus.user.mapper;

import com.jaeychoi.dailyus.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    boolean existsActiveByEmail(String email);

    boolean existsActiveByNickname(String nickname);

    User findActiveByEmail(String email);

    void insert(User user);
}
