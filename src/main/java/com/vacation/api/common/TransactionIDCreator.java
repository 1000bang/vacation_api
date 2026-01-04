package com.vacation.api.common;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 트랜잭션 ID를 생성하는 유틸리티 클래스
 *
 * @author vacation-api
 * @version 1.0
 * @since 2025-12-26
 */
@Component
public class TransactionIDCreator {

    public String createTransactionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return timestamp + "-" + uuid;
    }
}

