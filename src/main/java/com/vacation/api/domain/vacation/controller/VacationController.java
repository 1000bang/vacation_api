package com.vacation.api.domain.vacation.controller;

import com.vacation.api.domain.sample.request.VacationSampleRequest;
import com.vacation.api.domain.sample.service.PdfGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 연차 신청 및 관리와 관련된 요청을 처리하는 controller
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Slf4j
@RestController
public class VacationController {
    Logger logger = LoggerFactory.getLogger(VacationController.class);




}

