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
import java.util.List;
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

    /**
     * 계좌 잔고 조회
     * 한국투자증권 API를 사용하여 실시간 잔고 정보를 조회합니다.
     * API 실패 시 DB 폴백을 사용합니다.
     * 
     * @param accountNo 계좌번호
     * @return 계좌 잔고 정보
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'balance_' + #accountNo")
    public AccountBalanceDto getAccountBalance(String accountNo) {
        log.debug("계좌 잔고 조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));

        try {
            // 현재 사용자 ID 가져오기
            String userId = getCurrentUserId();

            // 한국투자증권 API 사용 시도
            try {
                KoreaInvestmentAccountClient.BalanceAndPositionsResult result = accountClient.inquireBalance(userId,
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
        log.debug("사용자 계좌 조회: userId={}, serverType={}", userId, serverType);

        try {
            String st = serverType != null ? serverType : "1";
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
            log.error("사용자 계좌 조회 실패: userId={}, serverType={}", userId, serverType, e);
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
     * 보유 종목 조회
     * 한국투자증권 API를 사용하여 실시간 보유 종목 정보를 조회합니다.
     * API 실패 시 DB 폴백을 사용합니다.
     * 
     * @param accountNo 계좌번호
     * @return 보유 종목 목록
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'positions_' + #accountNo")
    public List<AccountPositionDto> getPositions(String accountNo) {
        log.debug("보유 종목 조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));

        try {
            // 현재 사용자 ID 가져오기
            String userId = getCurrentUserId();

            // 한국투자증권 API 사용 시도
            try {
                KoreaInvestmentAccountClient.BalanceAndPositionsResult result = accountClient.inquireBalance(userId,
                        accountNo);
                return result.getPositions();
            } catch (Exception apiException) {
                log.warn("한국투자증권 API 호출 실패, DB 폴백 사용: accountNo={}, error={}",
                        LogMaskingUtil.maskAccountNo(accountNo), apiException.getMessage());
                // API 실패 시 DB 폴백
                return getPositionsFromDb(accountNo);
            }
        } catch (IllegalStateException e) {
            // 인증되지 않은 사용자 또는 userId를 찾을 수 없는 경우 DB 폴백
            log.debug("사용자 ID를 찾을 수 없음, DB 폴백 사용: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));
            return getPositionsFromDb(accountNo);
        } catch (Exception e) {
            log.error("보유 종목 조회 실패: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "보유 종목 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * DB에서 보유 종목 조회 (폴백용)
     */
    private List<AccountPositionDto> getPositionsFromDb(String accountNo) {
        List<Portfolio> portfolios = portfolioRepository.findByAccountNo(accountNo);

        return portfolios.stream()
                .map(portfolio -> {
                    BigDecimal currentPrice = portfolio.getCurrentPrice();
                    if (currentPrice == null) {
                        currentPrice = portfolio.getAveragePrice();
                    }

                    BigDecimal totalValue = currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
                    BigDecimal profitLoss = totalValue.subtract(
                            portfolio.getAveragePrice().multiply(BigDecimal.valueOf(portfolio.getQuantity())));
                    BigDecimal profitLossRate = portfolio.getAveragePrice().compareTo(BigDecimal.ZERO) > 0 ? profitLoss
                            .divide(portfolio.getAveragePrice().multiply(BigDecimal.valueOf(portfolio.getQuantity())),
                                    4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

                    return AccountPositionDto.builder()
                            .symbol(portfolio.getSymbol())
                            .name(portfolio.getName())
                            .quantity(portfolio.getQuantity())
                            .averagePrice(portfolio.getAveragePrice())
                            .currentPrice(currentPrice)
                            .totalValue(totalValue)
                            .profitLoss(profitLoss)
                            .profitLossRate(profitLossRate)
                            .currency(portfolio.getCurrency() != null ? portfolio.getCurrency() : "KRW")
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
        return accountClient.inquireBuyableAmount(userId, accountNo, symbol, price);
    }

    /**
     * 매도가능수량조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'sellable_' + #accountNo + '_' + #symbol")
    public SellableQuantityDto getSellableQuantity(String accountNo, String symbol) {
        log.debug("매도가능수량조회: accountNo={}, symbol={}", LogMaskingUtil.maskAccountNo(accountNo), symbol);

        String userId = getCurrentUserId();
        return accountClient.inquireSellableQuantity(userId, accountNo, symbol);
    }

    /**
     * 주문체결조회
     */
    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getOrderHistory(String accountNo, LocalDate startDate, LocalDate endDate) {
        log.debug("주문체결조회: accountNo={}, startDate={}, endDate={}", LogMaskingUtil.maskAccountNo(accountNo), startDate,
                endDate);

        String userId = getCurrentUserId();
        return accountClient.inquireOrderHistory(userId, accountNo, startDate, endDate);
    }

    /**
     * 투자계좌자산현황조회
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACCOUNT, key = "'assets_' + #accountNo")
    public AccountAssetDto getAccountAssets(String accountNo) {
        log.debug("투자계좌자산현황조회: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo));

        String userId = getCurrentUserId();
        return accountClient.inquireAssets(userId, accountNo);
    }

    /**
     * 기간별손익조회
     */
    @Transactional(readOnly = true)
    public ProfitLossDto getPeriodProfitLoss(String accountNo, LocalDate startDate, LocalDate endDate) {
        log.debug("기간별손익조회: accountNo={}, startDate={}, endDate={}", LogMaskingUtil.maskAccountNo(accountNo), startDate,
                endDate);

        String userId = getCurrentUserId();
        return accountClient.inquirePeriodProfitLoss(userId, accountNo, startDate, endDate);
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
     * @param serverType 서버 타입 ("1": 모의투자, "0": 실거래), null이면 "1"
     * @return 메인 계좌 정보
     */
    @Transactional(readOnly = true)
    public MainAccountResponseDto getMainAccount(String userId, String serverType) {
        log.debug("메인 계좌 조회: userId={}, serverType={}", userId, serverType);

        String st = serverType != null ? serverType : "1";
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
        log.debug("사용자 계좌 목록 조회: userId={}, serverType={}", userId, serverType);

        List<UserAccount> accounts = serverType != null && !serverType.isEmpty()
                ? userAccountRepository.findByUserIdAndServerTypeAndIsActiveTrue(userId, serverType)
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
