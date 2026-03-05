package com.investment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipelineTradingWindowProperties")
class PipelineTradingWindowPropertiesTest {

    @Nested
    @DisplayName("getKrSegments")
    class GetKrSegments {
        @Test
        void default_returnsTwoSegments() {
            PipelineTradingWindowProperties props = new PipelineTradingWindowProperties();
            List<PipelineTradingWindowProperties.Segment> segments = props.getKrSegments();
            assertThat(segments).hasSize(2);
            assertThat(segments.get(0).getStart()).isEqualTo(LocalTime.of(9, 0));
            assertThat(segments.get(0).getEnd()).isEqualTo(LocalTime.of(10, 0));
            assertThat(segments.get(1).getStart()).isEqualTo(LocalTime.of(14, 30));
            assertThat(segments.get(1).getEnd()).isEqualTo(LocalTime.of(15, 30));
        }

        @Test
        void segment2Blank_returnsOneSegment() {
            PipelineTradingWindowProperties props = new PipelineTradingWindowProperties();
            props.getKr().setStart2("");
            props.getKr().setEnd2("");
            List<PipelineTradingWindowProperties.Segment> segments = props.getKrSegments();
            assertThat(segments).hasSize(1);
            assertThat(segments.get(0).getStart()).isEqualTo(LocalTime.of(9, 0));
            assertThat(segments.get(0).getEnd()).isEqualTo(LocalTime.of(10, 0));
        }
    }

    @Nested
    @DisplayName("getUsSegments")
    class GetUsSegments {
        @Test
        void default_returnsTwoSegments() {
            PipelineTradingWindowProperties props = new PipelineTradingWindowProperties();
            List<PipelineTradingWindowProperties.Segment> segments = props.getUsSegments();
            assertThat(segments).hasSize(2);
            assertThat(segments.get(0).getStart()).isEqualTo(LocalTime.of(23, 30));
            assertThat(segments.get(0).getEnd()).isEqualTo(LocalTime.of(1, 0));
            assertThat(segments.get(1).getStart()).isEqualTo(LocalTime.of(5, 0));
            assertThat(segments.get(1).getEnd()).isEqualTo(LocalTime.of(6, 0));
        }

        @Test
        void segment2Null_returnsOneSegment() {
            PipelineTradingWindowProperties props = new PipelineTradingWindowProperties();
            props.getUs().setStart2(null);
            props.getUs().setEnd2(null);
            List<PipelineTradingWindowProperties.Segment> segments = props.getUsSegments();
            assertThat(segments).hasSize(1);
            assertThat(segments.get(0).getStart()).isEqualTo(LocalTime.of(23, 30));
            assertThat(segments.get(0).getEnd()).isEqualTo(LocalTime.of(1, 0));
        }
    }

    @Nested
    @DisplayName("parseTime")
    class ParseTime {
        @Test
        void customStart2End2_parsedCorrectly() {
            PipelineTradingWindowProperties props = new PipelineTradingWindowProperties();
            props.getKr().setStart2("08:45");
            props.getKr().setEnd2("11:00");
            List<PipelineTradingWindowProperties.Segment> segments = props.getKrSegments();
            assertThat(segments.get(1).getStart()).isEqualTo(LocalTime.of(8, 45));
            assertThat(segments.get(1).getEnd()).isEqualTo(LocalTime.of(11, 0));
        }
    }
}
