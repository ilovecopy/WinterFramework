package com.demo.winterframework.spring.context;

public interface WinterContext {
    /**
     * 通过名称获取bean
     * @param beanName
     * @return {@link Object }
     */
    Object getBeanByName(String beanName);

    /**
     * 通过类型获取bean
     * @param clazz
     * @return {@link Object }
     */
    Object getBeanByClass(Class clazz);
}
