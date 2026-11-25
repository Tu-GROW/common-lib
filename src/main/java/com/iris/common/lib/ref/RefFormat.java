package com.iris.common.lib.ref;


import com.iris.common.lib.utils.ReadableFormat;

/**
 * Ref-specific display helpers (built on the general {@link ReadableFormat}; use that directly for anything else).
 * Display helpers only: they never change what is generated or stored.
 * Store the compact ref; show {@link #display} to people; turn what they type back into the stored
 * form with {@link #toStored}.
 *
 * <pre>
 * stored:   ABQP30TZZC          RC-ABQP30TZZC7  (prefix, check char)
 * display:  ABQ-P30T-ZZC        RC-ABQ-P30T-ZZC-7
 *           └date┘└─┬─┘└┬┘
 * </pre>
 */
public final class RefFormat {
    private RefFormat() {}

    /** "ABQP30TZZC" -> "ABQ-P30T-ZZC"; with a check char "ABQP30TZZC7" -> "ABQ-P30T-ZZC-7". Other lengths and null pass through. */
    public static String display(String ref) { return display(ref, "-"); }

    /** Same, with your own separator (for example a space for SMS or printed receipts). */
    public static String display(String ref, String separator) {
        if (ref == null) return null;
        int n = ref.length();
        if (n != 10 && n != 11) return ref;
        return ReadableFormat.groups(3, 4, 3, 1).separator(separator).apply(ref);   // date | 4 | 3 | check char
    }

    /**
     * Display form of a stored value from a {@link Reference} field: prefix and suffix kept, the ref grouped.
     * Only fields declared with {@code display = FORMATTED} are grouped; for any other field the stored value is
     * returned unchanged.
     */
    public static String display(Class<?> type, String fieldName, String stored) {
        RefInjector.Spec spec = RefInjector.spec(type, fieldName);
        if (spec.display() != RefDisplay.FORMATTED) return stored;
        String bare = RefInjector.unwrap(type, fieldName, stored);
        if (bare == null) return stored;
        return spec.prefix() + display(bare) + spec.suffix();
    }

    /**
     * Turns what a person typed ("rc-abq-p30t-zzc", "ABQ P30T ZZC", O for 0, I/L for 1) into the stored value
     * for that field, ready for a lookup. Always the compact stored form, whatever the field's display setting.
     * The prefix and suffix may be omitted. Returns null when the input
     * is not a valid ref (bad characters, wrong length, impossible date, or a wrong check character).
     */
    public static String toStored(Class<?> type, String fieldName, String input) {
        if (input == null) return null;
        RefInjector.Spec spec = RefInjector.spec(type, fieldName);
        String prefix = spec.prefix(), suffix = spec.suffix();
        String v = input.trim();
        if (!prefix.isEmpty() && v.regionMatches(true, 0, prefix, 0, prefix.length())) {
            v = v.substring(prefix.length());
        }
        if (!suffix.isEmpty() && v.length() >= suffix.length()
                && v.regionMatches(true, v.length() - suffix.length(), suffix, 0, suffix.length())) {
            v = v.substring(0, v.length() - suffix.length());
        }
        String bare = RefGenerator.normalize(v);
        if (bare == null) return null;
        boolean valid = switch (bare.length()) {
            case 10 -> RefGenerator.isValid(bare, false);
            case 11 -> RefGenerator.isValid(bare, true);
            default -> false;
        };
        return valid ? prefix + bare + suffix : null;
    }
}