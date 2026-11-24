package com.iris.common.lib.config;

import com.iris.common.lib.ref.RefGenerator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@ConfigurationProperties(prefix = "refgen")
public class RefProperties {
    /** Base64, >= 32 bytes. Must be identical on every instance. */
    private String masterKey;
    /** Where counter blocks are leased: "jdbc" (default, raw DataSource connection) or "jpa" (REQUIRES_NEW on the JPA transaction manager). */
    private String allocator = "jdbc";
    /** Auto-inject @GeneratedRef fields on JPA insert via a Hibernate interceptor. */
    private boolean injectJpa = true;
    /** Counter values leased from the store per round trip. */
    private long blockSize = RefGenerator.DEFAULT_BLOCK_SIZE;
    /** Append an 11th check character. */
    private boolean checkChar = false;
    /** Substrings to skip (uppercase Crockford chars). Replaces the default list when set. */
    private Set<String> blocklist = new HashSet<>(RefGenerator.DEFAULT_BLOCKLIST);

}
