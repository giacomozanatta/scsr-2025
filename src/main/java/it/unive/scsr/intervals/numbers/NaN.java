package it.unive.scsr.intervals.numbers;

public record NaN() implements IntervalNumber {
    public static final NaN INSTANCE = new NaN();

    @Override
    public IntervalNumber add(IntervalNumber other, Computation orElse) {
        return this;
    }

    @Override
    public IntervalNumber subtract(IntervalNumber other, Computation orElse) {
        return this;
    }

    @Override
    public IntervalNumber multiply(IntervalNumber other, Computation orElse) {
        return this;
    }

    @Override
    public IntervalNumber divide(IntervalNumber other, Computation orElse) {
        return this;
    }
}
