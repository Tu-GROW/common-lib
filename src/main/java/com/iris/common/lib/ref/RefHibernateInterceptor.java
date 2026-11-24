package com.iris.common.lib.ref;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Automatic JPA injection using only Hibernate (already present in any Spring Data JPA app): no
 * @EntityListeners, no extra libraries. onSave runs only when a NEW row is about to be inserted
 * (persist, or merge of a transient entity), never for updates, and Hibernate copies the modified
 * state back onto the entity. A @GeneratedRef property is filled only when its value is null.
 * The injector is resolved lazily so this can be wired into EntityManagerFactory creation
 * without a bean cycle (the JPA allocator itself needs the EntityManagerFactory).
 */
public final class RefHibernateInterceptor implements Interceptor {
    private final Supplier<RefInjector> source;
    private volatile RefInjector injector;

    public RefHibernateInterceptor(Supplier<RefInjector> source) { this.source = source; }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        String[] fields = RefInjector.fieldNames(entity.getClass());
        if (fields.length == 0) return false;               // cheap path for every other entity
        boolean changed = false;
        for (int i = 0; i < propertyNames.length; i++) {
            if (state[i] == null && contains(fields, propertyNames[i])) {
                state[i] = injector().nextRef();
                changed = true;
            }
        }
        return changed;
    }

    private RefInjector injector() {
        RefInjector i = injector;
        if (i == null) injector = i = source.get();
        return i;
    }

    private static boolean contains(String[] names, String name) {
        for (String n : names) if (n.equals(name)) return true;
        return false;
    }

    /** Keeps an interceptor the app already configured: every call goes to it, and onSave also runs ours. */
    public static Interceptor chain(Interceptor user, Interceptor ours) {
        return (Interceptor) Proxy.newProxyInstance(
            Interceptor.class.getClassLoader(), new Class<?>[] { Interceptor.class },
            (proxy, method, args) -> {
                Object result;
                try {
                    result = method.invoke(user, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
                if ("onSave".equals(method.getName()) && args != null && args.length == 5) {
                    boolean mine = ours.onSave(args[0], args[1], (Object[]) args[2], (String[]) args[3], (Type[]) args[4]);
                    return Boolean.TRUE.equals(result) || mine;
                }
                return result;
            });
    }
}
