package com.investment.report.service;

import com.investment.account.dto.ProfitLossDto;
import com.investment.account.service.AccountService;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.report.dto.TaxReportSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * 연말 세금·리포트 서비스.
 * 집계: 사용자 계좌별 한국투자증권 기간별손익조회(realizedProfitLoss) 연도 구간 합산.
 * 배당·해외 구분·PDF/CSV·Hometax 연동은 별도.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxReportService {

    private static final String DISCLAIMER = "본 내용은 추정이며 세무 자문이 아닙니다. 실제 신고는 국세청 홈택스 등에서 확인하세요.";
    /** 예상 세금 단순 가정: 국내 실현손익 양수 구간 세율 (표준 단순 적용) */
    private static final BigDecimal DOMESTIC_TAX_RATE_ASSUMED = new BigDecimal("0.22");

    private final TradingSettingRepository tradingSettingRepository;
    private final AccountService accountService;

    /**
     * 연말 세금 요약. userId가 null이면 스텁 반환.
     * 실데이터: 해당 연도 기간별손익 실현손익 합산 → domesticRealizedGainLoss, 예상 세금 가정 공식 적용.
     */
    public TaxReportSummaryDto getSummary(String userId, Integer year) {
        int y = year != null ? year : Year.now().getValue();
        if (userId == null || userId.isBlank()) {
            return TaxReportSummaryDto.builder()
                    .year(y)
                    .domesticRealizedGainLoss(null)
                    .overseasRealizedGainLoss(null)
                    .dividendTotal(null)
                    .estimatedTax(null)
                    .disclaimer(DISCLAIMER)
                    .build();
        }
        LocalDate start = LocalDate.of(y, 1, 1);
        LocalDate end = LocalDate.of(y, 12, 31);
        List<com.investment.domain.entity.TradingSetting> settings = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
        BigDecimal domesticSum = BigDecimal.ZERO;
        for (com.investment.domain.entity.TradingSetting setting : settings) {
            String accountNo = setting.getAccountNo();
            try {
                ProfitLossDto pl = accountService.getPeriodProfitLoss(accountNo, start, end);
                if (pl != null && pl.getRealizedProfitLoss() != null) {
                    domesticSum = domesticSum.add(pl.getRealizedProfitLoss());
                }
            } catch (Exception e) {
                log.warn("기간별손익 조회 스킵: accountNo 마스킹, year={}, error={}", y, e.getMessage());
            }
        }
        BigDecimal estimatedTax = null;
        if (domesticSum.compareTo(BigDecimal.ZERO) > 0) {
            estimatedTax = domesticSum.multiply(DOMESTIC_TAX_RATE_ASSUMED).setScale(0, RoundingMode.DOWN);
        } else if (domesticSum.compareTo(BigDecimal.ZERO) < 0) {
            estimatedTax = BigDecimal.ZERO;
        }
        return TaxReportSummaryDto.builder()
                .year(y)
                .domesticRealizedGainLoss(domesticSum)
                .overseasRealizedGainLoss(null)
                .dividendTotal(null)
                .estimatedTax(estimatedTax)
                .disclaimer(DISCLAIMER)
                .build();
    }

    /**
     * 요약 DTO를 CSV 형식으로 내보내기 (UTF-8 BOM).
     */
    public byte[] exportSummaryAsCsv(TaxReportSummaryDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // UTF-8 BOM
        sb.append("year,domesticRealizedGainLoss,overseasRealizedGainLoss,dividendTotal,estimatedTax\n");
        sb.append(dto.getYear()).append(",");
        sb.append(dto.getDomesticRealizedGainLoss() != null ? dto.getDomesticRealizedGainLoss() : "");
        sb.append(",").append(dto.getOverseasRealizedGainLoss() != null ? dto.getOverseasRealizedGainLoss() : "");
        sb.append(",").append(dto.getDividendTotal() != null ? dto.getDividendTotal() : "");
        sb.append(",").append(dto.getEstimatedTax() != null ? dto.getEstimatedTax() : "");
        sb.append("\n");
        if (dto.getDisclaimer() != null) {
            sb.append("disclaimer,\"").append(dto.getDisclaimer().replace("\"", "\"\"")).append("\"\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 요약 DTO를 PDF 형식으로 내보내기.
     */
    public byte[] exportSummaryAsPdf(TaxReportSummaryDto dto) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("연말 세금 요약 (" + dto.getYear() + "년)"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("국내 실현손익: " + (dto.getDomesticRealizedGainLoss() != null ? dto.getDomesticRealizedGainLoss() : "-")));
            document.add(new Paragraph("해외 실현손익: " + (dto.getOverseasRealizedGainLoss() != null ? dto.getOverseasRealizedGainLoss() : "-")));
            document.add(new Paragraph("배당 소득: " + (dto.getDividendTotal() != null ? dto.getDividendTotal() : "-")));
            document.add(new Paragraph("예상 세금: " + (dto.getEstimatedTax() != null ? dto.getEstimatedTax() : "-")));
            document.add(new Paragraph(" "));
            if (dto.getDisclaimer() != null) {
                document.add(new Paragraph(dto.getDisclaimer()));
            }
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("PDF 생성 실패: {}", e.getMessage());
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }
}
