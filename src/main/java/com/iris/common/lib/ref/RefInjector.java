package com.iris.common.lib.ref;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Fills null {@link Reference} fields (including inherited ones). Thread-safe; field lookup is cached per class. */
public final class RefInjector {
    /** What a @Reference field declares. */
    public record Spec(String prefix, String suffix, RefDisplay display) {}

    private record RefField(Field field, Spec spec) {
        String decorate(String ref) { return spec.prefix() + ref + spec.suffix(); }
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

    /** Names of the @Reference fields of a class (cached), e.g. to match JPA/Hibernate property names. */
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
        String prefix = f.spec().prefix(), suffix = f.spec().suffix();
        int p = prefix.length(), s = suffix.length();
        if (v.length() < p + s
                || !v.regionMatches(true, 0, prefix, 0, p)
                || !v.regionMatches(true, v.length() - s, suffix, 0, s)) return null;
        return v.substring(p, v.length() - s);
    }

    /** The prefix, suffix and display flag declared on the field; used by {@link RefFormat}. */
    public static Spec spec(Class<?> type, String fieldName) {
        return find(type, fieldName).spec();
    }

    private static RefField find(Class<?> type, String fieldName) {
        for (RefField f : FIELDS.get(type)) if (f.field().getName().equals(fieldName)) return f;
        throw new IllegalArgumentException("no @Reference field '" + fieldName + "' on " + type.getName());
    }

    private static RefField[] scan(Class<?> type) {
        List<RefField> out = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Reference a = f.getAnnotation(Reference.class);
                if (a == null) continue;
                int m = f.getModifiers();
                if (f.getType() != String.class || Modifier.isFinal(m) || Modifier.isStatic(m))
                    throw new IllegalStateException("@Reference needs a non-final, non-static String field: " + f);
                f.setAccessible(true);
                out.add(new RefField(f, new Spec(a.prefix(), a.suffix(), a.display())));
            }
        }
        return out.toArray(new RefField[0]);
    }
}