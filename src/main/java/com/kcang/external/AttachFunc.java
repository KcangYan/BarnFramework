package com.kcang.external;

import com.kcang.aop.impl.AopAnnotationImplement;
import com.kcang.aop.impl.AopCglibEnhancer;
import com.kcang.aop.impl.AopMethodInterceptor;
import com.kcang.aop.impl.AopMethodProxy;
import com.kcang.exception.IocInstanceRepeatException;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.annotation.InjectInstance;
import com.kcang.ioc.impl.IocAdmin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 这个类里放一些附带功能
 */
public class AttachFunc {
    /**
     * 在框架运行时创建实例
     * 用这个方法创建的实例会执行类中的注解 Inject 和 aop注解
     * @param cls 类
     * @return 实例
     */
    public static Object newInstance(Class cls){
        Object obj = IocAdmin.newInstance(cls);
        try {
            obj = aopImpl(obj);
            injectInstance(obj);
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * 实现自定义aop注解 内部使用
     * @param obj 实现对象
     * @return 代理对象
     */
    private static Object aopImpl(Object obj) throws IocWithoutInstanceException {
        Method[] methods = obj.getClass().getDeclaredMethods();
        AopMethodInterceptor methodInterceptor = null;
        Object cglibObj = null;
        for(Method method : methods){
            for(String aopProxyName : AopAnnotationImplement.aopAnnotationMap.keySet()){
                Class annotationCls = AopAnnotationImplement.aopAnnotationMap.get(aopProxyName);
                Object annotation = method.getDeclaredAnnotation(annotationCls);
                if(annotation != null){
                    if(methodInterceptor == null){
                        methodInterceptor = new AopMethodInterceptor();
                        cglibObj = AopCglibEnhancer.cglibEnhancer(obj.getClass(), methodInterceptor);
                    }
                    methodInterceptor.setAopMethodMap(method, (AopMethodProxy) IocAdmin.getIocInstance(aopProxyName));
                    methodInterceptor.setAnnotation(method, (AopMethodProxy) IocAdmin.getIocInstance(aopProxyName), annotation);
                }
            }
        }
        if(cglibObj == null){
            return obj;
        }else {
            return cglibObj;
        }
    }

    /**
     * 实现当前类的aop注解 并返回代理类对象实例
     * @param cls 类
     * @return 代理对象实例
     * @throws IocWithoutInstanceException
     */
    public static Object aopImpl(Class cls) throws IocWithoutInstanceException {
        Method[] methods = cls.getMethods();
        AopMethodInterceptor methodInterceptor = new AopMethodInterceptor();
        Object cglibObj = AopCglibEnhancer.cglibEnhancer(cls, methodInterceptor);
        for(Method method : methods){
            for(String aopProxyName : AopAnnotationImplement.aopAnnotationMap.keySet()){
                Class annotationCls = AopAnnotationImplement.aopAnnotationMap.get(aopProxyName);
                Object annotation = method.getDeclaredAnnotation(annotationCls);
                if(annotation != null){
                    methodInterceptor.setAopMethodMap(method, (AopMethodProxy) IocAdmin.getIocInstance(aopProxyName));
                    methodInterceptor.setAnnotation(method, (AopMethodProxy) IocAdmin.getIocInstance(aopProxyName), annotation);
                }
            }
        }
        return cglibObj;
    }

    /**
     * 注入类中有@InjectInstance注解的属性
     * @param obj 需要处理的对象实例
     * @throws IllegalAccessException
     * @throws IocInstanceRepeatException
     */
    public static void injectInstance(Object obj) throws IllegalAccessException, IocInstanceRepeatException {
        //Field[] fields = obj.getClass().getDeclaredFields();
        List<Field> fields = IocAdmin.getAllFields(obj.getClass());
        for(Field field : fields){
            InjectInstance injectInstance = field.getDeclaredAnnotation(InjectInstance.class);
            if(injectInstance != null){
                String beanNameInject = injectInstance.name().equals("")? field.getType().getSimpleName(): injectInstance.name();
                field.setAccessible(true);
                Object getInjectBean = null;
                try {
                    getInjectBean = IocAdmin.getIocInstance(beanNameInject);
                    field.set(obj, getInjectBean);
                }catch (IocWithoutInstanceException e){
                    //没有要注入的实例, 写到ioc中 此情景就是Bean的时候 需要创建这个类 但因为依赖关系没有办法注入所以放到容器里等待注入
                    IocAdmin.addIocInstance(obj+"Temp"+System.currentTimeMillis(), obj);
                }
            }
        }
    }

}
