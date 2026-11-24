package com.iris.common.lib.ref;

/** Crockford base32 (no I, L, O, U) plus a mod-32 check character. */
final class Base32 {
    static final String A = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private Base32() {}

    static int idx(char c) { return A.indexOf(c); }

    /** Uppercases, drops '-' and spaces, maps O->0, I/L->1. Returns null on any invalid char. */
    static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch == '-' || ch == ' ') continue;
            char c = Character.toUpperCase(ch);
            if (c == 'O') c = '0';
            else if (c == 'I' || c == 'L') c = '1';
            if (idx(c) < 0) return null;
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Weighted sum with odd weights 1,3,5,... mod 32. Odd weights are invertible mod 32, so every
     * single-char error is caught; adjacent swaps are caught unless the chars differ by exactly 16.
     */
    static char check(CharSequence s) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) sum += (2 * i + 1) * idx(s.charAt(i));
        return A.charAt(sum & 31);
    }
}
