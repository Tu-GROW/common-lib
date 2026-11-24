package com.iris.common.lib.ref;

import java.lang.annotation.*;

/**
 * Marks a non-final String field that receives a generated ref before the entity is saved.
 * The ref is injected only when the field is null; a value you set yourself is never overwritten
 * (and no counter value is consumed).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {
    String prefix() default "";
    String suffix() default "";
}
