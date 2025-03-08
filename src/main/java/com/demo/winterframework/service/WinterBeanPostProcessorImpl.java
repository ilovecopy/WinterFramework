package com.demo.winterframework.service;

import com.demo.winterframework.spring.annotation.Component;
import com.demo.winterframework.spring.interfaces.BeanPostProcessor;

import java.lang.reflect.Proxy;

@Component
public class WinterBeanPostProcessorImpl implements BeanPostProcessor {

    /**
     * 每个bean初始化之前
     *
     * @param bean
     * @param beanName
     * @return {@link Object }
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("\t容器中的bean[" + beanName + "]初始化之前 postProcessBeforeInitialization执行！");
        return bean;
    }

    /**
     * 每个bean初始化之后
     *
     * @param bean
     * @param beanName
     * @return {@link Object }
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("\t容器中的bean[" + beanName + "]初始化之后 postProcessBeforeInitialization执行！");
        //如果当前bean是userService，则创建一个代理对象并返回
        if ("userService".equals(beanName)) {
            return Proxy.newProxyInstance(WinterBeanPostProcessorImpl.class.getClassLoader(), bean.getClass().getInterfaces(), (proxy, method, args) -> {
                System.out.println("\t切面逻辑...");
                return method.invoke(bean, args);
            });
        }
        return bean;
    }
}
