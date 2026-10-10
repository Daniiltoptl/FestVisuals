package com.fest.visuals.api.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    Category category();

    /** One-line Russian summary shown under the module name in the click GUI. */
    String desc() default "";
    int bind() default -999;
}
