package com.demo.winterframework;

public class Utils {

    public static void printString(int number, String target) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < number; i++) {
            stringBuffer.append(target);
        }
        System.out.println("\t" + stringBuffer);
    }
}
