package com.demo.winterframework.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * bean名称
     *
     * @return {@link String }
     */
    String beanName() default "";

    /**
     * bean类型
     *
     * @return {@link Class }
     */
    Class beanClazz() default Object.class;
}
