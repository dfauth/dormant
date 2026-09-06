package io.github.dfauth.ta;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ZScoreTest {

    private static Stream<Arguments> inputsAndResultsAvg() {
        return Stream.of(
                Arguments.of(new double[]{0.0, 0.78, 0.28, 17.66, 21.37, 0.06, 37.35, 35.88, 41.82, 12.49},
                        new DoubleSummaryStatistics(10, 0.0, 41.82, 167.69)),
                Arguments.of(new double[]{1.0, 1.0, 1.0},
                        new DoubleSummaryStatistics(3, 1.0, 1.0, 3.0)),
                Arguments.of(new double[]{1.0, 2.0, 3.0},
                        new DoubleSummaryStatistics(3, 1.0, 3.0, 6.0))
        );
    }

    private static Stream<Arguments> inputsAndResultsStdDev() {
        return Stream.of(
                Arguments.of(new double[]{0.0, 0.78, 0.28, 17.66, 21.37, 0.06, 37.35, 35.88, 41.82, 12.49},
                        16.81453964083862),
                Arguments.of(new double[]{1.0, 1.0, 1.0},
                        0.0),
                Arguments.of(new double[]{1.0, 2.0, 3.0},
                        1.0)
        );
    }

    private static Stream<Arguments> inputsAndResultsZScore() {
        return Stream.of(
                Arguments.of(new double[]{0.0, 0.78, 0.28, 17.66, 21.37, 0.06, 37.35, 35.88, 41.82, 12.49},
                        List.of(-0.9972916510465729, -0.9509032267030625, -0.9806393961540306, 0.05298985396162552, 0.27363223128780995, -0.9937233107124569, 1.2240002069407552, 1.1365758687549086, 1.4898415618324112, -0.25448213816138615)),
                Arguments.of(new double[]{1.0, 1.0, 1.0},
                        List.of(0.0, 0.0, 0.0)),
                Arguments.of(new double[]{1.0, 2.0, 3.0},
                        List.of(-1.0, 0.0, 1.0))
        );
    }

    @ParameterizedTest()
    @MethodSource("inputsAndResultsAvg")
    public void testAvg(double[] input, DoubleSummaryStatistics expected) {
        Collector<Double, ZScore<Double, Double>, List<Double>> collector = ZScore.zScoreCollector();
        DoubleSummaryStatistics summaryStatistics = stream(input).summaryStatistics();
        assertEquals(expected.getAverage(), summaryStatistics.getAverage());
        assertEquals(expected.getCount(), summaryStatistics.getCount());
        assertEquals(expected.getSum(), summaryStatistics.getSum());
        assertEquals(expected.getMax(), summaryStatistics.getMax());
        assertEquals(expected.getMin(), summaryStatistics.getMin());
        log.info(""+summaryStatistics);
    }

    @ParameterizedTest
    @MethodSource("inputsAndResultsStdDev")
    public void testStdDev(double[] input, Double expected) {
        Collector<Double, StdDev.Factory, StdDev> collector = StdDev.stdDevCollector();
        StdDev stdDev = stream(input).boxed().collect(collector);
        assertEquals(expected, stdDev.getStdDev());
        log.info(""+stdDev);
    }

    @ParameterizedTest
    @MethodSource("inputsAndResultsZScore")
    public void testZScore(double[] input, List<Double> expected) {
        Collector<Double, ZScore<Double, Double>, List<Double>> collector = ZScore.zScoreCollector();
        List<Double> zScores = stream(input).boxed().collect(collector);
        assertEquals(expected, zScores);
        log.info(""+zScores);
    }
}
