package com.vacation.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

/**
 * 날짜 범위 검증 어노테이션
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@jakarta.validation.Constraint(validatedBy = DateRangeValidator.DateRangeConstraintValidator.class)
public @interface DateRangeValidator {
    String message() default "날짜 범위가 유효하지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};

    class DateRangeConstraintValidator implements ConstraintValidator<DateRangeValidator, Object> {
        @Override
        public void initialize(DateRangeValidator constraintAnnotation) {
            // 초기화 로직 없음
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }

            try {
                if (!(value instanceof com.vacation.api.domain.vacation.request.VacationSampleRequest)) {
                    return true;
                }

                com.vacation.api.domain.vacation.request.VacationSampleRequest request = 
                    (com.vacation.api.domain.vacation.request.VacationSampleRequest) value;

                LocalDate startDate = request.getStartDate();
                LocalDate endDate = request.getEndDate();
                LocalDate referenceDate = request.getRequestDate();

                if (startDate == null || endDate == null || referenceDate == null) {
                    return true; // @NotNull로 처리
                }

                // 시작일이 종료일보다 이후일 수 없음
                if (startDate.isAfter(endDate)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("시작일은 종료일보다 이후일 수 없습니다.")
                            .addPropertyNode("startDate")
                            .addConstraintViolation();
                    return false;
                }

                // 시작일과 종료일이 신청일보다 이후일 수 없음
                if (startDate.isAfter(referenceDate) || endDate.isAfter(referenceDate)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("휴가 기간은 신청일보다 이후일 수 없습니다.")
                            .addPropertyNode("startDate")
                            .addConstraintViolation();
                    return false;
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

