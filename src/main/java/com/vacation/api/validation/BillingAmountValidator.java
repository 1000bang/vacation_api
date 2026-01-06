package com.vacation.api.validation;

import com.vacation.api.domain.sample.request.RentalSupportPropSampleRequest;
import com.vacation.api.domain.sample.request.RentalSupportSampleRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 월세 청구 금액 검증 어노테이션
 * - 월세 청구 금액은 계약 월세 금액의 50% 이하
 * - 최대 25만원까지 가능
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-01-05
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@jakarta.validation.Constraint(validatedBy = BillingAmountValidator.BillingAmountConstraintValidator.class)
public @interface BillingAmountValidator {
    String message() default "월세 청구 금액이 유효하지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};

    class BillingAmountConstraintValidator implements ConstraintValidator<BillingAmountValidator, Object> {
        
        private static final long MAX_BILLING_AMOUNT = 250000L; // 최대 25만원

        @Override
        public void initialize(BillingAmountValidator constraintAnnotation) {
            // 초기화 로직 없음
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }

            Long contractMonthlyRent = null;
            Long billingAmount = null;

            // RentalSupportPropSampleRequest 또는 RentalSupportSampleRequest인지 확인
            if (value instanceof RentalSupportPropSampleRequest) {
                RentalSupportPropSampleRequest request = (RentalSupportPropSampleRequest) value;
                contractMonthlyRent = request.getContractMonthlyRent();
                billingAmount = request.getBillingAmount();
            } else if (value instanceof RentalSupportSampleRequest) {
                RentalSupportSampleRequest request = (RentalSupportSampleRequest) value;
                contractMonthlyRent = request.getContractMonthlyRent();
                billingAmount = request.getBillingAmount();
            } else {
                return true; // 다른 타입은 검증하지 않음
            }

            // null 체크는 @NotNull로 처리
            if (contractMonthlyRent == null || billingAmount == null) {
                return true;
            }

            // 1. 월세 청구 금액은 계약 월세 금액의 50% 이하여야 함
            long maxBillingAmount = contractMonthlyRent / 2;
            if (billingAmount > maxBillingAmount) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("월세 청구 금액은 계약 월세 금액의 50%%(%,d원) 이하여야 합니다.", maxBillingAmount))
                        .addPropertyNode("billingAmount")
                        .addConstraintViolation();
                return false;
            }

            // 2. 최대 25만원까지 가능
            if (billingAmount > MAX_BILLING_AMOUNT) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("월세 청구 금액은 최대 %,d원까지 가능합니다.", MAX_BILLING_AMOUNT))
                        .addPropertyNode("billingAmount")
                        .addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}

