package com.demo.winterframework.service;

import com.demo.winterframework.spring.annotation.Autowired;
import com.demo.winterframework.spring.annotation.Component;
import com.demo.winterframework.spring.annotation.Scope;
import com.demo.winterframework.spring.enums.ScopeEnum;
import com.demo.winterframework.spring.interfaces.InitializingBean;
import org.springframework.beans.factory.BeanNameAware;

@Component
@Scope(scope = ScopeEnum.单例)
public class UserService implements BeanNameAware, InitializingBean, UserServiceInterface {

    @Autowired
    private UserDao userDao;
    @Override
    public void test() {

    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("userService bean初始化方法被执行！");
    }

    @Override
    public void setBeanName(String beanName) {
        System.out.println("useService的BeanNameAware执行：" + beanName);
    }
}
