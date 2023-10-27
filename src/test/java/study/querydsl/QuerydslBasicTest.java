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


  /**
   * 회원 정렬 순서
   * 1. 회원 나이 내림차순(desc)
   * 2. 회원 이름 올림차순(asc)
   * 단, 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
   */
  @Test
  void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result = query
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.asc(), member.username.asc().nullsLast())
        .fetch();

//    Member member5 = result.get(0);
//    Member member6 = result.get(1);
//    Member memberNull = result.get(2);
//
//    assertThat(member5.getUsername()).isEqualTo("member5");
//    assertThat(member6.getUsername()).isEqualTo("member6");
//    assertThat(memberNull.getUsername()).isNull();
  }
}
