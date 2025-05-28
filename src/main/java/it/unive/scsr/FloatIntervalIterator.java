package it.unive.scsr;

import java.util.Iterator;

public class FloatIntervalIterator implements Iterator<Long> {
    private long init;
    private final long end;

    public FloatIntervalIterator(long init, long end) {
        this.init = init;
        this.end = end;
    }

    public boolean hasNext() {
        return this.init <= this.end;
    }

    public Long next() {
        return Long.valueOf((long)(this.init++));
    }
}
