package com.demo.winterframework.service;

public class CalculatorImpl implements Calculator {


    @Override
    public int add(int i, int j) {
        System.out.println("add开始，参数是" + i + j);
        int result = i + j;
        System.out.println("方法内部result=" + result);
        System.out.println("add结束，参数是" + i + j);
        return result;
    }

    @Override
    public int sub(int i, int j) {
        System.out.println("sub开始，参数是" + i + j);
        int result = i - j;
        System.out.println("方法内部result=" + result);
        System.out.println("sub结束，参数是" + i + j);
        return result;
    }

    @Override
    public int mul(int i, int j) {
        System.out.println("mul开始，参数是" + i + j);
        int result = i * j;
        System.out.println("方法内部result=" + result);
        System.out.println("mul结束，参数是" + i + j);
        return result;
    }

    @Override
    public int div(int i, int j) {
        System.out.println("div开始，参数是" + i + j);
        int result = i / j;
        System.out.println("方法内部result=" + result);
        System.out.println("div结束，参数是" + i + j);
        return result;
    }
}
