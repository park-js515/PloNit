package com.plonit.plonitservice.domain.crewping.repository;

import com.plonit.plonitservice.api.crewping.controller.response.FindCrewpingsRes;
import com.plonit.plonitservice.api.member.controller.response.FindCrewpingInfoRes;
import com.plonit.plonitservice.common.enums.Status;
import com.plonit.plonitservice.domain.crewping.Crewping;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.plonit.plonitservice.domain.crewping.QCrewping.crewping;
import static com.plonit.plonitservice.domain.crewping.QCrewpingMember.crewpingMember;
import static com.plonit.plonitservice.domain.member.QMember.member;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.types.Projections.list;
import static java.time.temporal.ChronoUnit.DAYS;

@Repository
public class CrewpingQueryRepository {

    private final JPAQueryFactory queryFactory;

    public CrewpingQueryRepository(EntityManager em) { this.queryFactory = new JPAQueryFactory(em); }

    public List<FindCrewpingsRes> findCrewpings(Long crewId) {
        StringTemplate startDateFormat = Expressions.stringTemplate("DATE_FORMAT({0}, {1})", crewping.startDate, ConstantImpl.create("%Y-%m-%d %H:%i"));
        StringTemplate endDateFormat = Expressions.stringTemplate("DATE_FORMAT({0}, {1})", crewping.endDate, ConstantImpl.create("%Y-%m-%d %H:%i"));

        return queryFactory
                .select(Projections.constructor(FindCrewpingsRes.class,
                        crewping.id,
                        crewping.name,
                        crewping.crewpingImage,
                        crewping.place,
                        startDateFormat,
                        endDateFormat,
                        crewping.cntPeople,
                        crewping.maxPeople))
                .from(crewping)
                .where(crewping.crew.id.eq(crewId))
                .fetch();
    }

    public List<FindCrewpingInfoRes> findCrewpingInfo(Long memberId) {
        // Crewping 기본 정보
        List<Crewping> crewpings = queryFactory
                .selectFrom(crewping)
                .join(crewpingMember)
                .on(crewpingMember.crewping.eq(crewping))
                .where(crewpingMember.member.id.eq(memberId), crewping.status.in(Status.ACTIVE, Status.ONGOING))
                .fetch();

        Map<Long, List<?>> memberProfilesMap = queryFactory
                .selectFrom(member)
                .join(crewpingMember)
                .on(crewpingMember.member.eq(member))
                .where(crewpingMember.crewping.id.in(crewpings.stream().map(Crewping::getId).collect(Collectors.toList())))
                .transform(groupBy(crewpingMember.crewping.id).as(list(member.profileImage)));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return crewpings.stream().map(crewpingEntity -> {
            List<String> memberProfiles = (List<String>) memberProfilesMap.getOrDefault(crewpingEntity.getId(), Collections.emptyList());
            Long dDay = DAYS.between(LocalDate.now(), crewpingEntity.getStartDate());

            return FindCrewpingInfoRes.builder()
                    .id(crewpingEntity.getId())
                    .crewName(crewpingEntity.getCrewName())
                    .crewpingName(crewpingEntity.getName())
                    .crewpingImage(crewpingEntity.getCrewpingImage())
                    .dDay(dDay)
                    .startDate(crewpingEntity.getStartDate().format(formatter))
                    .endDate(crewpingEntity.getEndDate().format(formatter))
                    .place(crewpingEntity.getPlace())
                    .cntPeople(crewpingEntity.getCntPeople())
                    .memberProfileImage(memberProfiles)
                    .build();
        }).collect(Collectors.toList());
    }
}
