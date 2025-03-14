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
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;

import java.beans.Introspector;
import java.io.File;
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
     * 单例池（一级缓存）
     */
    private final ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    /**
     * 二级缓存
     */
    private final ConcurrentHashMap<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    /**
     * 三级缓存
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);
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

                                //获取bean名称
                                Component componentAnnotation = clazz.getAnnotation(Component.class);
                                String beanName = componentAnnotation.beanName();
                                if (Objects.isNull(beanName) || beanName.isEmpty()) {
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
            // 创建所有 Bean 后处理器，放入 singletonObjects 容器中，并注册到 beanPostProcessorList
            registerBeanPostProcessors();
            // 将扫描到的单例 bean 创建出来放到单例池中
            preInstantiateSingletons();
        }
    }

    /**
     * 创建所有Bean后处理器，放入singletonObjects容器中，并注册到beanPostProcessorList
     * 在后续的preInstantiateSingletons()初始化单例中，会先从容器中获取，获取不到再创建
     * Bean后处理器属于单例，提前创建好了放入容器，所以Bean后处理器并不会重复创建
     */
    private void registerBeanPostProcessors() {
        registerCommonBeanPostProcessor();
         /*
           1. 从 beanDefinitionMap 中找出所有的 BeanPostProcessor
           2. 创建 BeanPostProcessor 放入容器
           3. 将创建的 BeanPostProcessor 注册到 beanPostProcessorList

           这里的写法：先注册的 BeanPostProcessor 会对后创建的 BeanPostProcessor 进行拦截处理，
           BeanPostProcessor 的创建走 bean 的生命周期流程
          */
        this.beanDefinitionConcurrentHashMap.entrySet().stream().filter((entry) -> BeanPostProcessor.class.isAssignableFrom(entry.getValue().getClazz())).forEach((entry) -> {
            BeanPostProcessor beanPostProcessor = (BeanPostProcessor) getBeanByName(entry.getKey());
            beanPostProcessors.add(beanPostProcessor);
        });
    }

    private void preInstantiateSingletons() {
        //将扫描到的单例bean创建出来放到单例池中
        System.out.println("\n\t 创建所有的单例bean：\n\t");

        beanDefinitionConcurrentHashMap.forEach((beanName, beanDefiniton) -> {
            if (beanDefiniton.getScope() == ScopeEnum.单例) {
                getBeanByName(beanName);
                System.out.println("\n\t单例bean[" + beanName + "]创建成功，并放入单例池中！");
            }
        });

    }

    /**
     * 注册常用的Bean后处理器到beanDefinitionMap中
     */
    private void registerCommonBeanPostProcessor() {
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setClazz(AnnotationAwareAspectJAutoProxyCreator.class);
        beanDefinition.setScope(ScopeEnum.单例);
        beanDefinitionConcurrentHashMap.put("internalAutoProxyCreator", beanDefinition);
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
        try {
            Object bean = clazz.getConstructor().newInstance();
            // 如果当前创建的是单例对象，依赖注入前将工厂对象 fa 存入三级缓存 singletonFactories 中
            if (beanDefinition.getScope() == ScopeEnum.单例) {
                System.out.println("🐶🐶🐶🐶 createBean：Eagerly caching bean '" + beanName + "' to allow for resolving potential circular references");
                this.singletonFactories.put(beanName, () -> {
                    Object exposedObject = bean;
                    for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                        if (beanPostProcessor instanceof SmartInstantiationAwareBeanPostProcessor) {
                            exposedObject = ((SmartInstantiationAwareBeanPostProcessor) beanPostProcessor).getEarlyBeanReference(exposedObject, beanName);
                        }
                    }
                    return exposedObject;
                });
                this.earlySingletonObjects.remove(beanName);
            }
            Object exposedObject = bean;
            populateBean(beanName, beanDefinition, bean);
            exposedObject = initilizeBean(beanName, beanDefinition, bean);
            //去二级缓存earlySingleObjects中查看有没有当前bean，
            //如果有，说明发生了循环依赖，返回缓存中的a对象（可能是代理对象也可能是原始对象，主要看有没有切点匹配到bean）
            if (beanDefinition.getScope() == ScopeEnum.单例) {
                Object earlySingletonReference = getSingleton(beanName, false);
                if (earlySingletonReference != null) {
                    exposedObject = earlySingletonReference;
                }
            }
            return exposedObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return singletonObjects;
    }

    /**
     * 依赖注入阶段，执行bean后处理器的postProcessProperties方法
     *
     * @param beanName
     * @param beanDefinition
     * @param instance
     */
    private void populateBean(String beanName, BeanDefinition beanDefinition, Object instance) throws IllegalAccessException {
        //解决bean的依赖注入
        System.out.println("😋😋😋😋 依赖注入阶段：" + beanName + ", class = " + instance.getClass().getName());
        //获取所有属性
        Field[] fields = beanDefinition.getClass().getDeclaredFields();
        for (Field field : fields) {
            //如果当前属性存在Autowired注解
            if (field.isAnnotationPresent(Autowired.class)) {
                //设置属性可强制读取
                field.setAccessible(true);
                //通过属性名去容器中获取对应的bean
                Object bean = getBeanByName(field.getName());
                //设置实例的该属性的值为容器中对应bean
                field.set(instance, bean);
            }
        }
    }

    /**
     * 初始化阶段，包含：Aware回调、初始化前、初始化、初始化后
     *
     * @param beanName
     * @param beanDefinition
     * @param instance
     * @return {@link Object }
     */
    private Object initilizeBean(String beanName, BeanDefinition beanDefinition, Object instance) {
        //各种Aware回调
        //回调是：告诉某个东西给对象
        if (instance instanceof BeanNameAware) {
            //调用对应的setBeanName方法
            ((BeanNameAware) instance).setBeanName(beanName);
        }

        //初始化每个bean之前 执行所有的后置处理器
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
        return instance;
    }

    /**
     * 尝试依次从3处缓存获取bean
     *
     * @param beanName
     * @param allowEarlyReference 是否应该创建早期引用。
     *                            bean 初始化后一个检查二级缓存是否提前创建了bean，此时 allowEarlyReference 为 false ，只检查到二级缓存即可
     * @return {@link Object }
     */
    private Object getSingleton(String beanName, boolean allowEarlyReference) {
        //一级缓存：单例池
        Object singletonObject = singletonObjects.get(beanName);
        if (singletonObject == null) {
            //二级缓存：提前创建的单例对象池
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                //三级缓存：单例工厂池
                ObjectFactory<?> objectFactory = this.singletonFactories.get(beanName);
                //如果三级缓存有bean，则将三级缓存bean移到二级缓存中
                if (objectFactory != null) {
                    singletonObject = objectFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
        return singletonObject;
    }

    /**
     * 先将beanDefinition扫描出来再进行创建，而不是边扫描边创建
     * 因为在createBean的时候，要进行依赖注入，需要看看有没有提供某个类的依赖，所以要先扫描后创建
     * 并将所有单例Bean放到单例池中
     *
     * @param beanName
     * @return {@link Object }
     */
    @Override
    public Object getBeanByName(String beanName) {
        //通过bean名称获取对应bean定义
        BeanDefinition beanDefinition = beanDefinitionConcurrentHashMap.get(beanName);

        if (beanDefinition == null) {
            throw new NullPointerException("容器中没有名称为[" + beanName + "]的bean！");
        } else {
            if (beanDefinition.getScope() == ScopeEnum.单例) {
                Object singletonObject = getSingleton(beanName, true);
                //如果三处缓存都没有某个bean，只能create了
                if (singletonObject == null) {
                    singletonObject = createBean(beanName, beanDefinition);
                    singletonObjects.put(beanName, singletonObject);
                    earlySingletonObjects.remove(beanName);
                    singletonFactories.remove(beanName);
                }
                //返回
                return singletonObject;
            } else {
                //如果是多例 创建一个bean
                Object bean = createBean(beanName, beanDefinition);
                System.out.println("获取多例bean：" + beanName + "，创建成功！");
                //返回
                return bean;
            }
        }
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
