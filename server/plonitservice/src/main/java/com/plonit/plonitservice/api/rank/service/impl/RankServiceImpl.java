package com.plonit.plonitservice.api.rank.service.impl;

import com.plonit.plonitservice.api.crew.controller.response.CrewRankRes;
import com.plonit.plonitservice.api.member.controller.response.MemberRankRes;
import com.plonit.plonitservice.api.rank.controller.response.CrewAvgRes;
import com.plonit.plonitservice.api.rank.controller.response.CrewTotalRes;
import com.plonit.plonitservice.api.rank.controller.response.MembersRankRes;
import com.plonit.plonitservice.api.rank.service.RankService;
import com.plonit.plonitservice.common.enums.Rank;
import com.plonit.plonitservice.common.util.RedisUtils;
import com.plonit.plonitservice.domain.crew.repository.CrewMemberRepository;
import com.plonit.plonitservice.domain.crew.repository.CrewQueryRepository;
import com.plonit.plonitservice.domain.member.repository.MemberQueryRepository;
import com.plonit.plonitservice.domain.rank.RankingPeriod;
import com.plonit.plonitservice.domain.rank.repository.RankingPeriodQueryRepository;
import com.plonit.plonitservice.domain.rank.repository.RankingPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {
    
    private final MemberQueryRepository memberQueryRepository;
    private final CrewQueryRepository crewQueryRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final RankingPeriodQueryRepository rankingPeriodQueryRepository;
    private final RankingPeriodRepository rankingPeriodRepository;
    private final RedisUtils redisUtils;

    /**
     * 전체 회원 랭킹 조회
     * @param memberKey 멤버 식별키
     * @return 전체 회원 랭킹
     */
    @Override
    public MembersRankRes findAllMembersRank(Long memberKey) {

        MembersRankRes membersRankRes = new MembersRankRes();

        
        // 현재 랭킹 기간 조회
        RankingPeriod rankingPeriod = nowRankingPeriod();
        membersRankRes.setStartDate(rankingPeriod.getStartDate());
        membersRankRes.setEndDate(rankingPeriod.getEndDate());

        // Redis에서 랭킹 조회
        Set<ZSetOperations.TypedTuple<String>> sortedSetRangeWithScores = redisUtils.getSortedSetRangeWithScores(Rank.MEMBER.getDescription(), 0, 9);

        List<MembersRankRes.MembersRank> membersRanks = membersRankRes.getMembersRanks();
        
        List<Long> memberIds = new LinkedList<>();
        Map<Long, Object[]> distanceRankings = new HashMap<>();
        
        // 멤버 식별키와 누적거리 및 랭킹 저장
        Integer index = 0;
        for (ZSetOperations.TypedTuple<String> sortedSetRangeWithScore : sortedSetRangeWithScores) {
            log.info("sortedSetRangeWithScore = {}, {}", sortedSetRangeWithScore.getScore(), sortedSetRangeWithScore.getValue());

            Long memberId = Long.parseLong(sortedSetRangeWithScore.getValue());
            Double distance = sortedSetRangeWithScore.getScore();

            memberIds.add(memberId);
            distanceRankings.put(memberId, new Object[]{distance, ++index});
        }

        // 닉네임, 프로필이미지, 랭킹, 거리, 내꺼인지 확인
        List<MemberRankRes> memberRankResList = memberQueryRepository.findByIds(memberIds);

        for (MemberRankRes memberRankRes : memberRankResList) {
            Long memberId = memberRankRes.getMemberId();
            
            // 내꺼인지 확인
            boolean isMine = false;
            if (memberId.equals(memberKey)) {
                isMine = true;
            }
            
            // 누적 거리와 랭킹
            Object[] distanceRanking = distanceRankings.get(memberRankRes.getMemberId());
            Double distance = (Double) distanceRanking[0];
            Integer ranking = (Integer) distanceRanking[1];

            membersRanks.add(MembersRankRes.MembersRank.builder()
                    .nickName(memberRankRes.getNickName())
                    .profileImage(memberRankRes.getProfileImage())
                    .ranking(ranking)
                    .distance(distance)
                    .isMine(isMine)
                    .build());
        }

        membersRanks.sort((o1, o2) -> {
            return o1.getRanking() - o2.getRanking();
        });
        
        membersRankRes.setMembersRanks(membersRanks);
        
        return membersRankRes;
    }

    @Override
    public CrewTotalRes findAllCrewRank(Long memberKey) {
        
        // memberKey로 crewId 가져오기
        List<Long> crewIdByMemberKey = crewMemberRepository.findCrewMemberByMemberId(memberKey);

        CrewTotalRes crewTotalRes = new CrewTotalRes();

        MembersRankRes membersRankRes = new MembersRankRes();

        // 현재 랭킹 기간 조회
        RankingPeriod rankingPeriod = nowRankingPeriod();
        membersRankRes.setStartDate(rankingPeriod.getStartDate());
        membersRankRes.setEndDate(rankingPeriod.getEndDate());

        
        // Redis에서 랭킹 조회
        Set<ZSetOperations.TypedTuple<String>> sortedSetRangeWithScores = redisUtils.getSortedSetRangeWithScores(Rank.CREW.getDescription(), 0, 9);

        List<CrewTotalRes.CrewsRanks> crewsRanks = crewTotalRes.getCrewsRanks();
        
        List<Long> crewIds = new LinkedList<>();
        Map<Long, Object[]> disanceRankings = new HashMap<>();
        
        // 크루 식별키, 누적 거리, 랭킹 순위 정리
        Integer index = 0;
        for (ZSetOperations.TypedTuple<String> sortedSetRangeWithScore : sortedSetRangeWithScores) {
            Long crewKey = Long.valueOf(sortedSetRangeWithScore.getValue());
            Double distance = sortedSetRangeWithScore.getScore();

            crewIds.add(crewKey);
            disanceRankings.put(crewKey, new Object[]{distance, ++index});
        }

        // 크루 이미지, 크루 닉네임 가져오기
        List<CrewRankRes> crewRankResList = crewQueryRepository.findByIds(crewIds);

        for (CrewRankRes crewRankRes : crewRankResList) {
            Long crewKey = crewRankRes.getId();

            // 내 크루인지 확인
            boolean isMine = false;
            if (crewIdByMemberKey.contains(crewKey)) {
                isMine = true;
            }
            
            // 누적 거리와 랭킹
            Object[] distanceRanking = disanceRankings.get(crewKey);
            Double distance = (Double) distanceRanking[0];
            Integer ranking = (Integer) distanceRanking[1];

            crewsRanks.add(CrewTotalRes.CrewsRanks.builder()
                    .nickName(crewRankRes.getName())
                    .profileImage(crewRankRes.getCrewImage())
                    .ranking(ranking)
                    .distance(distance)
                    .isMine(isMine)
                    .build());
        }

        crewsRanks.sort((o1, o2) -> {
            return o1.getRanking() - o2.getRanking();
        });
        
        crewTotalRes.setCrewsRanks(crewsRanks);
        
        return crewTotalRes;
    }

    @Override
    public CrewAvgRes findAllCrewRankByAVG(Long memberKey) {

        // memberKey로 crewId 가져오기
        List<Long> crewIdByMemberKey = crewMemberRepository.findCrewMemberByMemberId(memberKey);
        
        CrewAvgRes crewAvgRes = new CrewAvgRes();

        MembersRankRes membersRankRes = new MembersRankRes();

        // 현재 랭킹 기간 조회
        RankingPeriod rankingPeriod = nowRankingPeriod();
        membersRankRes.setStartDate(rankingPeriod.getStartDate());
        membersRankRes.setEndDate(rankingPeriod.getEndDate());


        // Redis에서 랭킹 조회
        Set<ZSetOperations.TypedTuple<String>> sortedSetRangeWithScores = redisUtils.getSortedSetRangeWithScores(Rank.CREW_AVG.getDescription(), 0, 9);

        List<CrewAvgRes.CrewsAvgRanks> crewsAvgRanks = crewAvgRes.getCrewsAvgRanks();

        List<Long> crewIds = new LinkedList<>();
        Map<Long, Object[]> disanceRankings = new HashMap<>();

        // 크루 식별키, 평균 거리, 랭킹 정리
        Integer index = 0;
        for (ZSetOperations.TypedTuple<String> sortedSetRangeWithScore : sortedSetRangeWithScores) {
            Long crewKey = Long.valueOf(sortedSetRangeWithScore.getValue());
            Double distance = sortedSetRangeWithScore.getScore();

            crewIds.add(crewKey);
            disanceRankings.put(crewKey, new Object[]{distance, ++index});
        }

        // 크루 이미지, 크루 닉네임 가져오기
        List<CrewRankRes> crewRankResList = crewQueryRepository.findByIds(crewIds);

        for (CrewRankRes crewRankRes : crewRankResList) {
            Long crewKey = crewRankRes.getId();

            // 내 크루인지 확인
            boolean isMine = false;
            if (crewIdByMemberKey.contains(crewKey)) {
                isMine = true;
            }

            // 누적 거리와 랭킹
            Object[] distanceRanking = disanceRankings.get(crewKey);
            Double distance = (Double) distanceRanking[0];
            Integer ranking = (Integer) distanceRanking[1];

            crewsAvgRanks.add(CrewAvgRes.CrewsAvgRanks.builder()
                    .nickName(crewRankRes.getName())
                    .profileImage(crewRankRes.getCrewImage())
                    .ranking(ranking)
                    .distance(distance)
                    .isMine(isMine)
                    .build());
        }

        crewsAvgRanks.sort((o1, o2) -> {
            return o1.getRanking() - o2.getRanking();
        });
        
        crewAvgRes.setCrewsAvgRanks(crewsAvgRanks);

        return crewAvgRes;
    }

    /**
     * 현재 랭킹 기간 조회
     * @return 현재 랭킹 기간
     */
    private RankingPeriod nowRankingPeriod() {
        Optional<RankingPeriod> rankingPeriodOptional = rankingPeriodQueryRepository.findRecentId();
        if (rankingPeriodOptional.isPresent()) {
            return rankingPeriodOptional.get();
        }
        return null;
    }
}
