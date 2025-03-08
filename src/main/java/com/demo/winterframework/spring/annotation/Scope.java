package com.demo.winterframework.spring.annotation;

import com.demo.winterframework.spring.enums.ScopeEnum;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {
    ScopeEnum scope() default ScopeEnum.单例;
}
