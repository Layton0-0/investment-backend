package com.investment.account.service;

import com.investment.account.client.KoreaInvestmentAccountClient;
import com.investment.account.dto.*;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.CacheConfig;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.Portfolio;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.PortfolioRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 계좌 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final PortfolioRepository portfolioRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final UserAccountRepository userAccountRepository;
    private final EncryptionUtil encryptionUtil;
    private final KoreaInvestmentAccountClient accountClient;
    private final KoreaInvestmentTokenService tokenService;
    private final AccountApiRunner accountApiRunner;

    /**
     * 계좌 잔고 조회
     * 한국투자증권 API를 사용하여 실시간 잔고 정보를 조회합니다.
     * API 실패 시 DB 폴백을 사용합니다.
     * 
     * @param accountNo 계좌번호
     * @return 계좌 잔고 정보
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'balance_' + #accountNo")
    public AccountBalanceDto getAccountBalance(String accountNo) {
        log.debug("계좌 잔고 조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));

        try {
            // 현재 사용자 ID 가져오기
            String userId = getCurrentUserId();

            // 한국투자증권 API 사용 시도 (별도 트랜잭션에서 실행해 rollback-only 오염 방지)
            try {
                KoreaInvestmentAccountClient.BalanceAndPositionsResult result = accountApiRunner.inquireBalanceInNewTx(userId,
                        accountNo);
                return result.getBalance();
            } catch (Exception apiException) {
                log.warn("한국투자증권 API 호출 실패, DB 폴백 사용: accountNo={}, error={}",
                        LogMaskingUtil.maskAccountNo(accountNo), apiException.getMessage());
                // API 실패 시 DB 폴백
                return getAccountBalanceFromDb(accountNo);
            }
        } catch (IllegalStateException e) {
            // 인증되지 않은 사용자 또는 userId를 찾을 수 없는 경우 DB 폴백
            log.debug("사용자 ID를 찾을 수 없음, DB 폴백 사용: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            return getAccountBalanceFromDb(accountNo);
        } catch (Exception e) {
            log.error("계좌 잔고 조회 실패: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "계좌 잔고 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * DB에서 계좌 잔고 조회 (폴백용)
     */
    private AccountBalanceDto getAccountBalanceFromDb(String accountNo) {
        BigDecimal portfolioValue = portfolioRepository.getTotalPortfolioValue(accountNo);
        if (portfolioValue == null) {
            portfolioValue = BigDecimal.ZERO;
        }

        return AccountBalanceDto.builder()
                .accountNo(accountNo)
                .totalBalance(portfolioValue)
                .availableBalance(portfolioValue)
                .investedAmount(portfolioValue)
                .currency("KRW")
                .deposit(portfolioValue)
                .orderableCash(portfolioValue)
                .totalAssetValue(portfolioValue)
                .totalProfitLoss(BigDecimal.ZERO)
                .totalProfitLossRate(BigDecimal.ZERO)
                .build();
    }

    /**
     * 현재 사용자 ID 가져오기
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }

    /**
     * 서버 타입 정규화: "0"(실거래), "1"(모의투자)만 허용, 그 외는 "1" 반환.
     * null·공백·쿼리 파라미터 오염 방지.
     */
    private static String normalizeServerType(String serverType) {
        if (serverType == null) {
            return "1";
        }
        String trimmed = serverType.trim();
        return "0".equals(trimmed) ? "0" : "1";
    }

    /**
     * 사용자 계좌 목록 조회 (첫 번째 계좌 자동 선택용)
     * 
     * @deprecated 사용자별 계좌 조회는 getUserAccountNo(String userId)를 사용하세요.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public String getDefaultAccountNo() {
        log.debug("기본 계좌번호 조회");

        List<String> accountNos = portfolioRepository.findDistinctAccountNos();
        if (accountNos == null || accountNos.isEmpty()) {
            return null;
        }

        // 첫 번째 계좌 반환
        return accountNos.get(0);
    }

    /**
     * 사용자 계좌 조회 (메인 계좌 또는 첫 번째 계좌)
     * 서버 타입 미지정 시 모의투자("1") 계좌를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 계좌번호 (계좌가 없으면 null)
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    public String getUserAccountNo(String userId) {
        return getUserAccountNo(userId, "1");
    }

    /**
     * 사용자 계좌 조회 (서버 타입별 메인 계좌 또는 첫 번째 계좌)
     *
     * @param userId     사용자 ID
     * @param serverType 서버 타입 ("1": 모의투자, "0": 실거래)
     * @return 계좌번호 (계좌가 없으면 null)
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    public String getUserAccountNo(String userId, String serverType) {
        String st = normalizeServerType(serverType);
        log.debug("사용자 계좌 조회: userId={}, serverType={}", userId, st);

        try {
            UserAccount mainAccount = userAccountRepository.findByUserIdAndServerTypeAndIsDefaultTrue(userId, st)
                    .orElse(null);

            if (mainAccount == null) {
                List<UserAccount> accounts = userAccountRepository.findByUserIdAndServerTypeAndIsActiveTrue(userId, st);
                if (!accounts.isEmpty()) {
                    mainAccount = accounts.get(0);
                }
            }

            if (mainAccount != null) {
                String accountNo = encryptionUtil.decrypt(mainAccount.getAccountNoEncrypted());
                log.debug("UserAccount에서 계좌번호 조회 성공: userId={}, accountId={}, serverType={}",
                        userId, mainAccount.getId(), st);
                return accountNo;
            }

            log.debug("UserAccount에 계좌가 없음, 기존 방식 사용: userId={}", userId);
            return getDefaultAccountNoFallback();

        } catch (Exception e) {
            log.error("사용자 계좌 조회 실패: userId={}, serverType={}", userId, st, e);
            return getDefaultAccountNoFallback();
        }
    }

    /**
     * 기본 계좌번호 조회 (폴백용, 별도 트랜잭션)
     * getUserAccountNo()에서 예외 발생 시 트랜잭션 롤백을 방지하기 위해 별도 메서드로 분리
     */
    @Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private String getDefaultAccountNoFallback() {
        log.debug("기본 계좌번호 조회 (폴백)");

        List<String> accountNos = portfolioRepository.findDistinctAccountNos();
        if (accountNos == null || accountNos.isEmpty()) {
            return null;
        }

        // 첫 번째 계좌 반환
        return accountNos.get(0);
    }

    /**
     * 보유 종목 조회 (전체: 국내+해외 병합). 캐시는 getPositions(accountNo, null)에서 "all" 키로 적용.
     * @see #getPositions(String, String)
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    public List<AccountPositionDto> getPositions(String accountNo) {
        return getPositions(accountNo, null);
    }

    /**
     * 보유 종목 조회 (시장별 별도 조회 지원).
     * market=KR: 국내만, market=US: 해외만, null/빈값: 국내+해외 병합.
     *
     * @param accountNo 계좌번호
     * @param market    시장 구분 (KR, US, null 또는 빈 문자열이면 전체)
     * @return 보유 종목 목록
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'positions_' + #accountNo + '_' + (T(org.springframework.util.StringUtils).hasText(#market) ? #market : 'all')")
    public List<AccountPositionDto> getPositions(String accountNo, String market) {
        log.debug("보유 종목 조회: accountNo={}, market={}", LogMaskingUtil.maskAccountNo(accountNo), market);

        try {
            String userId = getCurrentUserId();
            boolean krOnly = "KR".equalsIgnoreCase(market);
            boolean usOnly = "US".equalsIgnoreCase(market);

            try {
                if (usOnly) {
                    KoreaInvestmentAccountClient.OverseasBalanceResult overseas = getOverseasBalanceResult(accountNo);
                    return overseas != null && overseas.getPositions() != null ? overseas.getPositions() : new ArrayList<>();
                }
                KoreaInvestmentAccountClient.BalanceAndPositionsResult result = accountApiRunner.inquireBalanceInNewTx(userId, accountNo);
                List<AccountPositionDto> domestic = result.getPositions();
                if (krOnly) {
                    return domestic != null ? domestic : new ArrayList<>();
                }
                // 전체: 국내 + 해외 (해외는 캐시된 getOverseasBalanceResult 사용)
                List<AccountPositionDto> all = new ArrayList<>(domestic != null ? domestic : List.of());
                KoreaInvestmentAccountClient.OverseasBalanceResult overseasResult = getOverseasBalanceResult(accountNo);
                List<AccountPositionDto> overseasPositions = overseasResult != null ? overseasResult.getPositions() : null;
                if (overseasPositions != null && !overseasPositions.isEmpty()) {
                    all.addAll(overseasPositions);
                }
                return all;
            } catch (Exception apiException) {
                log.warn("한국투자증권 API 호출 실패, DB 폴백 사용: accountNo={}, market={}, error={}",
                        LogMaskingUtil.maskAccountNo(accountNo), market, apiException.getMessage());
                List<AccountPositionDto> fromDb = getPositionsFromDb(accountNo);
                if (fromDb == null) {
                    return new ArrayList<>();
                }
                if (krOnly || usOnly) {
                    String m = krOnly ? "KR" : "US";
                    fromDb = fromDb.stream().filter(p -> m.equalsIgnoreCase(p.getMarket())).toList();
                }
                return fromDb;
            }
        } catch (IllegalStateException e) {
            log.debug("사용자 ID를 찾을 수 없음, DB 폴백 사용: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            try {
                List<AccountPositionDto> fromDb = getPositionsFromDb(accountNo);
                if (fromDb == null) return new ArrayList<>();
                if ("KR".equalsIgnoreCase(market)) {
                    return fromDb.stream().filter(p -> "KR".equalsIgnoreCase(p.getMarket())).toList();
                }
                if ("US".equalsIgnoreCase(market)) {
                    return fromDb.stream().filter(p -> "US".equalsIgnoreCase(p.getMarket())).toList();
                }
                return fromDb;
            } catch (Exception dbEx) {
                log.warn("DB 폴백 보유 종목 조회 실패, 빈 목록 반환: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo), dbEx);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("보유 종목 조회 실패: accountNo={}, market={}", LogMaskingUtil.maskAccountNo(accountNo), market, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "보유 종목 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 해외(미국) 잔고 조회 결과(보유종목+output2 요약) 캐시. getPositions(US)·getOverseasSummary에서 공유.
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'overseas_' + #accountNo")
    public KoreaInvestmentAccountClient.OverseasBalanceResult getOverseasBalanceResult(String accountNo) {
        log.debug("해외 잔고 조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
        String userId = getCurrentUserId();
        return accountApiRunner.inquireOverseasBalanceInNewTx(userId, accountNo);
    }

    /**
     * 해외(미국) 계좌 요약 — 예수금·총자산 등. 대시보드 US 계좌 카드용.
     * getOverseasBalanceResult와 동일 캐시를 사용하므로 positions 조회 후 호출 시 API 재호출 없음.
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    public OverseasBalanceSummaryDto getOverseasSummary(String accountNo) {
        KoreaInvestmentAccountClient.OverseasBalanceResult result = getOverseasBalanceResult(accountNo);
        return result != null ? result.getSummary() : null;
    }

    /**
     * 계좌 잔고와 보유 종목을 한 번에 조회
     * 주식잔고조회 API를 1회만 호출하여 중복 DB/API 호출을 줄인다.
     * 대시보드 등 잔고·보유종목을 동시에 필요로 하는 화면에서 사용한다.
     *
     * @param accountNo 계좌번호
     * @return 잔고와 보유 종목 (API 실패 시 DB 폴백)
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'balanceAndPositions_' + #accountNo")
    public BalanceAndPositionsDto getBalanceAndPositions(String accountNo) {
        log.debug("잔고·보유종목 일괄 조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));

        try {
            String userId = getCurrentUserId();

            try {
                KoreaInvestmentAccountClient.BalanceAndPositionsResult result = accountApiRunner.inquireBalanceInNewTx(userId,
                        accountNo);
                List<AccountPositionDto> allPositions = new ArrayList<>(result.getPositions());
                KoreaInvestmentAccountClient.OverseasBalanceResult overseasResult = accountApiRunner.inquireOverseasBalanceInNewTx(userId, accountNo);
                if (overseasResult != null && overseasResult.getPositions() != null && !overseasResult.getPositions().isEmpty()) {
                    allPositions.addAll(overseasResult.getPositions());
                }
                return new BalanceAndPositionsDto(result.getBalance(), allPositions);
            } catch (Exception apiException) {
                log.warn("한국투자증권 API 호출 실패, DB 폴백 사용: accountNo={}, error={}",
                        LogMaskingUtil.maskAccountNo(accountNo), apiException.getMessage());
                return new BalanceAndPositionsDto(
                        getAccountBalanceFromDb(accountNo),
                        getPositionsFromDb(accountNo));
            }
        } catch (IllegalStateException e) {
            log.debug("사용자 ID를 찾을 수 없음, DB 폴백 사용: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            return new BalanceAndPositionsDto(
                    getAccountBalanceFromDb(accountNo),
                    getPositionsFromDb(accountNo));
        } catch (Exception e) {
            log.error("잔고·보유종목 조회 실패: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "잔고·보유종목 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * userId 지정으로 잔고·보유종목 조회 (파이프라인·스케줄러 전용).
     * SecurityContext 없이 호출 가능. 캐시 미사용.
     *
     * @param userId    사용자 ID
     * @param accountNo 계좌번호
     * @return 잔고·보유종목 (API 실패 시 DB 폴백), 실패 시 null
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    public BalanceAndPositionsDto getBalanceAndPositionsWithUserId(String userId, String accountNo) {
        if (userId == null || accountNo == null) {
            return null;
        }
        try {
            KoreaInvestmentAccountClient.BalanceAndPositionsResult result = accountApiRunner.inquireBalanceInNewTx(userId,
                    accountNo);
            List<AccountPositionDto> allPositions = new ArrayList<>(result.getPositions());
            KoreaInvestmentAccountClient.OverseasBalanceResult overseasResult = accountApiRunner.inquireOverseasBalanceInNewTx(userId, accountNo);
            List<AccountPositionDto> overseasPositions = overseasResult != null ? overseasResult.getPositions() : null;
            if (overseasPositions != null && !overseasPositions.isEmpty()) {
                allPositions.addAll(overseasPositions);
            }
            return new BalanceAndPositionsDto(result.getBalance(), allPositions);
        } catch (Exception e) {
            log.warn("잔고·보유종목 조회 실패(파이프라인): accountNo={}, error={}",
                    LogMaskingUtil.maskAccountNo(accountNo), e.getMessage());
            try {
                return new BalanceAndPositionsDto(
                        getAccountBalanceFromDb(accountNo),
                        getPositionsFromDb(accountNo));
            } catch (Exception dbEx) {
                return null;
            }
        }
    }

    /**
     * DB에서 보유 종목 조회 (폴백용).
     * null-safe: averagePrice/currentPrice/quantity 등이 null이어도 NPE 없이 처리.
     */
    private List<AccountPositionDto> getPositionsFromDb(String accountNo) {
        List<Portfolio> portfolios = portfolioRepository.findByAccountNo(accountNo);
        if (portfolios == null) {
            return new ArrayList<>();
        }

        return portfolios.stream()
                .map(portfolio -> {
                    BigDecimal avgPrice = portfolio.getAveragePrice() != null
                            ? portfolio.getAveragePrice() : BigDecimal.ZERO;
                    BigDecimal currentPrice = portfolio.getCurrentPrice() != null
                            ? portfolio.getCurrentPrice() : avgPrice;
                    int qty = portfolio.getQuantity() >= 0 ? portfolio.getQuantity() : 0;

                    BigDecimal totalValue = currentPrice.multiply(BigDecimal.valueOf(qty));
                    BigDecimal cost = avgPrice.multiply(BigDecimal.valueOf(qty));
                    BigDecimal profitLoss = totalValue.subtract(cost);
                    BigDecimal profitLossRate = BigDecimal.ZERO;
                    if (cost.compareTo(BigDecimal.ZERO) > 0) {
                        profitLossRate = profitLoss
                                .divide(cost, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }

                    return AccountPositionDto.builder()
                            .symbol(portfolio.getSymbol() != null ? portfolio.getSymbol() : "")
                            .name(portfolio.getName())
                            .quantity(qty)
                            .averagePrice(avgPrice)
                            .currentPrice(currentPrice)
                            .totalValue(totalValue)
                            .profitLoss(profitLoss)
                            .profitLossRate(profitLossRate)
                            .currency(portfolio.getCurrency() != null ? portfolio.getCurrency() : "KRW")
                            .market("KR")
                            .lastUpdated(portfolio.getLastUpdated())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 매수가능조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'buyable_' + #accountNo + '_' + #symbol + '_' + #price")
    public BuyableAmountDto getBuyableAmount(String accountNo, String symbol, BigDecimal price) {
        log.debug("매수가능조회: accountNo={}, symbol={}, price={}", LogMaskingUtil.maskAccountNo(accountNo), symbol, price);

        String userId = getCurrentUserId();
        return accountApiRunner.inquireBuyableAmountInNewTx(userId, accountNo, symbol, price);
    }

    /**
     * 매도가능수량조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'sellable_' + #accountNo + '_' + #symbol")
    public SellableQuantityDto getSellableQuantity(String accountNo, String symbol) {
        log.debug("매도가능수량조회: accountNo={}, symbol={}", LogMaskingUtil.maskAccountNo(accountNo), symbol);

        String userId = getCurrentUserId();
        return accountApiRunner.inquireSellableQuantityInNewTx(userId, accountNo, symbol);
    }

    /**
     * 주문체결조회
     */
    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getOrderHistory(String accountNo, LocalDate startDate, LocalDate endDate) {
        log.debug("주문체결조회: accountNo={}, startDate={}, endDate={}", LogMaskingUtil.maskAccountNo(accountNo), startDate,
                endDate);

        String userId = getCurrentUserId();
        return accountApiRunner.inquireOrderHistoryInNewTx(userId, accountNo, startDate, endDate);
    }

    /**
     * 주식정정취소가능주문조회 (미체결 주문 목록)
     */
    @Transactional(readOnly = true)
    public List<CancelableOrderDto> getCancelableOrders(String accountNo) {
        log.debug("주식정정취소가능주문조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
        String userId = getCurrentUserId();
        return accountApiRunner.inquireCancelableOrdersInNewTx(userId, accountNo);
    }

    /**
     * 투자계좌자산현황조회
     * API 키 없음·한국투자증권 API 실패 시 DB(포트폴리오 합계) 기준으로 자산 요약 반환하여 404 방지.
     */
    @Transactional(readOnly = true, noRollbackFor = { RuntimeException.class, Exception.class })
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'assets_' + #accountNo")
    public AccountAssetDto getAccountAssets(String accountNo) {
        log.debug("투자계좌자산현황조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));

        try {
            String userId = getCurrentUserId();
            return accountApiRunner.inquireAssetsInNewTx(userId, accountNo);
        } catch (IllegalStateException e) {
            log.debug("인증 없음, DB 폴백: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            return getAccountAssetsFromDb(accountNo);
        } catch (DomainException e) {
            if (e.getErrorCode() == ErrorCode.ACCOUNT_NOT_FOUND) {
                log.warn("자산현황 API 실패(API 키 없음 등), DB 폴백: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
                return getAccountAssetsFromDb(accountNo);
            }
            throw e;
        } catch (Exception e) {
            log.warn("자산현황 조회 실패, DB 폴백: accountNo={}, error={}",
                    LogMaskingUtil.maskAccountNo(accountNo), e.getMessage());
            return getAccountAssetsFromDb(accountNo);
        }
    }

    /**
     * DB(포트폴리오 합계) 기준 자산 요약 반환 (폴백용)
     */
    private AccountAssetDto getAccountAssetsFromDb(String accountNo) {
        BigDecimal total = portfolioRepository.getTotalPortfolioValue(accountNo);
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        return AccountAssetDto.builder()
                .accountNo(accountNo)
                .totalAssetValue(total)
                .deposit(total)
                .stockValue(total)
                .totalProfitLoss(BigDecimal.ZERO)
                .totalProfitLossRate(BigDecimal.ZERO)
                .orderableCash(total)
                .currency("KRW")
                .build();
    }

    /**
     * 사용자의 실계좌(serverType "0") 계좌번호 목록. 대시보드 일일손익 등 "실계좌 기준" 데이터 집계용.
     */
    @Transactional(readOnly = true)
    public Set<String> getRealAccountNumbersForUser(String userId) {
        List<UserAccount> realAccounts = userAccountRepository.findByUserIdAndServerTypeAndIsActiveTrue(userId, "0");
        return realAccounts.stream()
                .map(ua -> encryptionUtil.decrypt(ua.getAccountNoEncrypted()))
                .collect(Collectors.toSet());
    }

    /**
     * 기간별손익조회
     */
    @Transactional(readOnly = true)
    public ProfitLossDto getPeriodProfitLoss(String accountNo, LocalDate startDate, LocalDate endDate) {
        log.debug("기간별손익조회: accountNo={}, startDate={}, endDate={}", LogMaskingUtil.maskAccountNo(accountNo), startDate,
                endDate);

        String userId = getCurrentUserId();
        return accountApiRunner.inquirePeriodProfitLossInNewTx(userId, accountNo, startDate, endDate);
    }

    /**
     * 주식잔고조회_실현손익
     */
    @Transactional(readOnly = true)
    public BalanceRealizedProfitLossDto getBalanceRealizedProfitLoss(String accountNo) {
        log.debug("주식잔고조회_실현손익: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
        String userId = getCurrentUserId();
        return accountApiRunner.inquireBalanceRealizedProfitLossInNewTx(userId, accountNo);
    }

    /**
     * 기간별매매손익현황조회
     */
    @Transactional(readOnly = true)
    public PeriodProfitLossStatusDto getPeriodProfitLossStatus(String accountNo, LocalDate startDate, LocalDate endDate) {
        log.debug("기간별매매손익현황조회: accountNo={}, startDate={}, endDate={}",
                LogMaskingUtil.maskAccountNo(accountNo), startDate, endDate);
        String userId = getCurrentUserId();
        return accountApiRunner.inquirePeriodProfitLossStatusInNewTx(userId, accountNo, startDate, endDate);
    }

    /**
     * 메인 계좌 조회 (서버 타입 미지정 시 모의투자 계좌)
     */
    @Transactional(readOnly = true)
    public MainAccountResponseDto getMainAccount(String userId) {
        return getMainAccount(userId, "1");
    }

    /**
     * 메인 계좌 조회 (서버 타입별)
     *
     * @param userId     사용자 ID
     * @param serverType 서버 타입 ("1": 모의투자, "0": 실거래), null/공백/기타 값이면 "1"
     * @return 메인 계좌 정보
     */
    @Transactional(readOnly = true)
    public MainAccountResponseDto getMainAccount(String userId, String serverType) {
        String st = normalizeServerType(serverType);
        log.debug("메인 계좌 조회: userId={}, serverType={}", userId, st);

        UserAccount mainAccount = userAccountRepository.findByUserIdAndServerTypeAndIsDefaultTrue(userId, st)
                .orElse(null);

        if (mainAccount == null) {
            List<UserAccount> accounts = userAccountRepository.findByUserIdAndServerTypeAndIsActiveTrue(userId, st);
            if (accounts.isEmpty()) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "등록된 계좌가 없습니다");
            }
            mainAccount = accounts.get(0);
        }

        String accountNo = encryptionUtil.decrypt(mainAccount.getAccountNoEncrypted());
        String stName = "1".equals(mainAccount.getServerType()) ? "모의투자" : "실거래";

        return MainAccountResponseDto.builder()
                .accountId(mainAccount.getId())
                .accountNo(accountNo)
                .accountNoMasked(LogMaskingUtil.maskAccountNo(accountNo))
                .brokerType(mainAccount.getBrokerType().getCode())
                .brokerTypeName(mainAccount.getBrokerType().getName())
                .serverType(mainAccount.getServerType())
                .serverTypeName(stName)
                .accountName(mainAccount.getAccountName())
                .build();
    }

    /**
     * 사용자 계좌 목록 조회 (전체 또는 서버 타입별)
     *
     * @param userId     사용자 ID
     * @param serverType 서버 타입 ("1", "0"), null이면 전체
     * @return 계좌 목록 (계좌번호 마스킹)
     */
    @Transactional(readOnly = true)
    public AccountListResponseDto getUserAccounts(String userId, String serverType) {
        String st = (serverType != null && !serverType.trim().isEmpty()) ? normalizeServerType(serverType) : null;
        log.debug("사용자 계좌 목록 조회: userId={}, serverType={}", userId, st);

        List<UserAccount> accounts = st != null
                ? userAccountRepository.findByUserIdAndServerTypeAndIsActiveTrue(userId, st)
                : userAccountRepository.findByUserIdAndIsActiveTrue(userId);

        List<UserAccountDto> accountDtos = accounts.stream()
                .map(account -> {
                    String accountNo = encryptionUtil.decrypt(account.getAccountNoEncrypted());
                    String stName = "1".equals(account.getServerType()) ? "모의투자" : "실거래";
                    return UserAccountDto.builder()
                            .accountId(account.getId())
                            .accountNoMasked(LogMaskingUtil.maskAccountNo(accountNo))
                            .brokerType(account.getBrokerType().getCode())
                            .brokerTypeName(account.getBrokerType().getName())
                            .serverType(account.getServerType())
                            .serverTypeName(stName)
                            .accountName(account.getAccountName())
                            .isDefault(account.getIsDefault())
                            .isActive(account.getIsActive())
                            .build();
                })
                .collect(Collectors.toList());

        String mainAccountId = accounts.stream()
                .filter(UserAccount::getIsDefault)
                .map(UserAccount::getId)
                .findFirst()
                .orElse(null);

        return AccountListResponseDto.builder()
                .accounts(accountDtos)
                .mainAccountId(mainAccountId)
                .totalCount(accountDtos.size())
                .build();
    }

    /**
     * 사용자 계좌 목록 조회 (전체)
     */
    @Transactional(readOnly = true)
    public AccountListResponseDto getUserAccounts(String userId) {
        return getUserAccounts(userId, null);
    }

    /**
     * 특정 계좌 조회
     * 
     * @param userId    사용자 ID
     * @param accountId 계좌 ID
     * @return 계좌 정보 (계좌번호는 복호화된 값)
     */
    @Transactional(readOnly = true)
    public MainAccountResponseDto getAccountByAccountId(String userId, String accountId) {
        log.debug("특정 계좌 조회: userId={}, accountId={}", userId, accountId);

        UserAccount account = userAccountRepository.findByUserIdAndId(userId, accountId)
                .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "계좌를 찾을 수 없습니다"));

        if (!account.getIsActive()) {
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "비활성화된 계좌입니다");
        }

        String accountNo = encryptionUtil.decrypt(account.getAccountNoEncrypted());
        String stName = "1".equals(account.getServerType()) ? "모의투자" : "실거래";

        return MainAccountResponseDto.builder()
                .accountId(account.getId())
                .accountNo(accountNo)
                .accountNoMasked(LogMaskingUtil.maskAccountNo(accountNo))
                .brokerType(account.getBrokerType().getCode())
                .brokerTypeName(account.getBrokerType().getName())
                .serverType(account.getServerType())
                .serverTypeName(stName)
                .accountName(account.getAccountName())
                .build();
    }

    /**
     * 메인 계좌 변경
     * 
     * @param userId    사용자 ID
     * @param accountId 새로운 메인 계좌 ID
     */
    @Transactional
    public void setMainAccount(String userId, String accountId) {
        log.debug("메인 계좌 변경: userId={}, accountId={}", userId, accountId);

        // 계좌 조회
        UserAccount account = userAccountRepository.findByUserIdAndId(userId, accountId)
                .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "계좌를 찾을 수 없습니다"));

        if (!account.getIsActive()) {
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "비활성화된 계좌는 메인 계좌로 설정할 수 없습니다");
        }

        // 같은 서버 타입 내 기존 메인 계좌 해제
        userAccountRepository.unsetDefaultAccountsForUserAndServerType(userId, account.getServerType());

        // 새 메인 계좌 설정
        account.setAsDefault();
        userAccountRepository.save(account);

        log.info("메인 계좌 변경 완료: userId={}, accountId={}", userId, accountId);
    }

}
