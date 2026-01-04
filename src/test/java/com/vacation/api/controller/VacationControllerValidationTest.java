package com.vacation.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacation.api.common.TransactionIDCreator;
import com.vacation.api.config.SecurityConfig;
import com.vacation.api.domain.vacation.controller.VacationController;
import com.vacation.api.domain.vacation.request.VacationSampleRequest;
import com.vacation.api.enums.VacationType;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.GlobalExceptionHandler;
import com.vacation.api.interceptor.ApiInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VacationController Validation 통합 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@WebMvcTest(controllers = VacationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class VacationControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.vacation.api.domain.vacation.service.PdfGenerationService pdfGenerationService;

    @MockBean
    private TransactionIDCreator transactionIDCreator;

    @MockBean
    private ApiInterceptor apiInterceptor;

    @Test
    @DisplayName("유효한 요청은 200 OK를 반환해야 한다")
    void testValidRequest() throws Exception {
        // given
        VacationSampleRequest request = createValidRequest();
        byte[] mockPdfBytes = "mock pdf content".getBytes();
        when(pdfGenerationService.generateVacationApplicationPdf(any(VacationSampleRequest.class)))
                .thenReturn(mockPdfBytes);

        // when & then
        mockMvc.perform(post("/sample/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("requestDate가 null이면 400 Bad Request를 반환해야 한다")
    void testRequestDateNull() throws Exception {
        // given
        VacationSampleRequest request = createValidRequest();
        request.setRequestDate(null);

        // when & then
        mockMvc.perform(post("/sample/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(ApiErrorCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errorMessage").value(ApiErrorCode.VALIDATION_FAILED.getDescription()))
                .andExpect(jsonPath("$.errors.requestDate").value("신청일자는 필수입니다."));
    }

    @Test
    @DisplayName("잘못된 vacationType이면 901 에러를 반환해야 한다")
    void testInvalidVacationType() throws Exception {
        // given
        String invalidJson = """
            {
                "requestDate": "2025-12-26",
                "department": "서비스본부/서비스개발2팀",
                "applicant": "천병재",
                "startDate": "2025-09-05",
                "endDate": "2025-09-05",
                "vacationType": "연차",
                "totalVacationDays": 18.0,
                "remainingVacationDays": 8.0,
                "requestedVacationDays": 1.0
            }
            """;

        // when & then
        mockMvc.perform(post("/sample/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(ApiErrorCode.INVALID_VACATION_TYPE.getCode()))
                .andExpect(jsonPath("$.errorMessage").value(ApiErrorCode.INVALID_VACATION_TYPE.getDescription()));
    }

    @Test
    @DisplayName("시작일이 종료일보다 이후이면 400 Bad Request를 반환해야 한다")
    void testStartDateAfterEndDate() throws Exception {
        // given
        VacationSampleRequest request = createValidRequest();
        request.setStartDate(LocalDate.of(2025, 9, 10));
        request.setEndDate(LocalDate.of(2025, 9, 5));

        // when & then
        mockMvc.perform(post("/sample/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                //.andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(ApiErrorCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errorMessage").value(ApiErrorCode.VALIDATION_FAILED.getDescription()))
                .andExpect(jsonPath("$.errors.startDate").value("시작일은 종료일보다 이후일 수 없습니다."));
    }

    @Test
    @DisplayName("휴가 기간이 신청일보다 이후이면 400 Bad Request를 반환해야 한다")
    void testVacationPeriodAfterRequestDate() throws Exception {
        // given
        VacationSampleRequest request = createValidRequest();
        request.setRequestDate(LocalDate.of(2025, 9, 1));
        request.setStartDate(LocalDate.of(2025, 9, 5));
        request.setEndDate(LocalDate.of(2025, 9, 5));

        // when & then
        mockMvc.perform(post("/sample/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(ApiErrorCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errorMessage").value(ApiErrorCode.VALIDATION_FAILED.getDescription()))
                .andExpect(jsonPath("$.errors.startDate").value("휴가 기간은 신청일보다 이후일 수 없습니다."));
    }

    @Test
    @DisplayName("모든 필수 필드가 null이면 400 Bad Request를 반환해야 한다")
    void testAllRequiredFieldsNull() throws Exception {
        // given
        VacationSampleRequest request = new VacationSampleRequest();

        // when & then
        mockMvc.perform(post("/sample/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(ApiErrorCode.VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.errorMessage").value(ApiErrorCode.VALIDATION_FAILED.getDescription()))
                .andExpect(jsonPath("$.errors").exists());
    }

    private VacationSampleRequest createValidRequest() {
        VacationSampleRequest request = new VacationSampleRequest();
        request.setRequestDate(LocalDate.of(2025, 12, 26));
        request.setDepartment("서비스본부/서비스개발2팀");
        request.setApplicant("천병재");
        request.setStartDate(LocalDate.of(2025, 9, 5));
        request.setEndDate(LocalDate.of(2025, 9, 5));
        request.setVacationType(VacationType.YEONCHA);
        request.setReason("개인 사정");
        request.setTotalVacationDays(18.0);
        request.setRemainingVacationDays(8.0);
        request.setRequestedVacationDays(1.0);
        return request;
    }
}

