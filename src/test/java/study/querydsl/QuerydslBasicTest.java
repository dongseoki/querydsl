package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @PersistenceContext
  EntityManager em;

  JPAQueryFactory query;


  @BeforeEach
  void before() {
    query = new JPAQueryFactory(em);
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  void startJPQL() {
    Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                          .setParameter("username", "member1")
                          .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void startQuerydsl() {
    Member findMember = query.select(member)
                             .from(member)
                             .where(member.username.eq("member1"))
                             .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void search() {
    Member findMember = query
        .selectFrom(member)
        .where(member.username.eq("member1").and(member.age.eq(10)))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
    assertThat(findMember.getAge()).isEqualTo(10);
  }

  @Test
  void search2() {
    Member findMember = query
        .selectFrom(member)
        .where(member.username.eq("member1").not())
        .fetchFirst();

    assertThat(findMember.getUsername()).isNotEqualTo("member1");
//    assertThat(findMember.getAge()).isEqualTo(10);
  }

  @Test
  void searchAndParam() {
    Member findMember = query
        .selectFrom(member)
        .where(
            member.username.eq("member1"),  //,인 경우 and
            member.age.eq(10)
        )
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
    assertThat(findMember.getAge()).isEqualTo(10);
  }

  @Test
  void resultFetch() {

    //List  리스트 조회, 데이터 없으면 빈 리스트 반환
    List<Member> fetch = query
        .selectFrom(member)
        .fetch();

    //단 건 JPQL에서 .getSingleResult 랑 같다.
    //결과가 없으면 : null
    //결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
//    Member fetchOne = query
//        .selectFrom(member)
//        .fetchOne();

    //처음 한 건 조회
    Member fetchFirst = query
        .selectFrom(member)
        .fetchFirst();

    //페이징에서 사용. 페이징 정보 포함, total count 쿼리 추가 실행
//    QueryResults<Member> results = query
//        .selectFrom(member)
//        .fetchResults();
//    results.getTotal();
//    List<Member> content = results.getResults();


    List<Member> results = query
        .selectFrom(member)
        .fetch();

    results.size();

    // 카운트 쿼리로 변경
//    long resultCount = query
//        .selectFrom(member)
//        .fetchCount();

//    long resultCount = query
//        .selectFrom(member)
//        .fetchCount();
  }
}
