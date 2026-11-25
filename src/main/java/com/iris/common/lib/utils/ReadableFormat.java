package com.iris.common.lib.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * General-purpose helper that makes compact strings easy to read, say aloud and type. It knows nothing
 * about refs: use it for receipt numbers, invoice numbers, phone numbers, card numbers, account numbers,
 * licence keys, big numbers, and so on. Instances are immutable and thread-safe; each setter returns a new one.
 *
 * <pre>{@code
 * ReadableFormat.groups(3, 4, 3).apply("ABQP30TZZC");                    // ABQ-P30T-ZZC
 * ReadableFormat.every(4).separator(" ").apply("4111111111111111");      // 4111 1111 1111 1111
 * ReadableFormat.every(3).separator(",").fromEnd().apply("1234567");     // 1,234,567
 * ReadableFormat.groups(4, 3, 3).separator(" ").apply("0712345678");     // 0712 345 678
 * ReadableFormat.groups(3, 4, 3).upperCase().apply("abqp30tzzc");        // ABQ-P30T-ZZC
 * }</pre>
 *
 * Groups are filled in order; when the value runs out the remaining sizes are simply unused, and characters
 * left over after the explicit sizes form one last group (or repeat as uniform groups for {@link #every}).
 */
public final class ReadableFormat {
    private enum Case { KEEP, UPPER, LOWER }

    private final int[] sizes;          // explicit leading groups
    private final int repeat;           // size of the groups after those; 0 = leftover forms one group
    private final String separator;
    private final boolean fromEnd;
    private final Case letterCase;

    private ReadableFormat(int[] sizes, int repeat, String separator, boolean fromEnd, Case letterCase) {
        this.sizes = sizes;
        this.repeat = repeat;
        this.separator = separator;
        this.fromEnd = fromEnd;
        this.letterCase = letterCase;
    }

    /** Explicit group sizes, in order; anything left over becomes one final group. Default separator "-". */
    public static ReadableFormat groups(int... sizes) {
        for (int s : sizes) if (s <= 0) throw new IllegalArgumentException("group size must be > 0: " + s);
        return new ReadableFormat(sizes.clone(), 0, "-", false, Case.KEEP);
    }

    /** Uniform groups of {@code size}; the last group holds the remainder. Default separator "-". */
    public static ReadableFormat every(int size) {
        if (size <= 0) throw new IllegalArgumentException("group size must be > 0: " + size);
        return new ReadableFormat(new int[0], size, "-", false, Case.KEEP);
    }

    public ReadableFormat separator(String separator) {
        if (separator == null) throw new IllegalArgumentException("separator is required");
        return new ReadableFormat(sizes, repeat, separator, fromEnd, letterCase);
    }

    /** Group from the right instead of the left, so the remainder lands at the front (1,234,567). */
    public ReadableFormat fromEnd() { return new ReadableFormat(sizes, repeat, separator, true, letterCase); }

    public ReadableFormat upperCase() { return new ReadableFormat(sizes, repeat, separator, fromEnd, Case.UPPER); }

    public ReadableFormat lowerCase() { return new ReadableFormat(sizes, repeat, separator, fromEnd, Case.LOWER); }

    /** Readable form of {@code value}. Null returns null. */
    public String apply(String value) {
        if (value == null) return null;
        String v = convertCase(value);
        List<String> chunks = new ArrayList<>();
        int pos = fromEnd ? v.length() : 0;
        for (int i = 0; fromEnd ? pos > 0 : pos < v.length(); i++) {
            int size = i < sizes.length ? sizes[i] : (repeat > 0 ? repeat : Integer.MAX_VALUE);
            int len = Math.min(size, fromEnd ? pos : v.length() - pos);
            if (fromEnd) {
                chunks.add(0, v.substring(pos - len, pos));
                pos -= len;
            } else {
                chunks.add(v.substring(pos, pos + len));
                pos += len;
            }
        }
        return String.join(separator, chunks);
    }

    /**
     * Reverse of {@link #apply}: drops whitespace and every character of the separator, then applies the
     * case setting. Use it on what a person typed before comparing or looking it up. Null returns null.
     */
    public String strip(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (!Character.isWhitespace(c) && separator.indexOf(c) < 0) sb.append(c);
        }
        return convertCase(sb.toString());
    }

    private String convertCase(String s) {
        return switch (letterCase) {
            case UPPER -> s.toUpperCase(Locale.ROOT);
            case LOWER -> s.toLowerCase(Locale.ROOT);
            case KEEP -> s;
        };
    }
}