package com.sdp4j.simplemigration.annotations;

import com.sdp4j.core.enums.OnDelete;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {

    Class<?> mapsTo();

    String referencedColumn() default "id";

    OnDelete action() default OnDelete.RESTRICT;
}
