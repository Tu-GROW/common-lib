package com.iris.common.lib.ref;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Fills null {@link Reference} fields (including inherited ones). Thread-safe; field lookup is cached per class. */
public final class RefInjector {
    private record RefField(Field field, String prefix, String suffix) {
        String decorate(String ref) { return prefix + ref + suffix; }
    }

    private static final ClassValue<RefField[]> FIELDS = new ClassValue<>() {
        @Override protected RefField[] computeValue(Class<?> type) { return scan(type); }
    };

    private final Supplier<String> refs;

    public RefInjector(RefGenerator generator) { this(generator::next); }

    public RefInjector(Supplier<String> refs) { this.refs = Objects.requireNonNull(refs); }

    /** Returns the same instance for chaining. Only null fields are filled, each with its own prefix/suffix. */
    public <T> T inject(T entity) {
        if (entity == null) return null;
        for (RefField f : FIELDS.get(entity.getClass())) {
            try {
                if (f.field().get(entity) == null) f.field().set(entity, f.decorate(refs.get()));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot set " + f.field(), e);
            }
        }
        return entity;
    }

    /** Names of the @GeneratedRef fields of a class (cached), e.g. to match JPA/Hibernate property names. */
    public static String[] fieldNames(Class<?> type) {
        RefField[] fs = FIELDS.get(type);
        String[] names = new String[fs.length];
        for (int i = 0; i < fs.length; i++) names[i] = fs[i].field().getName();
        return names;
    }

    /** One new bare ref from the underlying generator (no prefix/suffix). */
    public String nextRef() { return refs.get(); }

    /** One new ref decorated with the prefix/suffix declared on that field. */
    public String nextRef(Class<?> type, String fieldName) {
        return find(type, fieldName).decorate(refs.get());
    }

    /**
     * Removes the field's prefix/suffix (case-insensitive) so the result can go to
     * {@link RefGenerator#isValid}/{@link RefGenerator#normalize}. Returns null if the value doesn't carry them.
     */
    public static String unwrap(Class<?> type, String fieldName, String value) {
        RefField f = find(type, fieldName);
        if (value == null) return null;
        String v = value.trim();
        int p = f.prefix().length(), s = f.suffix().length();
        if (v.length() < p + s
                || !v.regionMatches(true, 0, f.prefix(), 0, p)
                || !v.regionMatches(true, v.length() - s, f.suffix(), 0, s)) return null;
        return v.substring(p, v.length() - s);
    }

    private static RefField find(Class<?> type, String fieldName) {
        for (RefField f : FIELDS.get(type)) if (f.field().getName().equals(fieldName)) return f;
        throw new IllegalArgumentException("no @GeneratedRef field '" + fieldName + "' on " + type.getName());
    }

    private static RefField[] scan(Class<?> type) {
        List<RefField> out = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Reference a = f.getAnnotation(Reference.class);
                if (a == null) continue;
                int m = f.getModifiers();
                if (f.getType() != String.class || Modifier.isFinal(m) || Modifier.isStatic(m))
                    throw new IllegalStateException("@GeneratedRef needs a non-final, non-static String field: " + f);
                f.setAccessible(true);
                out.add(new RefField(f, a.prefix(), a.suffix()));
            }
        }
        return out.toArray(new RefField[0]);
    }
}