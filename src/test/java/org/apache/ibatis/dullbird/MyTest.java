package org.apache.ibatis.dullbird;

import org.apache.ibatis.dullbird.bean.UserA;
import org.apache.ibatis.dullbird.bean.UserB;
import org.apache.ibatis.reflection.property.PropertyCopier;
import org.apache.ibatis.type.JdbcType;
import org.junit.Test;

public class MyTest {
  @Test
  public void testPropertyCopier(){
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



}
