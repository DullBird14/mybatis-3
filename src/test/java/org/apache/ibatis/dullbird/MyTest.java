package org.apache.ibatis.dullbird;

import org.apache.ibatis.dullbird.bean.UserA;
import org.apache.ibatis.dullbird.bean.UserB;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.reflection.property.PropertyCopier;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.type.JdbcType;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

public class MyTest {
  @Test
  public void testPropertyCopier() {
    UserA a = new UserA();
    a.setAge(10);
    a.setName("testA");
    UserB b = new UserB();
    UserA a1 = new UserA();

    PropertyCopier.copyBeanProperties(UserA.class, a, b);
    System.out.println(b);

    PropertyCopier.copyBeanProperties(UserA.class, a, a1);
    System.out.println(a1);
  }

  @Test
  public void testJdbcType() {
    JdbcType array = JdbcType.valueOf("ARRAY");
    System.out.println(array);
  }

  @Test
  public void testAllMybatisSteps() throws IOException {
    // 1. 读取配置文件
    final Reader reader = Resources.getResourceAsReader("org/apache/ibatis/dullbird/mybatis-config.xml");
    // 2. 创建 SqlSessionFactory,其中包括了对mybatis-config.xml的解析
    // 最终都变成了 Configuration
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    // 初始化内存数据库，把表和数据创建好
    initMemDB(sqlSessionFactory);
    // 3. 获取sqlSession
    SqlSession sqlSession = sqlSessionFactory.openSession();
    // 4. 获取对应的mapper
    UserMapper mapper = sqlSession.getMapper(UserMapper.class);
    //(1)
    List<User> users = mapper.listUsers();
    users.forEach(System.out::println);
    //(2)
    User userById = mapper.getUserById(1L);
    System.out.println(userById);
  }

  private void initMemDB(SqlSessionFactory sqlSessionFactory) throws IOException {
    // 执行内存数据库初始化
    SqlSession sqlSession = sqlSessionFactory.openSession();
    final Reader reader = Resources.getResourceAsReader("org/apache/ibatis/dullbird/CreateDB-user.sql");
    ScriptRunner scriptRunner = new ScriptRunner(sqlSession.getConnection());
    scriptRunner.setLogWriter(null);
    scriptRunner.runScript(reader);
    scriptRunner.closeConnection();
    reader.close();
    sqlSession.close();
  }


}