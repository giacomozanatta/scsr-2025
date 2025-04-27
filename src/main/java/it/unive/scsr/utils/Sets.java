package it.unive.scsr.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class Sets {

    @SafeVarargs
    public static <T> Set<T> from(Set<? extends T>... sets) {
        return Arrays
                .stream(sets)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
