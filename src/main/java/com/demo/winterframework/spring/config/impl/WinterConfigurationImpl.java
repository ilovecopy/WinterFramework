package com.demo.winterframework.spring.config.impl;

import com.demo.winterframework.spring.annotation.ComponentScan;
import com.demo.winterframework.spring.config.WinterApplicationConfig;

@ComponentScan(componentPath = "com.demo.winterframework.service")
public class WinterConfigurationImpl implements WinterApplicationConfig {
}
