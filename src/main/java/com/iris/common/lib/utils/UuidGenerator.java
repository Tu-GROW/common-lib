package com.iris.common.lib.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.UUID;

/**
 * Generates RFC 9562 UUID version 7 identifiers.
 *
 * <p>UUID v7 embeds a 48-bit millisecond Unix timestamp in the most significant bits,
 * making generated identifiers monotonically time-ordered (unlike the fully random UUID v4),
 * which improves database index locality and allows sorting by creation time.
 *
 * <p>Delegates to the well-tested JUG (Java UUID Generator) library, which also guarantees
 * monotonicity for UUIDs generated within the same millisecond.
 *
 * <p>Usage:
 *   UUID id = UuidGenerator.generateV7();
 */
public final class UuidGenerator {

    private static final NoArgGenerator V7_GENERATOR = Generators.timeBasedEpochGenerator();

    private UuidGenerator() {
    }

    /**
     * @return a new time-ordered UUID (version 7).
     */
    public static UUID generateV7() {
        return V7_GENERATOR.generate();
    }
}
