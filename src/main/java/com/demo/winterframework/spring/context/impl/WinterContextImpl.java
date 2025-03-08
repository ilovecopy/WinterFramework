package com.demo.winterframework.spring.context.impl;

import com.demo.winterframework.Utils;
import com.demo.winterframework.spring.BeanDefinition;
import com.demo.winterframework.spring.annotation.Autowired;
import com.demo.winterframework.spring.annotation.Component;
import com.demo.winterframework.spring.annotation.ComponentScan;
import com.demo.winterframework.spring.annotation.Scope;
import com.demo.winterframework.spring.context.WinterContext;
import com.demo.winterframework.spring.enums.ScopeEnum;
import com.demo.winterframework.spring.interfaces.BeanPostProcessor;
import com.demo.winterframework.spring.interfaces.InitializingBean;
import org.springframework.beans.factory.BeanNameAware;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class WinterContextImpl implements WinterContext {

    /**
     * 配置类
     */
    private final Class configClazz;

    /**
     * bean定义map
     */
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionConcurrentHashMap = new ConcurrentHashMap<>();

    /**
     * 单例池
     */
    private ConcurrentHashMap<String, Object> singletonPool = new ConcurrentHashMap<>();

    /**
     * 实现了该接口的所有bean集合
     */
    private ArrayList<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    /**
     * 以配置类的形式创建一个容器
     * 扫描以配置类中设置的路径
     * -如果文件是字节码文件
     * 判断是否有component注解
     * 如果有 创建bean定义对象 添加到bean定义map中
     *
     * @param configClazz
     */
    public WinterContextImpl(Class configClazz) {
        System.out.println("容器构造方法执行！");
        this.configClazz = configClazz;
        //通过配置类ComponentScan注解获取需要扫描bean的路径
        if (configClazz.isAnnotationPresent(ComponentScan.class)) {
            //拿到该注解的值 即为需要扫描bean的路径
            ComponentScan componentScanAnnotation = (ComponentScan) configClazz.getAnnotation(ComponentScan.class);
            String componentPath = componentScanAnnotation.componentPath();

            componentPath = componentPath.replace(".", "/");

            ClassLoader classLoader = WinterContext.class.getClassLoader();

            URL resource = classLoader.getResource(componentPath);

            File file = new File(resource.getFile());

            System.out.println("需要扫描的路径：" + file.getAbsolutePath());

            if (file.isDirectory()) {
                //拿到所有子文件
                File[] files = file.listFiles();
                for (File f : files) {
                    //绝对路径
                    String absolutePath = f.getAbsolutePath();
                    System.out.println("扫描到文件的绝对路径" + absolutePath);
                    //如果是字节码文件
                    if (absolutePath.endsWith(".class")) {

                        String relativelyClassPath = absolutePath.substring(absolutePath.indexOf(componentPath), absolutePath.indexOf(".class"));
                        relativelyClassPath = relativelyClassPath.replace("/", ".");
                        Class<?> clazz;
                        try {
                            //通过上面的类加载器 用字节码的绝对路径加载字节码到内存中
                            clazz = classLoader.loadClass(relativelyClassPath);

                            //判断当前类是否有Component注解
                            if (clazz.isAnnotationPresent(Component.class)) {
                                System.out.println("\t类：" + clazz + "有Component注解 需要我们加入容器中！");
                                // 判断是否实现了BeanPostProcessor接口
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                    try {
                                        beanPostProcessors.add((BeanPostProcessor) clazz.newInstance());
                                    } catch (InstantiationException | IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                //获取bean名称
                                Component componentAnnotation = clazz.getAnnotation(Component.class);
                                String beanName = componentAnnotation.beanName();
                                if (Objects.isNull(beanName) || "".equals(beanName)) {
                                    // 获取全类名
                                    String allPackageName = clazz.getName();
                                    beanName = allPackageName.substring(allPackageName.lastIndexOf(".") + 1);
                                    //首字母转小写
                                    beanName = Introspector.decapitalize(beanName);
                                }
                                //生成bean定义对象
                                BeanDefinition beanDefinition = new BeanDefinition();
                                //设置类型
                                beanDefinition.setClazz(clazz);

                                //单例还是多例
                                //如果有scope注解
                                if (clazz.isAnnotationPresent(Scope.class)) {
                                    //获取注解对象
                                    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                    //设置为注解中标注的值
                                    beanDefinition.setScope(scopeAnnotation.scope());
                                } else {
                                    //如果没有指定 默认为单例
                                    beanDefinition.setScope(ScopeEnum.单例);
                                }
                                //添加到bean定义map中
                                beanDefinitionConcurrentHashMap.put(beanName, beanDefinition);
                            } else {
                                System.out.println("\t" + clazz.getName() + "没有Component注解，不需要创建！");
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                Utils.printString(60, "-");
            }

            //创建bean定义map中的所有单例bean
            System.out.println("\n\t 创建所有的单例bean：\n\t");
            for (String beanName : beanDefinitionConcurrentHashMap.keySet()) {
                BeanDefinition beanDefinition = beanDefinitionConcurrentHashMap.get(beanName);
                if (beanDefinition.getScope().equals(ScopeEnum.单例)) {
                    // 创建bean
                    Object bean = createBean(beanName, beanDefinition);
                    //添加到单例池
                    singletonPool.put(beanName, bean);
                    System.out.println("\n\t单例bean[" + beanName + "]创建成功，并放入单例池中！");
                }
            }

        }
    }

    /**
     * 利用反射创建一个bean
     *
     * @param beanName
     * @param beanDefinition
     * @return {@link Object }
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        System.out.println("\n\t " + beanName + " 开始创建");
        Class clazz = beanDefinition.getClazz();

        //获取clazz对应的无参构造
        Constructor constructor = null;
        try {
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            System.out.println(beanName + "没有无参构造方法！创建失败");
        }
        //通过构造方法创建bean实例
        Object instance = null;
        try {
            assert constructor != null;
            instance = constructor.newInstance();

            //解决bean的依赖注入
            //获取所有属性
            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                //如果当前属性存在Autowired注解
                if (field.isAnnotationPresent(Autowired.class)) {
                    //设置属性可强制读取
                    field.setAccessible(true);
                    //通过属性名去容器中获取对应的bean
                    Object bean = getBeanByName(field.getName());
                    //设置实例的该属性的值为容器中对应bean
                    field.set(instance, bean);
                    System.out.println("\t容器中的bean[" + bean + "]自动注入到bean[" + instance.getClass().getName() + "]的属性[" + field.getName() + "]中。\n");
                }
            }
            if (instance instanceof BeanNameAware) {
                //调用对应的setBeanName方法
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            //初始化每个bean之前 执行所有的前置处理器
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }
            //初始化
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }
            //初始化每个bean之后 执行所有的后置处理器
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }
            System.out.println("\t" + beanName + " 创建完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    @Override
    public Object getBeanByName(String beanName) {
        //通过bean名称获取对应bean定义
        BeanDefinition beanDefinition = beanDefinitionConcurrentHashMap.get(beanName);

        if (beanDefinition == null) {
            throw new NullPointerException("容器中没有名称为[" + beanName + "]的bean！");
        }
        //判断单例还是多例
        ScopeEnum scopeEnum = beanDefinition.getScope();
        if (scopeEnum.equals(ScopeEnum.单例)) {
            //如果是单例 从单例池中获取
            Object bean = singletonPool.get(beanName);
            //如果单例池不存在
            if (bean == null) {
                //创建
                bean = createBean(beanName, beanDefinition);
                //放入单例池
                singletonPool.put(beanName, bean);
            }
            //返回
            return bean;
        } else if (scopeEnum.equals(ScopeEnum.多例)) {
            //如果是多例 创建一个bean
            Object bean = createBean(beanName, beanDefinition);
            System.out.println("获取多例bean：" + beanName + "，创建成功！");
            //返回
            return bean;
        }

        return null;
    }

    @Override
    public Object getBeanByClass(Class clazz) {
        for (Map.Entry<String, BeanDefinition> beanDefinitionEntry : beanDefinitionConcurrentHashMap.entrySet()) {
            //map中的bean定义对象
            BeanDefinition beanDefinition = beanDefinitionEntry.getValue();
            //bean名称
            String beanName = beanDefinitionEntry.getKey();
            //如果匹配到了需要bean类型
            if (beanDefinition.getClazz().equals(clazz)) {
                //通过名称获取bean
                return getBeanByName(beanName);
            }
        }
        throw new NullPointerException("容器中没有类型为：" + clazz.getName() + "的bean");
    }
}
