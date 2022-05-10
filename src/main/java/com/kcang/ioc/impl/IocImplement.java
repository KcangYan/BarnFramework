package com.kcang.ioc.impl;

import com.kcang.exception.IocInstanceRepeatException;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.annotation.Barn;
import com.kcang.ioc.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class IocImplement {
    private Logger myLogger = LoggerFactory.getLogger(IocImplement.class);

    //存放带有bean注解方法的类
    private static HashMap<String, Class> barnClassMap = new HashMap<>();

    //遍历所有类，初始化ioc容器
    public void init(List<Class> classList) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, IocInstanceRepeatException, IocWithoutInstanceException {
        for (Class cls : classList) {
            iocConfiguration(cls);
        }
        iocMethodBarnImpl();
    }
    /**
     * 将带有IocAdmin annotationList 列表里注解的类实例化
     * @param cls 反射类
     */
    private void iocConfiguration(Class cls) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, IocInstanceRepeatException {
        String instanceName = IocAdmin.getIocInstanceName(cls);
        //判断是否需要实例化托管容器
        if(instanceName != null){
            instanceName = instanceName.equals("")? cls.getSimpleName():instanceName;
            Object instance = IocAdmin.newInstance(cls);
            IocAdmin.addIocInstance(instanceName, instance);
            myLogger.debug("实例化并写入容器 "+instanceName+" 成功");
            //查找带bean注解的类
            Method[] methods = cls.getMethods();
            for(Method method : methods){
                Barn barn = method.getDeclaredAnnotation(Barn.class);
                if(barn != null){
                    barnClassMap.put(instanceName, cls);
                    break;
                }
            }
        }
    }

    /**
     * 实例化@Barn注解的方法 托管容器
     * @throws IllegalAccessException
     * @throws IocWithoutInstanceException
     * @throws InvocationTargetException
     * @throws IocInstanceRepeatException
     */
    private void iocMethodBarnImpl() throws IllegalAccessException, IocWithoutInstanceException, InvocationTargetException, IocInstanceRepeatException {
        for(String instanceName : barnClassMap.keySet()){
            Class cls = barnClassMap.get(instanceName);
            InjectInstanceImpl.injectConfigurationBarn(instanceName, cls);
            Method[] methods = cls.getMethods();
            for(Method method : methods){
                Barn barn = method.getDeclaredAnnotation(Barn.class);
                if(barn != null){
                    String beanName = barn.name().equals("")? method.getReturnType().getSimpleName() : barn.name();
                    //判断是否之前执行inject的时候 创建过，创建过就不再实例化一遍了
                    if(!IocAdmin.checkIocInstance(beanName)){
                        Object beanInstance = method.invoke(IocAdmin.getIocInstance(instanceName));
                        IocAdmin.addIocInstance(beanName, beanInstance);
                        myLogger.debug("实例化并写入容器 "+beanName+" 成功");
                    }
                }
            }
        }
    }

    /**
     * 为解决交替依赖，提前实例化一些类 因为他们在其他带有@Bean方法的类里被注入
     * 根据传入的beanName检索所有@Bean方法 是否有这个类，有就把它实例化
     * 当然循环依赖并不能完全解决 只能解决部分
     * 比如 A 依赖 B 才能实例化 同时 B 依赖 A 才能实例化 这种情况 是肯定只能报错了
     * 如果 A 依赖 B 但B为null也可以实例化，那么 B 依赖 A实例化过程中的A实例就是一个“正常实例” 但A中依赖B实例的属性就是空的 但B可以正常创建
     * 等B正常创建以后 又会对容器里所有的实例重新执行一遍新的inject过程 此时 A中依赖的B就是正常的B实例
     * @param barnName
     * @return
     */
    static Object createBeanInstance(String barnName) throws IocWithoutInstanceException, InvocationTargetException, IllegalAccessException, IocInstanceRepeatException {
        for(String instanceName : barnClassMap.keySet()){
            Class cls = barnClassMap.get(instanceName);
            Method[] methods = cls.getMethods();
            for(Method method : methods){
                Barn barn = method.getDeclaredAnnotation(Barn.class);
                if(barn != null){
                    String getBarnName = barn.name().equals("")? method.getReturnType().getSimpleName() : barn.name();
                    if(barnName.equals(getBarnName)){
                        Object instance =  method.invoke(IocAdmin.getIocInstance(instanceName));
                        IocAdmin.addIocInstance(barnName, instance);
                        return instance;
                    }
                }
            }
        }
        return null;
    }
}
