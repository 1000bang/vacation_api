package com.vacation.api.domain.expense.service;

import com.vacation.api.domain.expense.entity.ExpenseClaim;
import com.vacation.api.domain.expense.entity.ExpenseSub;
import com.vacation.api.domain.expense.repository.ExpenseClaimRepository;
import com.vacation.api.domain.expense.repository.ExpenseSubRepository;
import com.vacation.api.domain.expense.request.ExpenseClaimRequest;
import com.vacation.api.domain.expense.request.ExpenseItemRequest;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExpenseClaimService 테스트
 * DB를 통한 통합 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseClaimServiceTest {

    @Autowired
    private ExpenseClaimService expenseClaimService;

    @Autowired
    private ExpenseClaimRepository expenseClaimRepository;

    @Autowired
    private ExpenseSubRepository expenseSubRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("test@expense.com")
                .name("테스트 사용자")
                .password("encoded_password")
                .division("서비스사업본부")
                .team("서비스1팀")
                .position("주임")
                .status(UserStatus.APPROVED)
                .authVal("tw")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("비용 청구 생성 성공 - 유효한 요청으로 비용 청구가 생성되어야 한다")
    void testCreateExpenseClaim_Success() {
        // given
        ExpenseClaimRequest request = new ExpenseClaimRequest();
        request.setRequestDate(LocalDate.of(2026, 1, 15));
        request.setMonth(12); // 12월 청구

        List<ExpenseItemRequest> items = new ArrayList<>();
        ExpenseItemRequest item1 = new ExpenseItemRequest();
        item1.setDate(LocalDate.of(2025, 12, 10));
        item1.setUsageDetail("택시비");
        item1.setVendor("카카오택시");
        item1.setPaymentMethod("카드");
        item1.setProject("프로젝트A");
        item1.setAmount(10000L);
        item1.setNote("출장");
        items.add(item1);

        ExpenseItemRequest item2 = new ExpenseItemRequest();
        item2.setDate(LocalDate.of(2025, 12, 15));
        item2.setUsageDetail("식대");
        item2.setVendor("맛집");
        item2.setPaymentMethod("현금");
        item2.setProject("프로젝트A");
        item2.setAmount(50000L);
        items.add(item2);

        request.setExpenseItems(items);

        // when
        ExpenseClaim result = expenseClaimService.createExpenseClaim(testUser.getUserId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(result.getTotalAmount()).isEqualTo(60000L);
        assertThat(result.getChildCnt()).isEqualTo(2);
        assertThat(result.getBillingYyMonth()).isEqualTo(202512); // requestDate가 1월이고 month가 12이면 전년도

        // 자식 항목 확인
        List<ExpenseSub> subs = expenseSubRepository.findByParentSeqOrderByChildNoAsc(result.getSeq());
        assertThat(subs).hasSize(2);
        assertThat(subs.get(0).getAmount()).isEqualTo(10000L);
        assertThat(subs.get(1).getAmount()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("비용 청구 수정 성공 - 기존 청구를 수정할 수 있어야 한다")
    void testUpdateExpenseClaim_Success() {
        // given - 기존 청구 생성
        ExpenseClaimRequest createRequest = new ExpenseClaimRequest();
        createRequest.setRequestDate(LocalDate.of(2026, 1, 15));
        createRequest.setMonth(1);

        List<ExpenseItemRequest> items = new ArrayList<>();
        ExpenseItemRequest item = new ExpenseItemRequest();
        item.setDate(LocalDate.of(2026, 1, 10));
        item.setUsageDetail("택시비");
        item.setVendor("카카오택시");
        item.setPaymentMethod("카드");
        item.setProject("프로젝트A");
        item.setAmount(10000L);
        items.add(item);
        createRequest.setExpenseItems(items);

        ExpenseClaim created = expenseClaimService.createExpenseClaim(testUser.getUserId(), createRequest);

        // when - 수정 요청
        ExpenseClaimRequest updateRequest = new ExpenseClaimRequest();
        updateRequest.setRequestDate(LocalDate.of(2026, 1, 20));
        updateRequest.setMonth(1);

        List<ExpenseItemRequest> updateItems = new ArrayList<>();
        ExpenseItemRequest updateItem = new ExpenseItemRequest();
        updateItem.setDate(LocalDate.of(2026, 1, 15));
        updateItem.setUsageDetail("식대");
        updateItem.setVendor("맛집");
        updateItem.setPaymentMethod("현금");
        updateItem.setProject("프로젝트B");
        updateItem.setAmount(50000L);
        updateItems.add(updateItem);
        updateRequest.setExpenseItems(updateItems);

        ExpenseClaim result = expenseClaimService.updateExpenseClaim(created.getSeq(), testUser.getUserId(), updateRequest);

        // then
        assertThat(result.getTotalAmount()).isEqualTo(50000L);
        assertThat(result.getChildCnt()).isEqualTo(1);

        // 기존 자식 항목이 삭제되고 새로운 항목이 생성되었는지 확인
        List<ExpenseSub> subs = expenseSubRepository.findByParentSeqOrderByChildNoAsc(result.getSeq());
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).getUsageDetail()).isEqualTo("식대");
    }

    @Test
    @DisplayName("비용 청구 삭제 성공 - 청구와 자식 항목이 모두 삭제되어야 한다")
    void testDeleteExpenseClaim_Success() {
        // given
        ExpenseClaimRequest request = new ExpenseClaimRequest();
        request.setRequestDate(LocalDate.of(2026, 1, 15));
        request.setMonth(1);

        List<ExpenseItemRequest> items = new ArrayList<>();
        ExpenseItemRequest item = new ExpenseItemRequest();
        item.setDate(LocalDate.of(2026, 1, 10));
        item.setUsageDetail("택시비");
        item.setVendor("카카오택시");
        item.setPaymentMethod("카드");
        item.setProject("프로젝트A");
        item.setAmount(10000L);
        items.add(item);
        request.setExpenseItems(items);

        ExpenseClaim created = expenseClaimService.createExpenseClaim(testUser.getUserId(), request);
        Long seq = created.getSeq();

        // when
        expenseClaimService.deleteExpenseClaim(seq, testUser.getUserId());

        // then
        assertThat(expenseClaimRepository.findBySeqAndUserId(seq, testUser.getUserId())).isEmpty();
        assertThat(expenseSubRepository.findByParentSeqOrderByChildNoAsc(seq)).isEmpty();
    }

    @Test
    @DisplayName("비용 청구 삭제 실패 - 존재하지 않는 청구 삭제 시 예외가 발생해야 한다")
    void testDeleteExpenseClaim_WhenNotExists_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> expenseClaimService.deleteExpenseClaim(999L, testUser.getUserId()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.INVALID_LOGIN);
                });
    }
}
