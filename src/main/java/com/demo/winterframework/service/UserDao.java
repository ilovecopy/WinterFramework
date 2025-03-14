package com.demo.winterframework.service;

import com.demo.winterframework.spring.annotation.Autowired;
import com.demo.winterframework.spring.annotation.Component;

/**
 * 利用注解指定bean名称
 * @author tangjincheng
 * @date 2025/03/08
 */
@Component
public class UserDao {
    @Autowired
    private UserService userService;
}
