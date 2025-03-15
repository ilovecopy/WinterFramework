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
    @Autowired
    public void setUserService(UserService userService) {
        System.out.println("useDao依赖注入setUserService"+userService.getClass().getName());
        this.userService = userService;
    }
    public void print(){
        System.out.println("userDao.print");
    }
}
