package com.demo.winterframework.spring.interfaces;

public interface InitializingBean {
    /**
     * 设置属性前执行
     */
    void afterPropertiesSet();
}
