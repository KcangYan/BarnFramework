package com.kcang;

import com.kcang.aop.impl.AopAnnotationImplement;
import com.kcang.aop.realize.async.AsyncRunnableImpl;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.external.MainRunnable;
import com.kcang.ioc.impl.InjectInstanceImpl;
import com.kcang.ioc.impl.IocAdmin;
import com.kcang.ioc.impl.IocImplement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BarnFramework {
    private BarnFramework(){}

    private Logger myLogger = LoggerFactory.getLogger(this.getClass());

    public static void run(Class Main, String[] args){
        new BarnFramework().run(Main);
    }
    /**
     * 初始化全局配置
     * 目前发现在一些maven打包插件打的jar包下运行时 会得不到Main入口类的系统路径
     * 建议使用spring-boot-maven-plugin打包 不会有问题
     * @param Main 主入口类
     */
    private void run(Class Main){
        String pag = Main.getPackage().getName();
        myLogger.debug("开始扫描包 "+pag);
        URL classpath = Thread.currentThread().getContextClassLoader().getResource("");
        URL path = Thread.currentThread().getContextClassLoader().getResource(pag.replace(".", "/"));
        if(path == null || classpath == null){
            myLogger.error("获取URL路径失败，无法扫描包！");
            myLogger.error("MiniFramework start fail !");
            return;
        }
        String protocol = path.getProtocol();
        List<Class> classList = new ArrayList<>();
        if("jar".equalsIgnoreCase(protocol)){
            doScan(path, pag, classList);
        }else {
            doScan(new File(path.getPath()), classpath.getPath(), classList);
        }

        //内部实现的aop注解实现类
        //异步注解实现类
        classList.add(AsyncRunnableImpl.class);

        try {
            //初始化ioc容器，先创建所有需要托管的bean，以便其他注解方法获取使用
            IocImplement iocImplement = new IocImplement();
            iocImplement.init(classList);

            //AOP实现
            AopAnnotationImplement annotationImplement = new AopAnnotationImplement();
            annotationImplement.init(classList);

            //注入属性，需方在aop注解方法后面，不然注入了属性的实例会被aop的是动态代理生成的类替换掉
            InjectInstanceImpl injectInstanceImpl = new InjectInstanceImpl();
            injectInstanceImpl.init(IocAdmin.getIocInstanceNames());

            //执行继承了MainRunnable的启动类，需放在最后
            mainRunnable(classList);
        }catch (Exception e){
            e.printStackTrace();
            myLogger.error("MiniFramework start fail！");
        }
    }

    /**
     * 根据mainRunnableList 按顺序执行run方法
     * @param classList
     */
    private void mainRunnable(List<Class> classList) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IocWithoutInstanceException {
        List<MainRunnable> mainRunnableList = new ArrayList<>();
        for(Class cls : classList){
            String instanceName = IocAdmin.getIocInstanceName(cls);
            if(instanceName != null){
                mainRunnableOrderHandler(IocAdmin.getIocInstance(instanceName), mainRunnableList);
            }
        }
        if(mainRunnableList.size() == 0){
            throw new RuntimeException("没有找到主驱动方法，请继承MainRunnable类实现run方法");
        }
        for(MainRunnable mainRunnable : mainRunnableList){
            myLogger.debug("启动 "+mainRunnable.getClass().getName()+" order="+mainRunnable.order);
            mainRunnable.run();
        }

    }
    /**
     * 对MainRunnable子类排序 order从小到大
     * @param obj
     * @param mainRunnableList
     */
    private void mainRunnableOrderHandler(Object obj, List<MainRunnable> mainRunnableList){
        if(obj instanceof MainRunnable){
            MainRunnable mainRun = (MainRunnable) obj;
            int len = mainRunnableList.size();
            if(len == 0){
                mainRunnableList.add(mainRun);
            }else {
                //对order排序，按顺序执行mainRunnable
                if(mainRun.order <= mainRunnableList.get(0).order){
                    mainRunnableList.add(0,mainRun);
                }else if(mainRun.order > mainRunnableList.get(len-1).order){
                    mainRunnableList.add(mainRun);
                }else {
                    for(int i=1; i<len; i++){
                        if(mainRun.order >= mainRunnableList.get(i-1).order && mainRun.order <= mainRunnableList.get(i).order){
                            mainRunnableList.add(i,mainRun);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 扫描jar包下的类
     * @param path jar包环境中的路径
     * @param pag 包名
     * @param classes 类集合
     */
    private void doScan(URL path, String pag, List<Class> classes){
        try {
            JarURLConnection connection = (JarURLConnection) path.openConnection();
            JarFile jarFile = connection.getJarFile();
            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
            while (jarEntryEnumeration.hasMoreElements()){
                JarEntry entry = jarEntryEnumeration.nextElement();
                String jarEntryName = entry.getName();
                if (jarEntryName.contains(".class") && jarEntryName.replaceAll("/",".").startsWith(pag)){
                    String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replace("/", ".");
                    Class cls = Class.forName(className);
                    classes.add(cls);
                }
            }
        }catch (Exception e){
            myLogger.error("jar包扫描失败！"+e.getMessage());
            e.printStackTrace();
            myLogger.error("MiniFramework start fail !");
        }
    }

    /**
     * 扫描非jar包模式下 即普通文件模式下的类
     * @param file 文件对象
     * @param classpath 系统路径
     * @param classes 类集合
     */
    private void doScan(File file, String classpath, List<Class> classes){
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            assert files != null : "files path null!";
            for (File f1 : files) {
                doScan(f1, classpath, classes);
            }
        } else {
            if (file.getName().endsWith(".class")) {
                String s = file.getPath();
                s = s.replace(classpath.replace("/","\\").replaceFirst("\\\\",""),"").replace("\\",".").replace(".class","");
                try {
                    Class cls = Class.forName(s);
                    classes.add(cls);
                    //判断类是否是AnnotationImplement的子类
                    //if(AnnotationImplement.class.isAssignableFrom(cls) && cls != AnnotationImplement.class){}
                }catch (Exception e){
                    myLogger.error("包类创建失败！"+e.getMessage());
                    e.printStackTrace();
                    myLogger.error("MiniFramework start fail !");
                }
            }
        }
    }
}
