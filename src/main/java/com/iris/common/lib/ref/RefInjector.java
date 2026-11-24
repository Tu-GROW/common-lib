package com.iris.common.lib.ref;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Fills null {@link Reference} fields (including inherited ones). Thread-safe; field lookup is cached per class. */
public final class RefInjector {
    private final Supplier<String> refs;
    private static final ClassValue<Field[]> FIELDS = new ClassValue<>() {
        @Override protected Field[] computeValue(Class<?> type) { return scan(type); }
    };

    public RefInjector(RefGenerator generator) { this(generator::next); }

    public RefInjector(Supplier<String> refs) { this.refs = Objects.requireNonNull(refs); }

    /** Names of the @GeneratedRef fields of a class (cached), e.g. to match JPA/Hibernate property names. */
    public static String[] fieldNames(Class<?> type) {
        Field[] fs = FIELDS.get(type);
        String[] names = new String[fs.length];
        for (int i = 0; i < fs.length; i++) names[i] = fs[i].getName();
        return names;
    }

    /** One new ref from the underlying generator. */
    public String nextRef() { return refs.get(); }

    /** Returns the same instance for chaining. Only null fields are filled. */
    public <T> T inject(T entity) {
        if (entity == null) return null;
        for (Field f : FIELDS.get(entity.getClass())) {
            try {
                if (f.get(entity) == null) f.set(entity, refs.get());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot set " + f, e);
            }
        }
        return entity;
    }

    private static Field[] scan(Class<?> type) {
        List<Field> out = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.isAnnotationPresent(Reference.class)) continue;
                int m = f.getModifiers();
                if (f.getType() != String.class || Modifier.isFinal(m) || Modifier.isStatic(m))
                    throw new IllegalStateException("@GeneratedRef needs a non-final, non-static String field: " + f);
                f.setAccessible(true);
                out.add(f);
            }
        }
        return out.toArray(new Field[0]);
    }
}
