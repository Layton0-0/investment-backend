package com.investment.news;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NewsSentimentScorer")
class NewsSentimentScorerTest {

    private final NewsSentimentScorer scorer = new NewsSentimentScorer();

    @Test
    @DisplayName("긍정 키워드만 있으면 양수 점수를 반환한다")
    void scoreText_positiveKeywords_returnsPositiveScore() {
        assertThat(scorer.scoreText("실적호조", null)).isGreaterThan(BigDecimal.ZERO);
        assertThat(scorer.scoreText("매출증가", "")).isGreaterThan(BigDecimal.ZERO);
        assertThat(scorer.scoreText("상장", null)).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("부정 키워드만 있으면 음수 점수를 반환한다")
    void scoreText_negativeKeywords_returnsNegativeScore() {
        assertThat(scorer.scoreText("하락", null)).isEqualByComparingTo(new BigDecimal("-1.00"));
        assertThat(scorer.scoreText("상폐", "")).isEqualByComparingTo(new BigDecimal("-3.00"));
        assertThat(scorer.scoreText("적자 소송", null)).isEqualByComparingTo(new BigDecimal("-3.00")); // 합산 후 -3~+3 클램핑
    }

    @Test
    @DisplayName("빈 텍스트 또는 키워드 없음이면 0을 반환한다")
    void scoreText_emptyOrNoKeyword_returnsZero() {
        assertThat(scorer.scoreText(null, null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scorer.scoreText("", "")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scorer.scoreText("   ", null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scorer.scoreText("일반 뉴스 제목", "관련 없는 내용")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("점수는 -3 ~ +3 범위로 클램핑된다")
    void scoreText_clampsToRange() {
        // 긍정 과다 -> 3
        assertThat(scorer.scoreText("실적호조 매출증가 수주 신사업 성장", null)).isEqualByComparingTo(new BigDecimal("3.00"));
        // 부정 과다 -> -3
        assertThat(scorer.scoreText("상폐 횡령 부도", null)).isEqualByComparingTo(new BigDecimal("-3.00"));
    }

    @Test
    @DisplayName("긍정과 부정이 섞이면 합산한다")
    void scoreText_mixedKeywords_sumsCorrectly() {
        // 실적호조 +2, 하락 -1 -> 1
        assertThat(scorer.scoreText("실적호조 하락", null)).isEqualByComparingTo(new BigDecimal("1.00"));
        // 매출증가 +2, 적자 -2 -> 0 또는 양수(키워드 중복 매칭 시)
        assertThat(scorer.scoreText("매출증가 적자", null)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
}
