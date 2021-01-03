package org.apache.ibatis.dullbird.bean;

public class UserB {
  public Integer age;
  public String name;

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "UserB{" +
      "age=" + age +
      ", name='" + name + '\'' +
      '}';
  }
}
