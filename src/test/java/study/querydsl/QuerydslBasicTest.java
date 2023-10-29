package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

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

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  void paging1() {
    List<Member> result = query.selectFrom(member).orderBy(member.username.desc())
                                .offset(1)
                                .limit(2)
                                .fetch();
    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  public void paging2() {
    QueryResults<Member> queryResults = query
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetchResults();
    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
  }



  @Test
  void aggregation() {
    List<Tuple> result = query
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.max(),
            member.age.min()
        )
        .from(member)
        .fetch();

    Tuple tuple = result.get(0);

    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  /**
   * 팀의 이름과 각 팀의 평균 연령을 구해라
   */
  @Test
  void group() {
    List<Tuple> result = query
        .select(team.name, member.age.avg())
        .from(member)
        .join(member.team)
        .groupBy(team.name)
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }

  @Test
  void join() {
    List<Member> result = query
        .selectFrom(member)
        .join(member.team, team)
                        .on(member.team.name.eq("teamA"))
        .where(team.name.eq("teamA"))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  /**
   * 세타 조인
   * 회원의 이름이 팀 이름과 같은 회원을 조회
   */
  @Test
  void theta_join() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Member> result = query
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA","teamB");

    //outer join이 되지 않는다.
  }

  /**
   * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
   */
  @Test
  void join_on_filtering() {
    List<Tuple> result = query
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.name.eq("teamA"))
        .fetch();

    //> 참고: on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면,
    //where 절에서 필터링 하는 것과 기능이 동일하다. 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때,
    //내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  /**
   * 연관 관계가 없는 외부 조인 (outer)
   * 회원의 이름이 팀 이름과 같은 대상 외부 조인
   */
  @Test
  void join_on_no_relation() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result = query
        .select(member, team)
        .from(member)
        .leftJoin(team)
        .on(member.username.eq(team.name))
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    //하이버네이트 5.1부터 on 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다. 물론 내부 조인도 가능하다.
    //주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
    //일반조인: leftJoin(member.team, team)
    //on조인: from(member).leftJoin(team).on(xxx)
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  void fetchJoinNo() {
    em.flush();
    em.clear();

    Member findMember = query
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

    assertThat(loaded).isFalse();
    String name = findMember.getTeam().getName();
    System.out.println("name = " + name);
  }

  @Test
  void fetchJoin() {
    em.flush();
    em.clear();

    Member findMember = query
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

    assertThat(loaded).isTrue();
  }


  /**
   * 나이가 가장 많은 회원 조회
   */
  @Test
  void subQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = query
        .selectFrom(member)
        .where(
            member.age.eq(
                JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub)
            )
        )
        .fetch();

    assertThat(result)
        .extracting("age")
        .containsExactly(40);
  }

  /**
   * 나이가 평균 이상 회원 조회
   */
  @Test
  void subQueryGoe() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = query
        .selectFrom(member)
        .where(
            member.age.goe(
                JPAExpressions
                    .select(memberSub.age.avg())
                    .from(memberSub)
            )
        )
        .fetch();

    assertThat(result)
        .extracting("age")
        .containsExactly(30,40);
  }

  /**
   * 나이가 10살 이상 회원 조회 (IN 절)
   */
  @Test
  void subQueryIn() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = query
        .selectFrom(member)
        .where(
            member.age.in(
                JPAExpressions
                    .select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            )
        )
        .fetch();

    assertThat(result)
        .extracting("age")
        .containsExactly(20,30,40);
  }

  @Test
  void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = query
        .select(member.username,
            JPAExpressions
                .select(memberSub.age.avg())
                .from(memberSub))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    //from 절의 서브쿼리 한계
    //JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl도 지원하지 않는다.
    //하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.

    //from 절의 서브쿼리 해결방안
    //1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
    //2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
    //3. nativeSQL을 사용한다
  }

  @Test
  void basicCase() {
    List<String> result = query
        .select(member.age
            .when(10).then("10살")
            .when(20).then("20살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void complexCase() {
    List<String> result = query
        .select(
            new CaseBuilder()
                .when(member.age.between(0, 20)).then("0~20")
                .when(member.age.between(21, 30)).then("21~30")
                .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void constant() {
    List<Tuple> result = query
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  void concat() {
    List<String> result = query
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();

    //> 참고: member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로 문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void simpleProjection() {
    List<String> result = query
        .select(member.username)
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void tupleProjection() {
    List<Tuple> result = query
        .select(member.username, member.age)
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);

      System.out.println("username = " + username);
      System.out.println("age = " + age);
    }
  }

  @Test
  void findDtoByJPQL() {
    List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                               .getResultList();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }

    //순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
    //DTO의 package이름을 다 적어줘야해서 지저분함
    //생성자 방식만 지원함
  }

  @Test
  void findByDtoByQuerydslSetter() {
    //프로퍼티 접근 방식
    List<MemberDto> result = query
        .select(
            Projections.bean(MemberDto.class,
                member.username,
                member.age)
        )
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findByDtoQuerydslField() {
    //필드 접근 방식
    List<MemberDto> result = query
        .select(
            Projections.fields(MemberDto.class,
                member.username,
                member.age)
        )
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findByDtoQuerydslConstructor() {
    //생성자 방식
    List<MemberDto> result = query
        .select(
            Projections.constructor(MemberDto.class,
                member.username,
                member.age)
        )
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findByUserDtoQuerydslField() { //다른 DTO
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result = query
        .select(
            Projections.fields(UserDto.class,
                member.username.as("name"),
                ExpressionUtils.as(
                    JPAExpressions.select(memberSub.age.max())
                                  .from(memberSub), "maxAge"),
                member.age)
        )
        .from(member)
        .fetch();

    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }

  @Test
  void findByUserDtoQuerydslConstructor() { //다른 DTO
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result = query
        .select(
            Projections.constructor(UserDto.class,
                member.username,
                member.age)
        )
        .from(member)
        .fetch();

    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }

  @Test
  void findByDtoQuerydslProjection() {   //@QueryProjection 사용한 DTO
    List<MemberDto> result = query
        .select(new QMemberDto(member.username, member.age))  // Q-type DTO 사용.
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }

    //잘 안쓰는 이유는 dto가 queryDsl을 의존하게 되서 단점이다.


//    List<String> result = queryFactory
//        .select(member.username).distinct()
//        .from(member)
//        .fetch();
//    distinct 쓰는 법.
  }


  @Test
  void dynamicQuery_BooleanBuilder() {
    String usernameParam = "member1";
//    Integer ageParam = 10;
    Integer ageParam = null;

    List<Member> result = searchMember1(usernameParam, ageParam);
//    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember1(String usernameCond, Integer ageCond) {

    BooleanBuilder builder = new BooleanBuilder();

    if (usernameCond != null) {
      builder.and(member.username.eq(usernameCond));
    }

    if(ageCond != null) {
      builder.and(member.age.eq(ageCond));
    }

    return query
        .selectFrom(member)
        .where(builder)
        .fetch();
  }
}