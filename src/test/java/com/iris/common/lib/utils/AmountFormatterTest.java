package com.iris.common.lib.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AmountFormatterTest {

    // ── format ────────────────────────────────────────────────────────────────

    @Nested
    class Format {

        @Test
        void givenBigDecimalAmount_whenFormat_returnGroupedWithTwoDecimals() {
            assertEquals("1,234.50", AmountFormatter.format(new BigDecimal("1234.5")));
        }

        @Test
        void givenAmountUnderOneThousand_whenFormat_returnNoGrouping() {
            assertEquals("999.00", AmountFormatter.format(new BigDecimal("999")));
        }

        @Test
        void givenAmountWithMoreDecimalsThanRequested_whenFormat_returnHalfUpRounded() {
            assertEquals("1,234.57", AmountFormatter.format(new BigDecimal("1234.565")));
        }

        @Test
        void givenCustomDecimalPlaces_whenFormat_returnRequestedScale() {
            assertEquals("1,234.5", AmountFormatter.format(new BigDecimal("1234.5"), 1));
            assertEquals("1,235", AmountFormatter.format(new BigDecimal("1234.5"), 0));
        }

        @Test
        void givenDoubleAmount_whenFormat_returnGroupedWithTwoDecimals() {
            assertEquals("1,234.50", AmountFormatter.format(1234.5));
        }

        @Test
        void givenLongAmount_whenFormat_returnGroupedWithTwoDecimals() {
            assertEquals("1,000,000.00", AmountFormatter.format(1_000_000L));
        }

        @Test
        void givenNegativeAmount_whenFormat_returnLeadingMinusSign() {
            assertEquals("-1,234.50", AmountFormatter.format(new BigDecimal("-1234.5")));
        }

        @Test
        void givenNullAmount_whenFormat_throwNullPointerException() {
            assertThrows(NullPointerException.class, () -> AmountFormatter.format((BigDecimal) null));
        }
    }

    // ── formatWithCurrencyCode ───────────────────────────────────────────────

    @Nested
    class FormatWithCurrencyCode {

        @Test
        void givenAmountAndCurrencyCode_whenFormatWithCurrencyCode_returnCodePrefixed() {
            assertEquals("KES 1,234.50", AmountFormatter.formatWithCurrencyCode(new BigDecimal("1234.5"), "KES"));
        }

        @Test
        void givenLowercaseCurrencyCode_whenFormatWithCurrencyCode_returnUppercased() {
            assertEquals("USD 1,234.50", AmountFormatter.formatWithCurrencyCode(new BigDecimal("1234.5"), "usd"));
        }

        @Test
        void givenBlankCurrencyCode_whenFormatWithCurrencyCode_returnPlainFormat() {
            assertEquals("1,234.50", AmountFormatter.formatWithCurrencyCode(new BigDecimal("1234.5"), " "));
        }

        @Test
        void givenDoubleAmount_whenFormatWithCurrencyCode_returnCodePrefixed() {
            assertEquals("KES 1,234.50", AmountFormatter.formatWithCurrencyCode(1234.5, "KES"));
        }

        @Test
        void givenLongAmount_whenFormatWithCurrencyCode_returnCodePrefixed() {
            assertEquals("KES 1,000.00", AmountFormatter.formatWithCurrencyCode(1000L, "KES"));
        }
    }

    // ── formatWithSymbol ─────────────────────────────────────────────────────

    @Nested
    class FormatWithSymbol {

        @Test
        void givenAmountAndSymbol_whenFormatWithSymbol_returnSymbolPrefixedNoSpace() {
            assertEquals("$1,234.50", AmountFormatter.formatWithSymbol(new BigDecimal("1234.5"), "$"));
        }

        @Test
        void givenBlankSymbol_whenFormatWithSymbol_returnPlainFormat() {
            assertEquals("1,234.50", AmountFormatter.formatWithSymbol(new BigDecimal("1234.5"), ""));
        }

        @Test
        void givenDoubleAmount_whenFormatWithSymbol_returnSymbolPrefixed() {
            assertEquals("$1,234.50", AmountFormatter.formatWithSymbol(1234.5, "$"));
        }

        @Test
        void givenLongAmount_whenFormatWithSymbol_returnSymbolPrefixed() {
            assertEquals("$1,000.00", AmountFormatter.formatWithSymbol(1000L, "$"));
        }
    }
}
