package local.villagers.training;

import java.util.Arrays;

/** Native tick-cost sample summaries, distinct from achieved wall-clock TPS and scheduling lateness. */
public record TickStatistics(int samples, double meanMspt, double p50Mspt, double p95Mspt,
                             double p99Mspt, double maxMspt, int overBudgetTicks) {
    public static TickStatistics summarize(long[] nanos, double targetTps) {
        long[] sorted = Arrays.stream(nanos).filter(value -> value > 0).sorted().toArray();
        if (sorted.length == 0) return new TickStatistics(0, 0, 0, 0, 0, 0, 0);
        double budget = 1_000_000_000.0 / targetTps;
        double mean = Arrays.stream(sorted).average().orElse(0) / 1e6;
        int over = (int) Arrays.stream(sorted).filter(value -> value > budget).count();
        return new TickStatistics(sorted.length, mean, percentile(sorted, .5), percentile(sorted, .95),
            percentile(sorted, .99), sorted[sorted.length - 1] / 1e6, over);
    }
    private static double percentile(long[] sorted, double quantile) {
        return sorted[Math.max(0, (int) Math.ceil(quantile * sorted.length) - 1)] / 1e6;
    }
}
