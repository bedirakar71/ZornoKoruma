package dev.zorno.guard;

/** Vuruş başına harcanan süreyi ölçer (sunucuyu yormadığını /zg durum ile görmek için). */
public final class PerfStats {
    private long count;
    private double avgNanos;
    private long maxNanos;

    public void record(long nanos) {
        count++;
        avgNanos = count == 1 ? nanos : avgNanos + (nanos - avgNanos) * 0.02;
        if (nanos > maxNanos) maxNanos = nanos;
    }

    public long count() { return count; }
    public double avgMicros() { return avgNanos / 1000.0; }
    public double maxMicros() { return maxNanos / 1000.0; }
    public void resetMax() { maxNanos = 0L; }
}
