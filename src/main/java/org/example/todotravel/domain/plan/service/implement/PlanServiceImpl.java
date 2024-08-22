package org.example.todotravel.domain.plan.service.implement;

import lombok.RequiredArgsConstructor;
import org.example.todotravel.domain.notification.dto.request.AlarmRequestDto;
import org.example.todotravel.domain.notification.service.AlarmService;
import org.example.todotravel.domain.plan.dto.request.PlanRequestDto;
import org.example.todotravel.domain.plan.dto.response.CommentResponseDto;
import org.example.todotravel.domain.plan.dto.response.PlanListResponseDto;
import org.example.todotravel.domain.plan.dto.response.PlanResponseDto;
import org.example.todotravel.domain.plan.dto.response.PlanSummaryDto;
import org.example.todotravel.domain.plan.entity.Comment;
import org.example.todotravel.domain.plan.entity.Plan;
import org.example.todotravel.domain.plan.entity.PlanUser;
import org.example.todotravel.domain.plan.entity.Schedule;
import org.example.todotravel.domain.plan.repository.PlanRepository;
import org.example.todotravel.domain.plan.service.BookmarkService;
import org.example.todotravel.domain.plan.service.CommentService;
import org.example.todotravel.domain.plan.service.LikeService;
import org.example.todotravel.domain.plan.service.PlanService;
import org.example.todotravel.domain.user.entity.User;
import org.example.todotravel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {
    private final PlanRepository planRepository;
    private final UserRepository userRepository;//테스트용
    private final BookmarkService bookmarkService;
    private final LikeService likeService;
    private final AlarmService alarmService; //알림 자동 생성
    private final CommentService commentService;

    @Override
    @Transactional
    public Plan createPlan(PlanRequestDto planRequestDto, User user) {
        //플랜 생성 시 일정과 메모가 빈 플랜이 db에 생성

        Plan plan = planRequestDto.toEntity();
        //현재 로그인 중인 사용자 user
//        User user = new User();
//        User user = userRepository.findById(1L).orElseThrow();//테스트용
        plan.setPlanUser(user);
        //planUsers에 플랜 생성자 추가
        PlanUser planUser = PlanUser.builder()
            .status(PlanUser.StatusType.ACCEPTED)
            .user(user)
            .plan(plan)
            .build();
        plan.setPlanUsers(Collections.singleton(planUser));
        return planRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Plan getPlan(Long planId) {
        return planRepository.findByPlanId(planId).orElseThrow(() -> new RuntimeException("여행 플랜을 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public Plan updatePlan(Long planId, PlanRequestDto dto) {
        Plan plan = planRepository.findByPlanId(planId).orElseThrow(() -> new RuntimeException("여행 플랜을 찾을 수 없습니다."));

        //수정을 위해 toBuilder 사용
        plan = plan.toBuilder()
            .title(dto.getTitle())
            .location(dto.getLocation())
            .startDate(dto.getStartDate())
            .endDate(dto.getEndDate())
            .isPublic(dto.getIsPublic())
            .totalBudget(dto.getTotalBudget())
            .build();

        Plan updatedPlan = planRepository.save(plan);

        AlarmRequestDto requestDto = new AlarmRequestDto(plan.getPlanUser().getUserId(),
            "[" + plan.getTitle() + "] 플랜이 수정되었습니다.");
        alarmService.createAlarm(requestDto);

        return updatedPlan;
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        planRepository.deleteByPlanId(planId);
    }

    @Override
    @Transactional
    public List<PlanListResponseDto> getPublicPlans() {
        List<Plan> plans = planRepository.findAllByIsPublicTrue();
        List<PlanListResponseDto> planList = new ArrayList<>();
        for (Plan plan : plans) {
            planList.add(PlanListResponseDto.builder()
                .planId(plan.getPlanId())
                .title(plan.getTitle())
                .location(plan.getLocation())
                .description(plan.getDescription())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .bookmarkNumber(bookmarkService.countBookmark(plan))
                .likeNumber(likeService.countLike(plan))
                .planUserNickname(plan.getPlanUser().getNickname())
                .build());
        }
        return planList;
    }

    @Override
    @Transactional
    public PlanResponseDto getPlanDetails(Long planId) {
        Plan plan = planRepository.findByPlanId(planId).orElseThrow(() -> new RuntimeException("플랜을 찾을 수 없습니다."));
        PlanResponseDto planResponseDto = PlanResponseDto.fromEntity(plan);
        List<Comment> comments = commentService.getCommentsByPlan(plan);
        List<CommentResponseDto> commentList = new ArrayList<>();
        for (Comment comment : comments) {
            commentList.add(CommentResponseDto.fromEntity(comment));
        }
        return planResponseDto.toBuilder()
            .commentList(commentList)
            .bookmarkNumber(bookmarkService.countBookmark(plan))
            .likeNumber(likeService.countLike(plan))
            .build();
    }

    @Override
    @Transactional
    public Plan copyPlan(Long planId) {
        Plan plan = planRepository.findByPlanId(planId).orElseThrow(() -> new RuntimeException("플랜을 찾을 수 없습니다."));
        //현재 로그인 중인 사용자 user
//        User user = new User();
        User user = userRepository.findById(1L).orElseThrow();
        Plan newPlan = Plan.builder()
            .title(plan.getTitle())
            .location(plan.getLocation())
            .description(plan.getDescription())
            .startDate(plan.getStartDate())
            .endDate(plan.getEndDate())
            .isPublic(false)
            .status(false)
            .totalBudget(plan.getTotalBudget())
            .planUser(user)
            .build();
        List<Schedule> newSchedules = new ArrayList<>();
        for (Schedule schedule : plan.getSchedules()) {
            newSchedules.add(Schedule.builder()
                .status(false)
                .travelDayCount(schedule.getTravelDayCount())
                .description(schedule.getDescription())
                .travelTime(schedule.getTravelTime())
                .plan(newPlan)
                .location(schedule.getLocation())
                .build()
            );
        }
        newPlan.setSchedules(newSchedules);
        //planUsers에 플랜 생성자 추가
        PlanUser planUser = PlanUser.builder()
            .status(PlanUser.StatusType.ACCEPTED)
            .user(user)
            .plan(newPlan)
            .build();
        newPlan.setPlanUsers(Collections.singleton(planUser));
        return planRepository.save(newPlan);
    }

    @Override
    @Transactional
    public List<PlanListResponseDto> getSpecificPlans(String keyword) {
        List<Plan> plans = planRepository.findAllByIsPublicTrueAndTitleContains(keyword);
        List<PlanListResponseDto> planList = new ArrayList<>();
        for (Plan plan : plans) {
            planList.add(PlanListResponseDto.builder()
                .planId(plan.getPlanId())
                .title(plan.getTitle())
                .location(plan.getLocation())
                .description(plan.getDescription())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .bookmarkNumber(bookmarkService.countBookmark(plan))
                .likeNumber(likeService.countLike(plan))
                .build());
        }
        return planList;
    }

    @Override
    @Transactional
    public PlanResponseDto getPlanForModify(Long planId) {
        Plan plan = planRepository.findByPlanId(planId).orElseThrow(() -> new RuntimeException("여행 플랜을 찾을 수 없습니다."));
        PlanResponseDto planResponseDto = PlanResponseDto.fromEntity(plan);
        return planResponseDto.toBuilder()
            .bookmarkNumber(bookmarkService.countBookmark(plan))
            .likeNumber(likeService.countLike(plan))
            .build();
    }

    // 특정 사용자가 최근 북마크한 플랜 3개 조회 후 Dto로 반환
    @Override
    @Transactional(readOnly = true)
    public List<PlanListResponseDto> getRecentBookmarkedPlans(User user) {
        List<PlanSummaryDto> plans = bookmarkService.getRecentBookmarkedPlansByUser(user.getUserId());
        return plans.stream()
            .map(this::convertSummaryToPlanListResponseDto)
            .collect(Collectors.toList());
    }

    // 특정 사용자가 북마크한 플랜 조회 후 Dto로 반환
    @Override
    @Transactional(readOnly = true)
    public List<PlanListResponseDto> getAllBookmarkedPlans(User user) {
        List<Plan> plans = bookmarkService.getAllBookmarkedPlansByUser(user.getUserId());
        return plans.stream()
            .map(this::convertToPlanListResponseDto)
            .collect(Collectors.toList());
    }

    // 특정 사용자가 최근 좋아요한 플랜 3개 조회 후 Dto로 반환
    @Override
    @Transactional(readOnly = true)
    public List<PlanListResponseDto> getRecentLikedPlans(User user) {
        List<PlanSummaryDto> plans = likeService.getRecentLikedPlansByUser(user.getUserId());
        return plans.stream()
            .map(this::convertSummaryToPlanListResponseDto)
            .collect(Collectors.toList());
    }

    // 특정 사용자가 좋아요한 플랜 조회 후 Dto로 반환
    @Override
    @Transactional(readOnly = true)
    public List<PlanListResponseDto> getAllLikedPlans(User user) {
        List<Plan> plans = likeService.getAllLikedPlansByUser(user.getUserId());
        return plans.stream()
            .map(this::convertToPlanListResponseDto)
            .collect(Collectors.toList());
    }

    @Override
    public PlanListResponseDto convertToPlanListResponseDto(Plan plan) {
        return PlanListResponseDto.builder()
            .planId(plan.getPlanId())
            .title(plan.getTitle())
            .location(plan.getLocation())
            .description(plan.getDescription())
            .startDate(plan.getStartDate())
            .endDate(plan.getEndDate())
            .bookmarkNumber(bookmarkService.countBookmark(plan))
            .likeNumber(likeService.countLike(plan))
            .planUserNickname(plan.getPlanUser().getNickname())
            .build();
    }

    private PlanListResponseDto convertSummaryToPlanListResponseDto(PlanSummaryDto summaryDto) {
        return PlanListResponseDto.builder()
            .planId(summaryDto.getPlanId())
            .title(summaryDto.getTitle())
            .location(summaryDto.getLocation())
            .description(summaryDto.getDescription())
            .startDate(summaryDto.getStartDate())
            .endDate(summaryDto.getEndDate())
            .bookmarkNumber(bookmarkService.countBookmarkByPlanId(summaryDto.getPlanId()))
            .likeNumber(likeService.countLikeByPlanId(summaryDto.getPlanId()))
            .planUserNickname(summaryDto.getPlanUserNickname())
            .build();
    }
}
