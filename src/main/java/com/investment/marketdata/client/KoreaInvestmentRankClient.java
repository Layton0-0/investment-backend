package com.investment.marketdata.client;

import com.investment.marketdata.dto.InvestorDailyByMarketItemDto;
import com.investment.marketdata.dto.VolumeRankItemDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 한국투자증권 순위분석·투자자 매매동향 API 클라이언트 인터페이스.
 * 퀀트 스코어링(주도주 유니버스·수급 점수)용.
 *
 * @see com.investment.marketdata.client.impl.KoreaInvestmentRankClientImpl
 * @see <a href="https://github.com/koreainvestment/open-trading-api">한국투자증권 open-trading-api</a>
 *      MCP search_domestic_stock_api(순위분석, 시세분석)로 path·TR_ID 확인 후 구현.
 */
public interface KoreaInvestmentRankClient {

    /**
     * 거래량 순위 조회 (주도주 유니버스 갱신용).
     * MCP: volume_rank (거래량순위). path·TR_ID·파라미터는 MCP read_source_code로 확인.
     *
     * @param userId     API 토큰 발급에 사용할 사용자 ID
     * @param serverType "1" 모의, "0" 실전
     * @param marketDiv  시장 구분 (예: J=주식)
     * @param limit      상위 N건
     * @return 순위 목록 (없으면 빈 리스트)
     */
    List<VolumeRankItemDto> getVolumeRank(String userId, String serverType, String marketDiv, int limit);

    /**
     * 시장별 투자자 매매동향(일별) 조회 (수급 점수 반영용).
     * MCP: inquire_investor_daily_by_market. path·TR_ID·파라미터는 MCP read_source_code로 확인.
     *
     * @param userId     API 토큰 발급에 사용할 사용자 ID
     * @param serverType "1" 모의, "0" 실전
     * @param fromDate   조회 시작일
     * @param toDate     조회 종료일
     * @return 일별 투자자 매매동향 목록 (없으면 빈 리스트)
     */
    List<InvestorDailyByMarketItemDto> getInvestorDailyByMarket(String userId, String serverType,
                                                               LocalDate fromDate, LocalDate toDate);
}
