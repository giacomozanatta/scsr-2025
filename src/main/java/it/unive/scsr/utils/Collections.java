package it.unive.scsr.utils;

import java.util.Arrays;
import java.util.Collection;

public class Collections {

    @SafeVarargs
    public static <T> Collection<? extends T> from(Collection<? extends T>... collections) {
        return Arrays
                .stream(collections)
                .flatMap(Collection::stream)
                .toList();
    }
}
