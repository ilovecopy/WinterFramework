package com.demo.winterframework;

import com.demo.winterframework.spring.config.impl.WinterConfigurationImpl;
import com.demo.winterframework.spring.context.impl.WinterContextImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WinterFrameworkApplication {

    public static void main(String[] args) {
        new WinterContextImpl(WinterConfigurationImpl.class);
    }

}
