package com.demo.winterframework.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * bean扫描路径 注解
 * @author tangjincheng
 * @date 2025/03/08
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentScan {

    /**
     * bean扫描路径
     * @return {@link String }
     */
    String componentPath() default "";
}
