package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

  @PersistenceContext
  EntityManager em;

  @Autowired
  MemberJpaRepository memberJpaRepository;

  @Test
  void basicTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result = memberJpaRepository.findAll();
    assertThat(result).containsExactly(member);

    List<Member> result1 = memberJpaRepository.findByUsername("member1");
    assertThat(result1).containsExactly(member);
  }

  @Test
  void basicQuerydslTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    List<Member> result = memberJpaRepository.findAll_Querydsl();
    assertThat(result).containsExactly(member);

    List<Member> result1 = memberJpaRepository.findByUsername_Querydsl("member1");
    assertThat(result1).containsExactly(member);
  }
}