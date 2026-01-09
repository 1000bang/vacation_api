package com.vacation.api.domain.user.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.enums.UserStatus;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserService 권한 체크 테스트
 * DB를 통한 통합 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServicePermissionTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User masterUser;
    private User bonbujangUser;
    private User teamjangUser;
    private User teamwonUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        // 마스터 사용자
        masterUser = User.builder()
                .email("master@test.com")
                .name("마스터")
                .password("encoded")
                .division("전체")
                .team("전체")
                .position("대표")
                .status(UserStatus.APPROVED)
                .authVal("ma")
                .build();
        masterUser = userRepository.save(masterUser);

        // 본부장 사용자
        bonbujangUser = User.builder()
                .email("bonbujang@test.com")
                .name("본부장")
                .password("encoded")
                .division("서비스사업본부")
                .team("서비스1팀")
                .position("본부장")
                .status(UserStatus.APPROVED)
                .authVal("bb")
                .build();
        bonbujangUser = userRepository.save(bonbujangUser);

        // 팀장 사용자
        teamjangUser = User.builder()
                .email("teamjang@test.com")
                .name("팀장")
                .password("encoded")
                .division("서비스사업본부")
                .team("서비스1팀")
                .position("팀장")
                .status(UserStatus.APPROVED)
                .authVal("tj")
                .build();
        teamjangUser = userRepository.save(teamjangUser);

        // 팀원 사용자
        teamwonUser = User.builder()
                .email("teamwon@test.com")
                .name("팀원")
                .password("encoded")
                .division("서비스사업본부")
                .team("서비스1팀")
                .position("과장")
                .status(UserStatus.APPROVED)
                .authVal("tw")
                .build();
        teamwonUser = userRepository.save(teamwonUser);

        // 대상 사용자 (같은 본부, 같은 팀)
        targetUser = User.builder()
                .email("target@test.com")
                .name("대상 사용자")
                .password("encoded")
                .division("서비스사업본부")
                .team("서비스1팀")
                .position("대리")
                .status(UserStatus.APPROVED)
                .authVal("tw")
                .build();
        targetUser = userRepository.save(targetUser);
    }

    @Test
    @DisplayName("권한 체크 - 마스터는 모든 사용자에 접근 가능해야 한다")
    void testCheckUserAccessPermission_Master_ShouldAllowAll() {
        // when & then - 예외가 발생하지 않아야 함
        userService.checkUserAccessPermission(masterUser.getUserId(), targetUser.getUserId());
        userService.checkUserAccessPermission(masterUser.getUserId(), bonbujangUser.getUserId());
        userService.checkUserAccessPermission(masterUser.getUserId(), teamjangUser.getUserId());
        userService.checkUserAccessPermission(masterUser.getUserId(), teamwonUser.getUserId());
    }

    @Test
    @DisplayName("권한 체크 - 본부장은 같은 본부 사용자에만 접근 가능해야 한다")
    void testCheckUserAccessPermission_Bonbujang_SameDivision() {
        // when & then - 같은 본부는 접근 가능
        userService.checkUserAccessPermission(bonbujangUser.getUserId(), targetUser.getUserId());
        userService.checkUserAccessPermission(bonbujangUser.getUserId(), teamjangUser.getUserId());
        userService.checkUserAccessPermission(bonbujangUser.getUserId(), teamwonUser.getUserId());

        // 다른 본부 사용자 생성
        final User otherDivisionUser = User.builder()
                .email("other@test.com")
                .name("다른 본부 사용자")
                .password("encoded")
                .division("플랫폼사업본부")
                .team("플랫폼1팀")
                .position("과장")
                .status(UserStatus.APPROVED)
                .authVal("tw")
                .build();
        userRepository.save(otherDivisionUser);

        // when & then - 다른 본부는 접근 불가
        assertThatThrownBy(() -> userService.checkUserAccessPermission(bonbujangUser.getUserId(), otherDivisionUser.getUserId()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.ACCESS_DENIED);
                });
    }

    @Test
    @DisplayName("권한 체크 - 팀장은 같은 팀 사용자에만 접근 가능해야 한다")
    void testCheckUserAccessPermission_Teamjang_SameTeam() {
        // when & then - 같은 팀은 접근 가능
        userService.checkUserAccessPermission(teamjangUser.getUserId(), targetUser.getUserId());
        userService.checkUserAccessPermission(teamjangUser.getUserId(), teamwonUser.getUserId());

        // 같은 본부 다른 팀 사용자 생성
        final User otherTeamUser = User.builder()
                .email("otherteam@test.com")
                .name("다른 팀 사용자")
                .password("encoded")
                .division("서비스사업본부")
                .team("서비스2팀")
                .position("과장")
                .status(UserStatus.APPROVED)
                .authVal("tw")
                .build();
        userRepository.save(otherTeamUser);

        // when & then - 다른 팀은 접근 불가
        assertThatThrownBy(() -> userService.checkUserAccessPermission(teamjangUser.getUserId(), otherTeamUser.getUserId()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.ACCESS_DENIED);
                });
    }

    @Test
    @DisplayName("권한 체크 - 팀원은 접근 권한이 없어야 한다")
    void testCheckUserAccessPermission_Teamwon_ShouldDeny() {
        // when & then - 팀원은 접근 불가
        assertThatThrownBy(() -> userService.checkUserAccessPermission(teamwonUser.getUserId(), targetUser.getUserId()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.ACCESS_DENIED);
                });
    }
}
