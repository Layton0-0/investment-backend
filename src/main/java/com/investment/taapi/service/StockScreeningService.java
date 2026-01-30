package com.investment.taapi.service;

import com.investment.taapi.dto.StockAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주식 스크리닝 서비스
 * 여러 종목을 분석하여 매매 후보를 선정합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockScreeningService {
    
    private final StockAnalysisService stockAnalysisService;
    
    // 분석할 종목 목록 (국내 주식 종목 코드)
    // 한국투자증권 API는 국내 주식(코스피/코스닥) 데이터를 제공
    private static final List<String> WATCH_LIST = List.of(
            "005930", // 삼성전자
            "000660", // SK하이닉스
            "035420", // NAVER
            "035720", // 카카오
            "051910"  // LG화학
    );
    
    /**
     * 매매 후보 종목 선정
     * 
     * @param interval 시간 간격
     * @param limit 상위 N개 종목
     * @return 분석된 종목 목록 (점수 순으로 정렬)
     */
    public Mono<List<StockAnalysisDto>> screenStocks(String interval, int limit) {
        log.info("주식 스크리닝 시작: 종목 수={}, limit={}", WATCH_LIST.size(), limit);
        
        // 한국투자증권 API Rate limit 준수를 위해 순차 처리 (concatMap 사용)
        return Flux.fromIterable(WATCH_LIST)
                .concatMap(symbol -> stockAnalysisService.analyzeStock(symbol, interval)
                        .map(analysis -> {
                            // RSI 미반환 시 50으로 보정 (차트 데이터 부족/API 오류 시에도 점수 계산 가능)
                            if (analysis.getRsi() == null && (analysis.getEma20() != null || analysis.getCurrentPrice() != null)) {
                                analysis.setRsi(new BigDecimal("50"));
                                log.debug("종목 RSI 미반환, 50으로 보정: symbol={}", analysis.getSymbol());
                            }
                            BigDecimal score = calculateScore(analysis);
                            analysis.setExpectedReturn(score);
                            return analysis;
                        })
                        .onErrorResume(error -> {
                            log.warn("종목 분석 실패: symbol={}, error={}", symbol, error.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .map(analyses -> {
                    // 점수 순으로 정렬하고 상위 N개 선택 (점수가 있는 것만)
                    List<StockAnalysisDto> filtered = analyses.stream()
                            .filter(a -> a.getExpectedReturn() != null)
                            .sorted(Comparator.comparing(StockAnalysisDto::getExpectedReturn,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .limit(limit)
                            .collect(Collectors.toList());
                    
                    if (filtered.isEmpty() && !analyses.isEmpty()) {
                        log.warn("선정된 종목이 없습니다. API 키·인증·네트워크 또는 차트 데이터 부족을 확인하세요.");
                    }
                    
                    return filtered;
                })
                .doOnSuccess(result -> {
                    if (result.isEmpty()) {
                        log.warn("주식 스크리닝 완료: 선정된 종목이 없습니다. 모의 데이터를 사용하세요.");
                    } else {
                        log.info("주식 스크리닝 완료: 선정된 종목 수={}", result.size());
                    }
                });
    }
    
    /**
     * 종목 점수 계산
     * 기술적 지표를 기반으로 매매 적합도를 점수화합니다.
     */
    private BigDecimal calculateScore(StockAnalysisDto analysis) {
        BigDecimal score = BigDecimal.ZERO;
        
        // RSI 점수 (30-70 구간이 이상적)
        if (analysis.getRsi() != null) {
            BigDecimal rsi = analysis.getRsi();
            if (rsi.compareTo(new BigDecimal("30")) >= 0 && rsi.compareTo(new BigDecimal("70")) <= 0) {
                // 50에 가까울수록 높은 점수
                BigDecimal rsiScore = new BigDecimal("100")
                        .subtract(rsi.subtract(new BigDecimal("50")).abs().multiply(new BigDecimal("2")));
                score = score.add(rsiScore.multiply(new BigDecimal("0.3")));
            }
        }
        
        // MACD 점수 (히스토그램이 양수이고 증가 추세)
        if (analysis.getMacdHist() != null && analysis.getMacdHist().compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(new BigDecimal("30"));
        }
        
        // 골든크로스 점수
        if (analysis.isGoldenCross()) {
            score = score.add(new BigDecimal("20"));
        }
        
        // 돌파 점수
        if (analysis.isBreakout()) {
            score = score.add(new BigDecimal("20"));
        }
        
        return score.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 진입가, 목표가, 손절가 계산
     */
    public void calculatePriceTargets(StockAnalysisDto analysis) {
        if (analysis.getCurrentPrice() == null) {
            return;
        }
        
        BigDecimal currentPrice = analysis.getCurrentPrice();
        
        // 진입가: 현재가 ± 1%
        BigDecimal entryMin = currentPrice.multiply(new BigDecimal("0.99"));
        BigDecimal entryMax = currentPrice.multiply(new BigDecimal("1.01"));
        
        // 손절가: ATR 기반 또는 현재가의 3% 하락
        BigDecimal stopLoss;
        if (analysis.getAtr() != null && analysis.getAtr().compareTo(BigDecimal.ZERO) > 0) {
            stopLoss = currentPrice.subtract(analysis.getAtr().multiply(new BigDecimal("2")));
        } else {
            stopLoss = currentPrice.multiply(new BigDecimal("0.97"));
        }
        
        // 목표가 1: R:R 2:1 기준
        BigDecimal risk = currentPrice.subtract(stopLoss);
        BigDecimal target1 = currentPrice.add(risk.multiply(new BigDecimal("2")));
        
        // 목표가 2: R:R 3:1 기준
        BigDecimal target2 = currentPrice.add(risk.multiply(new BigDecimal("3")));
        
        // 리스크/리워드 비율 계산
        BigDecimal reward = target1.subtract(currentPrice);
        BigDecimal riskRewardRatio = risk.compareTo(BigDecimal.ZERO) > 0 ?
                reward.divide(risk, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // 예상 수익률 계산
        BigDecimal expectedReturn = reward.divide(currentPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        // DTO에 설정 (StockAnalysisDto에 필드 추가 필요)
        // 여기서는 계산만 수행하고, 실제로는 별도 메서드로 반환하거나 DTO 확장 필요
    }
}
