package com.demo.winterframework.spring;


import com.demo.winterframework.spring.enums.ScopeEnum;
import lombok.Data;

/**
 * bean的定义类
 *
 * @author tangjincheng
 * @date 2025/03/08
 */
@Data
public class BeanDefinition {
    /**
     * bean的类型
     */
    private Class clazz;

    /**
     * 单例还是多例
     */
    private ScopeEnum scope;
}
