package com.test.yanxiu.network;

/**
 * Created by cailei on 08/11/2016.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequestParamType {
    enum Type {
        GET,
        POST
    }

    Type value();
}