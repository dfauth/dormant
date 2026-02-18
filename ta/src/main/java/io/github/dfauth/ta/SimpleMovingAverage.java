package io.github.dfauth.ta;

public class SimpleMovingAverage {

    private SimpleMovingAverage() {}

    public static double[] calculate(double[] prices, int period) {
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1");
        }
        if (prices.length < period) {
            return new double[0];
        }
        int resultLength = prices.length - period + 1;
        double[] result = new double[resultLength];
        double window = 0;
        for (int i = 0; i < period; i++) {
            window += prices[i];
        }
        result[0] = window / period;
        for (int i = 1; i < resultLength; i++) {
            window += prices[i + period - 1] - prices[i - 1];
            result[i] = window / period;
        }
        return result;
    }

    public static double calculate(double[] prices) {
        double window = 0;
        for (int i = 0; i < prices.length; i++) {
            window += prices[i];
        }
        return window / prices.length;
    }
}
