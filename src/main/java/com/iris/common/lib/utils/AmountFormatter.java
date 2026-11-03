package com.iris.common.lib.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility for formatting monetary amounts with thousand-separator grouping
 * and optional currency code/symbol prefixing.
 *
 * Amounts are rounded HALF_UP to the requested number of decimal places
 * (2 by default) before formatting.
 *
 * Usage:
 *   AmountFormatter.format(new BigDecimal("1234.5"));                 // "1,234.50"
 *   AmountFormatter.formatWithCurrencyCode(1234.5, "KES");            // "KES 1,234.50"
 *   AmountFormatter.formatWithSymbol(1234.5, "$");                    // "$1,234.50"
 */
public final class AmountFormatter {

    private AmountFormatter() {}

    private static final int DEFAULT_DECIMAL_PLACES = 2;

    // -------------------------------------------------------------------------
    // Plain formatting (grouping only, no currency)
    // -------------------------------------------------------------------------

    public static String format(BigDecimal amount) {
        return format(amount, DEFAULT_DECIMAL_PLACES);
    }

    public static String format(BigDecimal amount, int decimalPlaces) {
        Objects.requireNonNull(amount, "amount must not be null");
        BigDecimal scaled = amount.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return buildFormat(decimalPlaces).format(scaled);
    }

    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount), DEFAULT_DECIMAL_PLACES);
    }

    public static String format(double amount, int decimalPlaces) {
        return format(BigDecimal.valueOf(amount), decimalPlaces);
    }

    public static String format(long amount) {
        return format(BigDecimal.valueOf(amount), DEFAULT_DECIMAL_PLACES);
    }

    public static String format(long amount, int decimalPlaces) {
        return format(BigDecimal.valueOf(amount), decimalPlaces);
    }

    // -------------------------------------------------------------------------
    // Currency code prefix, e.g. "KES 1,234.50"
    // -------------------------------------------------------------------------

    public static String formatWithCurrencyCode(BigDecimal amount, String currencyCode) {
        return formatWithCurrencyCode(amount, currencyCode, DEFAULT_DECIMAL_PLACES);
    }

    public static String formatWithCurrencyCode(BigDecimal amount, String currencyCode, int decimalPlaces) {
        String formatted = format(amount, decimalPlaces);
        return prefixCurrencyCode(formatted, currencyCode);
    }

    public static String formatWithCurrencyCode(double amount, String currencyCode) {
        return formatWithCurrencyCode(BigDecimal.valueOf(amount), currencyCode, DEFAULT_DECIMAL_PLACES);
    }

    public static String formatWithCurrencyCode(double amount, String currencyCode, int decimalPlaces) {
        return formatWithCurrencyCode(BigDecimal.valueOf(amount), currencyCode, decimalPlaces);
    }

    public static String formatWithCurrencyCode(long amount, String currencyCode) {
        return formatWithCurrencyCode(BigDecimal.valueOf(amount), currencyCode, DEFAULT_DECIMAL_PLACES);
    }

    public static String formatWithCurrencyCode(long amount, String currencyCode, int decimalPlaces) {
        return formatWithCurrencyCode(BigDecimal.valueOf(amount), currencyCode, decimalPlaces);
    }

    // -------------------------------------------------------------------------
    // Currency symbol prefix, e.g. "$1,234.50"
    // -------------------------------------------------------------------------

    public static String formatWithSymbol(BigDecimal amount, String symbol) {
        return formatWithSymbol(amount, symbol, DEFAULT_DECIMAL_PLACES);
    }

    public static String formatWithSymbol(BigDecimal amount, String symbol, int decimalPlaces) {
        String formatted = format(amount, decimalPlaces);
        return prefixSymbol(formatted, symbol);
    }

    public static String formatWithSymbol(double amount, String symbol) {
        return formatWithSymbol(BigDecimal.valueOf(amount), symbol, DEFAULT_DECIMAL_PLACES);
    }

    public static String formatWithSymbol(double amount, String symbol, int decimalPlaces) {
        return formatWithSymbol(BigDecimal.valueOf(amount), symbol, decimalPlaces);
    }

    public static String formatWithSymbol(long amount, String symbol) {
        return formatWithSymbol(BigDecimal.valueOf(amount), symbol, DEFAULT_DECIMAL_PLACES);
    }

    public static String formatWithSymbol(long amount, String symbol, int decimalPlaces) {
        return formatWithSymbol(BigDecimal.valueOf(amount), symbol, decimalPlaces);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String prefixCurrencyCode(String formatted, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) return formatted;
        return currencyCode.trim().toUpperCase(Locale.ROOT) + " " + formatted;
    }

    private static String prefixSymbol(String formatted, String symbol) {
        if (symbol == null || symbol.isBlank()) return formatted;
        return symbol.trim() + formatted;
    }

    private static DecimalFormat buildFormat(int decimalPlaces) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append('.');
            pattern.append("0".repeat(decimalPlaces));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }
}
