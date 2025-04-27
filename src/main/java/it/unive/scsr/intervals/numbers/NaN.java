package it.unive.scsr.intervals.numbers;

public record NaN() implements IntervalNumber {
    public static final NaN INSTANCE = new NaN();

    @Override
    public IntervalNumber add(IntervalNumber other) {
        return this;
    }

    @Override
    public IntervalNumber subtract(IntervalNumber other) {
        return this;
    }

    @Override
    public IntervalNumber multiply(IntervalNumber other) {
        return this;
    }

    @Override
    public IntervalNumber divide(IntervalNumber other) {
        return this;
    }
}
