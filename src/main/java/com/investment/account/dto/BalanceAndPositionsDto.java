package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 계좌 잔고와 보유 종목을 한 번에 담는 DTO.
 * 주식잔고조회 API 1회 호출 결과를 그대로 전달할 때 사용한다.
 * Redis 캐시 역직렬화를 위해 @NoArgsConstructor(force = true) 필요.
 */
@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class BalanceAndPositionsDto {

    private final AccountBalanceDto balance;
    private final List<AccountPositionDto> positions;
}
