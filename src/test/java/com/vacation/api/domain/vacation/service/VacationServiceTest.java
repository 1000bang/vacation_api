package com.vacation.api.domain.vacation.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.vacation.entity.UserVacationInfo;
import com.vacation.api.domain.vacation.entity.VacationHistory;
import com.vacation.api.domain.vacation.repository.UserVacationInfoRepository;
import com.vacation.api.domain.vacation.repository.VacationHistoryRepository;
import com.vacation.api.domain.vacation.request.UpdateVacationInfoRequest;
import com.vacation.api.domain.vacation.request.VacationRequest;
import com.vacation.api.enums.UserStatus;
import com.vacation.api.enums.VacationType;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VacationService 테스트
 * DB를 통한 통합 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VacationServiceTest {

    @Autowired
    private VacationService vacationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserVacationInfoRepository userVacationInfoRepository;

    @Autowired
    private VacationHistoryRepository vacationHistoryRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("test@vacation.com")
                .name("테스트 사용자")
                .password("encoded_password")
                .division("서비스사업본부")
                .team("서비스1팀")
                .position("과장")
                .status(UserStatus.APPROVED)
                .authVal("tw")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("연차 정보 조회 - 연차 정보가 없으면 기본값(0.0)을 반환해야 한다")
    void testGetUserVacationInfo_WhenNotExists_ShouldReturnDefault() {
        // when
        UserVacationInfo result = vacationService.getUserVacationInfo(testUser.getUserId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(result.getAnnualVacationDays()).isEqualTo(0.0);
        assertThat(result.getUsedVacationDays()).isEqualTo(0.0);
        assertThat(result.getReservedVacationDays()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("연차 정보 수정 - 연차 정보가 없으면 새로 생성해야 한다")
    void testUpdateUserVacationInfo_WhenNotExists_ShouldCreateNew() {
        // given
        UpdateVacationInfoRequest request = new UpdateVacationInfoRequest();
        request.setAnnualVacationDays(15.0);
        request.setUsedVacationDays(5.0);
        request.setReservedVacationDays(2.0);

        // when
        UserVacationInfo result = vacationService.updateUserVacationInfo(testUser.getUserId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAnnualVacationDays()).isEqualTo(15.0);
        assertThat(result.getUsedVacationDays()).isEqualTo(5.0);
        assertThat(result.getReservedVacationDays()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("휴가 신청 성공 - 잔여 연차가 충분하면 휴가 신청이 성공해야 한다")
    void testCreateVacation_Success() {
        // given
        UserVacationInfo vacationInfo = UserVacationInfo.builder()
                .userId(testUser.getUserId())
                .annualVacationDays(15.0)
                .usedVacationDays(5.0)
                .reservedVacationDays(2.0)
                .build();
        userVacationInfoRepository.save(vacationInfo);

        VacationRequest request = new VacationRequest();
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setPeriod(3.0);
        request.setVacationType(VacationType.YEONCHA.name());
        request.setReason("개인 사정");
        request.setRequestDate(LocalDate.now());

        // when
        VacationHistory result = vacationService.createVacation(testUser.getUserId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(result.getPeriod()).isEqualTo(3.0);
        assertThat(result.getStatus()).isEqualTo("R"); // 미래 날짜이므로 예약중

        // 예약중 연차가 증가했는지 확인
        UserVacationInfo updated = userVacationInfoRepository.findByUserId(testUser.getUserId()).orElseThrow();
        assertThat(updated.getReservedVacationDays()).isEqualTo(5.0); // 2.0 + 3.0
    }

    @Test
    @DisplayName("휴가 신청 실패 - 잔여 연차가 부족하면 예외가 발생해야 한다")
    void testCreateVacation_WhenInsufficientDays_ShouldThrowException() {
        // given
        UserVacationInfo vacationInfo = UserVacationInfo.builder()
                .userId(testUser.getUserId())
                .annualVacationDays(15.0)
                .usedVacationDays(10.0)
                .reservedVacationDays(5.0)
                .build();
        userVacationInfoRepository.save(vacationInfo);

        VacationRequest request = new VacationRequest();
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setPeriod(3.0); // 잔여 연차는 0.0인데 3.0을 신청
        request.setVacationType(VacationType.YEONCHA.name());
        request.setReason("개인 사정");
        request.setRequestDate(LocalDate.now());

        // when & then
        assertThatThrownBy(() -> vacationService.createVacation(testUser.getUserId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.INVALID_REQUEST_FORMAT);
                });
    }

    @Test
    @DisplayName("휴가 신청 삭제 - 최신 항목만 삭제 가능해야 한다")
    void testDeleteVacation_OnlyLatestCanBeDeleted() {
        // given
        UserVacationInfo vacationInfo = UserVacationInfo.builder()
                .userId(testUser.getUserId())
                .annualVacationDays(15.0)
                .usedVacationDays(0.0)
                .reservedVacationDays(0.0)
                .build();
        userVacationInfoRepository.save(vacationInfo);

        // 첫 번째 휴가 신청
        VacationRequest request1 = new VacationRequest();
        request1.setStartDate(LocalDate.now().plusDays(1));
        request1.setEndDate(LocalDate.now().plusDays(1));
        request1.setPeriod(1.0);
        request1.setVacationType(VacationType.YEONCHA.name());
        request1.setReason("개인 사정");
        request1.setRequestDate(LocalDate.now());
        VacationHistory history1 = vacationService.createVacation(testUser.getUserId(), request1);

        // 두 번째 휴가 신청 (최신)
        VacationRequest request2 = new VacationRequest();
        request2.setStartDate(LocalDate.now().plusDays(2));
        request2.setEndDate(LocalDate.now().plusDays(2));
        request2.setPeriod(1.0);
        request2.setVacationType(VacationType.YEONCHA.name());
        request2.setReason("개인 사정");
        request2.setRequestDate(LocalDate.now());
        VacationHistory history2 = vacationService.createVacation(testUser.getUserId(), request2);

        // when & then - 최신 항목 삭제 성공
        vacationService.deleteVacation(history2.getSeq(), testUser.getUserId());
        assertThat(vacationHistoryRepository.findBySeqAndUserId(history2.getSeq(), testUser.getUserId())).isEmpty();

        // when & then - 이전 항목 삭제 실패
        assertThatThrownBy(() -> vacationService.deleteVacation(history1.getSeq(), testUser.getUserId()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.CANNOT_DELETE_OLD_VACATION);
                });
    }
}
