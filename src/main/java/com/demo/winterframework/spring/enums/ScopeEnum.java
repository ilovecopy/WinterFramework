package com.demo.winterframework.spring.enums;

/**
 * @author tangjincheng
 * @date 2025/03/08
 */
public enum ScopeEnum {

    单例("单例", "singleton"),

    多例("多例", "prototype");
    /**
     * 名称
     */
    private String name;
    /**
     * 类型
     */
    private String scope;

    ScopeEnum(String name, String scope) {
        this.name = name;
        this.scope = scope;
    }
}
