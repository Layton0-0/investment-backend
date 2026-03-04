package com.investment.news;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 경량 키워드/감성사전 기반 뉴스 긍부정 점수.
 * ML 없이 키워드 매칭으로 -3 ~ +3 점수. 시그널 가중치 보조용.
 */
@Component
public class NewsSentimentScorer {

    /** 긍정 키워드 및 가중치 (점수 +1 ~ +3) */
    private static final List<KeywordWeight> POSITIVE = Arrays.asList(
            kw("상장", 1), kw("매출증가", 2), kw("실적호조", 2), kw("수주", 2), kw("신사업", 2),
            kw("호실적", 2), kw("증가", 1), kw("성장", 2), kw("돌파", 1), kw("상승", 1),
            kw("적자전환", 2), kw("배당", 1), kw("매수", 1), kw("목표가상향", 2)
    );

    /** 부정 키워드 및 가중치 (점수 -1 ~ -3) */
    private static final List<KeywordWeight> NEGATIVE = Arrays.asList(
            kw("하락", 1), kw("적자", 2), kw("상폐", 3), kw("횡령", 3), kw("소송", 2),
            kw("실적부진", 2), kw("감소", 1), kw("위기", 2), kw("매도", 1), kw("목표가하향", 2),
            kw("리스", 1), kw("부도", 3), kw("조작", 3), kw("규제", 1)
    );

    private static final int SCALE = 2;
    private static final BigDecimal MIN = BigDecimal.valueOf(-3);
    private static final BigDecimal MAX = BigDecimal.valueOf(3);

    /**
     * 제목과 본문(요약)을 합친 텍스트에 대해 긍부정 점수 계산.
     * 키워드 매칭 합산 후 [-3, 3]으로 클램핑. 빈 텍스트/키워드 없음 = 0.
     */
    public BigDecimal scoreText(String title, String summary) {
        String text = concat(title, summary);
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        int sum = 0;
        for (KeywordWeight kw : POSITIVE) {
            if (kw.matches(normalized)) {
                sum += kw.weight;
            }
        }
        for (KeywordWeight kw : NEGATIVE) {
            if (kw.matches(normalized)) {
                sum -= kw.weight;
            }
        }
        BigDecimal raw = BigDecimal.valueOf(sum).setScale(SCALE, RoundingMode.HALF_UP);
        if (raw.compareTo(MIN) < 0) return MIN;
        if (raw.compareTo(MAX) > 0) return MAX;
        return raw;
    }

    private static String concat(String title, String summary) {
        if (title == null) title = "";
        if (summary == null) summary = "";
        return (title + " " + summary).trim();
    }

    private static KeywordWeight kw(String keyword, int weight) {
        return new KeywordWeight(keyword, weight);
    }

    private static final class KeywordWeight {
        final Pattern pattern;
        final int weight;

        KeywordWeight(String keyword, int weight) {
            this.pattern = Pattern.compile(Pattern.quote(keyword));
            this.weight = weight;
        }

        boolean matches(String normalized) {
            return pattern.matcher(normalized).find();
        }
    }
}
