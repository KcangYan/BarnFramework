package com.kcang.aop.impl;


/**
 * 如果需要实现自定义注解 只需要实现这个接口 并且在实现类上添加 @AopAnnotationImpl这个注解即可
 */
public abstract class AopMethodProxy {
    /**
     * 代理方法执行序列，默认1 从小到大执行
     */
    private int order = 10;
    void setOrder(int order){
        this.order = order;
    }
    int getOrder(){
        return this.order;
    }
    /**
     * 这个方法会把当前的注解对象传给你，如果要取当前注解的属性可以从这里获取
     * @param annotation  当前注解对象
     */
    public abstract void setAnnotation(Object annotation);
    /**
     * 方法执行前
     * @param args 代理方法的入参
     */
    public abstract void before(Object[] args);

    /**
     * 方法给你运行，执行aopMethod.methodRun() 就是运行被你代理的方法入参修改可以到aopMethod的属性里直接修改
     *  当你return对象时，则标识你这一层aop向上一层抛出了你的返回值交给上一层aop代理处理。
     *  也可以在return之前 通过setResult方法强制修改最终返回值为你的
     * @param aopMethod 代理方法对象
     * @return 方法运行的返回值（如果有的话）
     */
    public Object around(AopMethod aopMethod) throws Throwable{
        return aopMethod.methodRun();
    }

    /**
     * 方法运行结束后
     * @param result 代理方法的出参
     */
    public abstract void after(Object result);
}
