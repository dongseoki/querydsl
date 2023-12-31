package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public MemberRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public List<MemberTeamDto> search(MemberSearchCondition condition) {
    return queryFactory
        .select(new QMemberTeamDto(
            member.id.as("memberId"),
            member.username,
            member.age,
            team.id.as("teamId"),
            team.name.as("teamName")
        ))
        .from(member)
        .join(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .fetch();
  }

  @Override
  public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
    QueryResults<MemberTeamDto> results = queryFactory
        .select(new QMemberTeamDto(
            member.id.as("memberId"),
            member.username,
            member.age,
            team.id.as("teamId"),
            team.name.as("teamName")
        ))
        .from(member)
        .join(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetchResults();

    List<MemberTeamDto> content = results.getResults();
    long total = results.getTotal();

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public Page<MemberTeamDto> searchPageComplexOld(MemberSearchCondition condition,
                                                  Pageable pageable) {

    // content, totalcount 쿼리 분리

    List<MemberTeamDto> content = queryFactory
        .select(new QMemberTeamDto(
            member.id.as("memberId"),
            member.username,
            member.age,
            team.id.as("teamId"),
            team.name.as("teamName")
        ))
        .from(member)
        .join(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    long total = queryFactory
        .select(member)
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .fetchCount();

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

    // content, totalcount 쿼리 분리

    List<MemberTeamDto> content = queryFactory
        .select(new QMemberTeamDto(
            member.id.as("memberId"),
            member.username,
            member.age,
            team.id.as("teamId"),
            team.name.as("teamName")
        ))
        .from(member)
        .join(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    JPAQuery<Member> countQuery = queryFactory
        .select(member)
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        );


//        return new PageImpl<>(content,pageable, total);

    //스프링 데이터 라이브러리가 제공 (PageableExecutionUtils)
    //count 쿼리가 생략 가능한 경우 생략해서 처리
    //페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
    //마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)
    return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());
  }

  @Override
  public Page<MemberTeamDto> searchPageComplexQueryDslVer5(MemberSearchCondition condition,
                                                           Pageable pageable) {
    List<MemberTeamDto> content = queryFactory
        .select(new QMemberTeamDto(member.id.as("memberId"), member.username, member.age,
            team.id.as("teamId"), team.name.as("teamName")))
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()), teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()), ageLoe(condition.getAgeLoe())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    JPAQuery<Long> countQuery = queryFactory
        .select(member.count())
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()), teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()), ageLoe(condition.getAgeLoe())
        );
    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  @Override
  public Page<MemberTeamDto> searchPageComplexUsingSort(MemberSearchCondition condition,
                                                        Pageable pageable) {

    // content, totalcount 쿼리 분리

    JPAQuery<MemberTeamDto> searchPageComplexWhereIncludePart = queryFactory
        .select(new QMemberTeamDto(
            member.id.as("memberId"),
            member.username,
            member.age,
            team.id.as("teamId"),
            team.name.as("teamName")
        ))
        .from(member)
        .join(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        );

    for (Sort.Order o : pageable.getSort()) {
      PathBuilder pathBuilder = new PathBuilder(member.getType(),
          member.getMetadata());
      searchPageComplexWhereIncludePart.orderBy(
          new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC,
              pathBuilder.get(o.getProperty())));
    }

    List<MemberTeamDto> content = searchPageComplexWhereIncludePart
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    JPAQuery<Member> countQuery = queryFactory
        .select(member)
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        );


//        return new PageImpl<>(content,pageable, total);

    //스프링 데이터 라이브러리가 제공 (PageableExecutionUtils)
    //count 쿼리가 생략 가능한 경우 생략해서 처리
    //페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
    //마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)

//    참고: 정렬( Sort )은조건이조금만 복잡해져도 Pageable 의 Sort 기능을 사용하기어렵다.
//    루트엔티티 범위를 넘어가는동적 정렬기능이 필요하면
//    스프링데이터 페이징이제공하는 Sort 를 사용하기보다는 파라미터를받아서직접 처리하는것을 권장한다.
    return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());
  }


  private BooleanExpression usernameEq(String username) {
    return StringUtils.hasText(username) ? member.username.eq(username) : null;
  }

  private BooleanExpression teamNameEq(String teamName) {
    return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
  }

  private BooleanExpression ageGoe(Integer ageGoe) {
    return ageGoe != null ? member.age.goe(ageGoe) : null;
  }

  private BooleanExpression ageLoe(Integer ageLoe) {
    return ageLoe != null ? member.age.loe(ageLoe) : null;
  }
}
