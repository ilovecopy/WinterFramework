package com.demo.winterframework.spring.interfaces;


public interface BeanPostProcessor {
    /**
     * 初始化之前执行
     *
     * @param bean
     * @param beanName
     * @return {@link Object }
     */
    Object postProcessBeforeInitialization(Object bean, String beanName);

    /**
     * 初始化之后执行
     *
     * @param bean
     * @param beanName
     * @return {@link Object }
     */
    Object postProcessAfterInitialization(Object bean, String beanName);
}
