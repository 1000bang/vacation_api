package com.vacation.api.validation;

import com.vacation.api.domain.vacation.request.VacationSampleRequest;
import com.vacation.api.enums.VacationType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VacationSampleRequest Validation 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@SpringBootTest
class VacationSampleRequestValidationTest {

    @Autowired
    private Validator validator;

    private VacationSampleRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new VacationSampleRequest();
        validRequest.setRequestDate(LocalDate.of(2025, 12, 26));
        validRequest.setDepartment("서비스본부/서비스개발2팀");
        validRequest.setApplicant("천병재");
        validRequest.setStartDate(LocalDate.of(2025, 9, 5));
        validRequest.setEndDate(LocalDate.of(2025, 9, 5));
        validRequest.setVacationType(VacationType.YEONCHA);
        validRequest.setReason("개인 사정");
        validRequest.setTotalVacationDays(18.0);
        validRequest.setRemainingVacationDays(8.0);
        validRequest.setRequestedVacationDays(1.0);
    }

    @Test
    @DisplayName("유효한 요청 데이터는 검증을 통과해야 한다")
    void testValidRequest() {
        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("requestDate가 null이면 검증에 실패해야 한다")
    void testRequestDateNotNull() {
        // given
        validRequest.setRequestDate(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("requestDate") &&
            v.getMessage().equals("신청일자는 필수입니다.")
        );
    }

    @Test
    @DisplayName("department가 null이면 검증에 실패해야 한다")
    void testDepartmentNotNull() {
        // given
        validRequest.setDepartment(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("department") &&
            v.getMessage().equals("소속은 필수입니다.")
        );
    }

    @Test
    @DisplayName("applicant가 null이면 검증에 실패해야 한다")
    void testApplicantNotNull() {
        // given
        validRequest.setApplicant(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("applicant") &&
            v.getMessage().equals("신청자는 필수입니다.")
        );
    }

    @Test
    @DisplayName("startDate가 null이면 검증에 실패해야 한다")
    void testStartDateNotNull() {
        // given
        validRequest.setStartDate(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("startDate") &&
            v.getMessage().equals("시작일은 필수입니다.")
        );
    }

    @Test
    @DisplayName("endDate가 null이면 검증에 실패해야 한다")
    void testEndDateNotNull() {
        // given
        validRequest.setEndDate(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("endDate") &&
            v.getMessage().equals("종료일은 필수입니다.")
        );
    }

    @Test
    @DisplayName("vacationType이 null이면 검증에 실패해야 한다")
    void testVacationTypeNotNull() {
        // given
        validRequest.setVacationType(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("vacationType") &&
            v.getMessage().equals("휴가 구분은 필수입니다.")
        );
    }

    @Test
    @DisplayName("totalVacationDays가 null이면 검증에 실패해야 한다")
    void testTotalVacationDaysNotNull() {
        // given
        validRequest.setTotalVacationDays(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("totalVacationDays") &&
            v.getMessage().equals("총 연차일수는 필수입니다.")
        );
    }

    @Test
    @DisplayName("remainingVacationDays가 null이면 검증에 실패해야 한다")
    void testRemainingVacationDaysNotNull() {
        // given
        validRequest.setRemainingVacationDays(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("remainingVacationDays") &&
            v.getMessage().equals("잔여 연차일수는 필수입니다.")
        );
    }

    @Test
    @DisplayName("requestedVacationDays가 null이면 검증에 실패해야 한다")
    void testRequestedVacationDaysNotNull() {
        // given
        validRequest.setRequestedVacationDays(null);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("requestedVacationDays") &&
            v.getMessage().equals("신청 연차일수는 필수입니다.")
        );
    }

    @Test
    @DisplayName("시작일이 종료일보다 이후이면 검증에 실패해야 한다")
    void testStartDateAfterEndDate() {
        // given
        validRequest.setStartDate(LocalDate.of(2025, 9, 10));
        validRequest.setEndDate(LocalDate.of(2025, 9, 5));

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("startDate") &&
            v.getMessage().equals("시작일은 종료일보다 이후일 수 없습니다.")
        );
    }

    @Test
    @DisplayName("시작일이 신청일보다 이후이면 검증에 실패해야 한다")
    void testStartDateAfterRequestDate() {
        // given
        validRequest.setRequestDate(LocalDate.of(2025, 9, 1));
        validRequest.setStartDate(LocalDate.of(2025, 9, 5));
        validRequest.setEndDate(LocalDate.of(2025, 9, 5));

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("startDate") &&
            v.getMessage().equals("휴가 기간은 신청일보다 이후일 수 없습니다.")
        );
    }

    @Test
    @DisplayName("종료일이 신청일보다 이후이면 검증에 실패해야 한다")
    void testEndDateAfterRequestDate() {
        // given
        validRequest.setRequestDate(LocalDate.of(2025, 9, 1));
        validRequest.setStartDate(LocalDate.of(2025, 9, 1));
        validRequest.setEndDate(LocalDate.of(2025, 9, 5));

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("startDate") &&
            v.getMessage().equals("휴가 기간은 신청일보다 이후일 수 없습니다.")
        );
    }

    @Test
    @DisplayName("신청일과 같은 날짜의 휴가는 유효해야 한다")
    void testSameDateAsRequestDate() {
        // given
        LocalDate sameDate = LocalDate.of(2025, 9, 5);
        validRequest.setRequestDate(sameDate);
        validRequest.setStartDate(sameDate);
        validRequest.setEndDate(sameDate);

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("신청일보다 이전 날짜의 휴가는 유효해야 한다")
    void testBeforeRequestDate() {
        // given
        validRequest.setRequestDate(LocalDate.of(2025, 9, 10));
        validRequest.setStartDate(LocalDate.of(2025, 9, 5));
        validRequest.setEndDate(LocalDate.of(2025, 9, 5));

        // when
        Set<ConstraintViolation<VacationSampleRequest>> violations = validator.validate(validRequest);

        // then
        assertThat(violations).isEmpty();
    }
}

