package com.iris.common.lib.ref;

import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;

import java.util.function.Supplier;

/**
 * Spring Data JDBC hook. This callback fires on every save (insert AND update), and JDBC updates write all
 * columns, so it injects only into NEW aggregates. Otherwise an update that omits the ref would mint a new one.
 */
public class RefBeforeConvertCallback implements BeforeConvertCallback<Object> {
    private final RefInjector injector;
    private final Supplier<RelationalMappingContext> context;   // lazy: only exists when Spring Data JDBC is active

    public RefBeforeConvertCallback(RefInjector injector, Supplier<RelationalMappingContext> context) {
        this.injector = injector;
        this.context = context;
    }

    @Override
    public Object onBeforeConvert(Object aggregate) {
        if (context.get().getRequiredPersistentEntity(aggregate.getClass()).isNew(aggregate)) {
            injector.inject(aggregate);
        }
        return aggregate;
    }
}
