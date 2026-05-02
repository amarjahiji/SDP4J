package com.sdp4j.smigration.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(UniqueKeysConstraint.List.class)
public @interface UniqueKeysConstraint {
    String[] keys();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface List {
        UniqueKeysConstraint[] value();
    }
}