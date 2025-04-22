package it.unive.scsr.checkers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WarnMap {

    private final Map<String, String> warnMap;

    public WarnMap(Set<Map.Entry<String, String>> entries) {
        // A funky dummy way to create a map from a set of entries.
        final var map = new HashMap<String, String>();
        entries.forEach(e -> map.put(e.getKey(), e.getValue()));
        this.warnMap = map;
    }

    @Override
    public String toString() {

        // Given a key string and a value, returns a JSON pair in the format "key":"value.
        BiFunction<String, String, String> toJsonPair = (key, value) -> {

            // To make parsing the JSON pair more convenient, a string is dynamically "double-quoted". This function
            // simply adds double quotes to both the key and value arguments.
            Function<String, String> appendQuotes = s -> {
                var doubleQuotes = "\"";
                var builder = new StringBuilder();

                if (!s.startsWith(doubleQuotes)) builder.append(doubleQuotes);
                builder.append(s);
                if (!s.endsWith(doubleQuotes)) builder.append(doubleQuotes);

                return builder.toString();
            };

            return appendQuotes.apply(key) + ':' + appendQuotes.apply(value);
        };

        // Compose a JSON-like object where all keys and values are simply strings.
        return warnMap
                .entrySet()
                .stream()
                .map(entry -> toJsonPair.apply(entry.getKey(), entry.getValue()))
                .reduce((result, value) -> result + ',' + value)
                .map(s -> '{' + s + '}')
                .orElse("{}");
    }
}
