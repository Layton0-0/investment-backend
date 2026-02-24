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
    /** 연간 기본공제 (250만원). 비대주주 국내주식 양도소득 공제 */
    private static final BigDecimal BASIC_DEDUCTION = new BigDecimal("2500000");

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
        // 기본공제 적용: 실현손익이 양수인 경우에만 공제 적용
        BigDecimal taxableAmount = null;
        BigDecimal estimatedTax = null;
        if (domesticSum.compareTo(BigDecimal.ZERO) > 0) {
            // 과세대상 = 실현손익 - 기본공제 (음수면 0)
            taxableAmount = domesticSum.subtract(BASIC_DEDUCTION).max(BigDecimal.ZERO);
            // 예상 세금 = 과세대상 × 22%
            estimatedTax = taxableAmount.multiply(DOMESTIC_TAX_RATE_ASSUMED).setScale(0, RoundingMode.DOWN);
        } else if (domesticSum.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
            estimatedTax = BigDecimal.ZERO;
        }
        return TaxReportSummaryDto.builder()
                .year(y)
                .domesticRealizedGainLoss(domesticSum)
                .overseasRealizedGainLoss(null)
                .dividendTotal(null)
                .basicDeduction(BASIC_DEDUCTION)
                .taxableAmount(taxableAmount)
                .estimatedTax(estimatedTax)
                .disclaimer(DISCLAIMER)
                .build();
    }

    /** CSV 헤더(한글): 연도, 국내 실현손익, 해외 실현손익, 배당 소득 합계, 기본공제, 과세대상 금액, 예상 세금 */
    private static final String CSV_HEADER_KO =
            "연도,국내 실현손익,해외 실현손익,배당 소득 합계,기본공제,과세대상 금액,예상 세금";

    /**
     * 요약 DTO를 CSV 형식으로 내보내기 (UTF-8 BOM). 컬럼명 한글, 하단에 용어 설명 섹션 포함.
     */
    public byte[] exportSummaryAsCsv(TaxReportSummaryDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // UTF-8 BOM
        sb.append(CSV_HEADER_KO).append("\n");
        sb.append(dto.getYear() != null ? dto.getYear() : "");
        sb.append(",").append(dto.getDomesticRealizedGainLoss() != null ? dto.getDomesticRealizedGainLoss() : "");
        sb.append(",").append(dto.getOverseasRealizedGainLoss() != null ? dto.getOverseasRealizedGainLoss() : "");
        sb.append(",").append(dto.getDividendTotal() != null ? dto.getDividendTotal() : "");
        sb.append(",").append(dto.getBasicDeduction() != null ? dto.getBasicDeduction() : "");
        sb.append(",").append(dto.getTaxableAmount() != null ? dto.getTaxableAmount() : "");
        sb.append(",").append(dto.getEstimatedTax() != null ? dto.getEstimatedTax() : "");
        sb.append("\n");
        if (dto.getDisclaimer() != null) {
            sb.append("면책,\"").append(dto.getDisclaimer().replace("\"", "\"\"")).append("\"\n");
        }
        appendCsvGlossary(sb);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** CSV 하단 용어 설명 섹션 (같은 파일 내 별도 블록) */
    private void appendCsvGlossary(StringBuilder sb) {
        sb.append("\n");
        sb.append("【용어 설명】\n");
        sb.append("용어,설명\n");
        sb.append("연도,기준 연도 (예: 2026)\n");
        sb.append("국내 실현손익,해당 연도 국내 주식 매도로 실현된 손익 합계(원)\n");
        sb.append("해외 실현손익,해당 연도 해외 주식 매도로 실현된 손익 합계(원화 환산)\n");
        sb.append("배당 소득 합계,해당 연도 받은 배당금 합계(원)\n");
        sb.append("기본공제,국내 주식 양도소득 시 적용되는 연간 기본공제(비대주주 250만원 등)\n");
        sb.append("과세대상 금액,실현손익에서 기본공제를 뺀 후 과세 대상이 되는 금액(원)\n");
        sb.append("예상 세금,과세대상 금액에 세율을 적용한 추정 세금(세무 자문 아님)\n");
    }

    /**
     * 요약 DTO를 PDF 형식으로 내보내기. 본문 하단에 용어 설명 섹션 포함.
     */
    public byte[] exportSummaryAsPdf(TaxReportSummaryDto dto) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("연말 세금 요약 (" + (dto.getYear() != null ? dto.getYear() : "") + "년)"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("국내 실현손익: " + formatAmount(dto.getDomesticRealizedGainLoss())));
            document.add(new Paragraph("해외 실현손익: " + formatAmount(dto.getOverseasRealizedGainLoss())));
            document.add(new Paragraph("배당 소득: " + formatAmount(dto.getDividendTotal())));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("기본공제: " + formatAmount(dto.getBasicDeduction())));
            document.add(new Paragraph("과세대상: " + formatAmount(dto.getTaxableAmount())));
            document.add(new Paragraph("예상 세금: " + formatAmount(dto.getEstimatedTax())));
            document.add(new Paragraph(" "));
            if (dto.getDisclaimer() != null) {
                document.add(new Paragraph(dto.getDisclaimer()));
            }
            appendPdfGlossary(document);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("PDF 생성 실패: {}", e.getMessage());
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    /** PDF 하단 용어 설명 섹션 */
    private void appendPdfGlossary(Document document) throws Exception {
        document.add(new Paragraph(" "));
        document.add(new Paragraph("────────────────────────"));
        document.add(new Paragraph("용어 설명"));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("· 연도: 기준 연도 (예: 2026)"));
        document.add(new Paragraph("· 국내 실현손익: 해당 연도 국내 주식 매도로 실현된 손익 합계(원)"));
        document.add(new Paragraph("· 해외 실현손익: 해당 연도 해외 주식 매도로 실현된 손익 합계(원화 환산)"));
        document.add(new Paragraph("· 배당 소득: 해당 연도 받은 배당금 합계(원)"));
        document.add(new Paragraph("· 기본공제: 국내 주식 양도소득 시 적용되는 연간 기본공제(비대주주 250만원 등)"));
        document.add(new Paragraph("· 과세대상: 실현손익에서 기본공제를 뺀 후 과세 대상이 되는 금액(원)"));
        document.add(new Paragraph("· 예상 세금: 과세대상에 세율을 적용한 추정 세금(세무 자문 아님)"));
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("%,d원", amount.longValue()) : "-";
    }
}
