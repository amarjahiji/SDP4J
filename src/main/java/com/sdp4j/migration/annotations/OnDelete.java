package com.sdp4j.migration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OnDelete {
    com.sdp4j.core.enums.OnDelete action() default com.sdp4j.core.enums.OnDelete.RESTRICT;
}
