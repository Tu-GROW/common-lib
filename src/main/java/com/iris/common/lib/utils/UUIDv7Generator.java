package com.iris.common.lib.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Hibernate {@link IdentifierGenerator} that delegates to {@code common-lib}'s
 * {@link UuidGenerator}, producing RFC 9562 UUID version 7 (time-ordered) primary keys
 * instead of Hibernate's built-in random UUID v4 generator.
 *
 * <p>Usage on an entity:
 * <pre>
 *     &#64;Id
 *     &#64;GenericGenerator(name = "uuid7", type = Uuid7Generator.class)
 *     &#64;GeneratedValue(generator = "uuid7")
 *     private UUID id;
 * </pre>
 */
public class UUIDv7Generator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return UuidGenerator.generateV7();
    }
}
