package org.apache.ibatis.dullbird;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * mybatis测试
 *
 * @author : cys
 * date: 2021-02-21 10:32
 */
public interface UserMapper {

    List<User> listUsers();

    User getUserById(@Param("id") Long id);
}