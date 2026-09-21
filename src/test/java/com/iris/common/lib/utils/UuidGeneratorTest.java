package com.iris.common.lib.utils;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorTest {

    @Test
    void generateV7_returnsUuidWithVersionAndVariantBitsSet() {
        UUID uuid = UuidGenerator.generateV7();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generateV7_producesTimeOrderedValues() throws InterruptedException {
        UUID first = UuidGenerator.generateV7();
        Thread.sleep(5);
        UUID second = UuidGenerator.generateV7();

        assertThat(first.compareTo(second)).isLessThan(0);
    }

    @RepeatedTest(5)
    void generateV7_isUnique() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(seen.add(UuidGenerator.generateV7())).isTrue();
        }
    }
}
