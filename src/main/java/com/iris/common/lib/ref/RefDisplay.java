package com.iris.common.lib.ref;

/** How a {@link Reference} field is shown to people. It never changes what is stored (always compact). */
public enum RefDisplay {
    /** Shown exactly as stored: {@code RC-ABQP30TZZC}. The default. */
    COMPACT,
    /** Shown grouped by {@link RefFormat#display(Class, String, String)}: {@code RC-ABQ-P30T-ZZC}. */
    FORMATTED
}
