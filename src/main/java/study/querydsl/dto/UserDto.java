package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {
  private String name;
  private int maxAge;
  private int age;

  public UserDto(String name, int age) {
    this.name = name;
    this.age = age;
  }

  public UserDto(String name, int maxAge, int age) {
    this.name = name;
    this.maxAge = maxAge;
    this.age = age;
  }
}
