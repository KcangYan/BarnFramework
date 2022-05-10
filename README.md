自研的一个微型java开发框架

## 主启动类入口

    import com.kcang.main.MiniFramework;
    
    public class ApplicationStart {
        public static void main(String[] args) {
            BarnFramework.run(ApplicationStart.class, args);
        }
    }
框架会根据ApplicationStart.class所在的包的路径，去扫描包下所有的类来实现具体的注解和方法。

## 添加主启动方法

    @ManagedInstance
    public class Test20220324 extends MainRunnable {
        private static Logger myLogger = LoggerFactory.getLogger(Test20220324.class);
    
        public Test20220324(){
                super.order = 2;
            }
    
        @Override
        public void run() {
            run("另一个run");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            myLogger.info("Test20220324");
        }
    
        public void run(String name){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            myLogger.info(name);
        }
    }
通过继承抽象类MainRunnable 实现run方法，并将该类通过@ManagedInstance注解交给容器托管即可。
当在主启动类的包下扫描到继承了MainRunnable类的子类则会运行该类下的run()方法

## ico 单例管理容器

### 1. 容器托管使用方法

    import com.kcang.annotation.ioc.bean.ManagedInstance;
    import com.kcang.annotation.ioc.InjectInstance;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    
    @ManagedInstance
    public class Test20220326_1 {
    
        private static Logger myLogger = LoggerFactory.getLogger(Test20220326_1.class);
        @InjectInstance
        private Test20220326_2 test20220326_2;
    
        public void run(){
            myLogger.info(" test 20220326-1 " + test20220326_2);
        }
    }

通过@ManagedInstance 注解在类上方标识该注解 同时也可以添加name属性自定义bean单例名称

    import com.kcang.annotation.ioc.bean.ManagedInstance;
    import com.kcang.annotation.ioc.InjectInstance;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    
    @ManagedInstance(name = "Test20220326_1")
    public class Test20220326_1 {
    
        private static Logger myLogger = LoggerFactory.getLogger(Test20220326_1.class);
        @InjectInstance
        private Test20220326_2 test20220326_2;
    
        public void run(){
            myLogger.info(" test 20220326-1 " + test20220326_2);
        }
    }
    
也可以通过声明配置类托管

     @Configuration
     public class ConfigBean {
         @Barn(name = "TestHashMap")
         public HashMap<String, String> hashMap(){
             return new HashMap<String, String>();
         }
         @Barn(name = "TestArrayList")
         public List list(){
             return new ArrayList();
         }
     }
     
还有@Component @Service等注解 也是将当前类托管至ioc容器 

### 2. 单例实例的使用

    @InjectInstance
    private Test20220326_2 test20220326_2;
    
    @InjectInstance(name="Test20220326_2")
    private Test20220326_2 test20220326_2;
    
可以通过在属性对象上方添加注解@InjectBean 使用。会将容器中的单例注入到属性中，直接使用即可

### 3. 使用注意

    所有的Aop 注解和自定义实现 以及 @InjectInstance 都依赖于该实例对象是托管至ioc容器中的实例。
    如果所需要的对象必须不能是单例的话 有两种使用办法 
    1. 利用@Bean注解 把所有需要的对象都一一Bean出来到容器里去用，这样aop 和 ioc的注解都不影响
    2. 使用AttachFunc.newInstance(Class cls)方法创建对象，该方法创建的对象会注入bean到属性里使用 并且实现aop注解

## Aop 方法代理注解实现

声明注解类

     import java.lang.annotation.ElementType;
     import java.lang.annotation.Retention;
     import java.lang.annotation.RetentionPolicy;
     import java.lang.annotation.Target;
     
     @Retention(RetentionPolicy.RUNTIME)
     @Target(value = {ElementType.METHOD})
     public @interface Test1 {
         String value() default "";
     }
     
继承AopMethodProxy抽象类 并在类上方使用注解@AopAnnotationImpl，并将该类指向你自定义的注解作为该注解的实现类
可以通过order来提高当前注解的执行顺序，order越小执行顺序越高，默认是10

     @AopAnnotationImpl(AopAnnotation = Test1.class, order=5)
     public class Test1Impl extends AopMethodProxy {
         private static Logger myLogger = LoggerFactory.getLogger(Test1Impl.class);
     
         private Test1 test1;
     
         @Override
         public void setAnnotation(Object annotation) {
             this.test1 = (Test1) annotation;
         }
     
         @Override
         public void before(Object[] args) {
             myLogger.info(Arrays.toString(args));
         }
     
         @Override
         public void after(Object result) {
     
         }
     }
     
具体方法的使用和定义，可以去AopMethodProxy类里面去看注释

当多个aop代理实现类代理同一个方法，需要在实现类的注解@AopAnnotationImpl(AopAnnotation = Test1.class, order = 1)
中增加order属性，order越小 层级越高
     
     @AopAnnotationImpl(AopAnnotation = Test1.class, order = 1)
     public class Test1Impl extends AopMethodProxy {
         private static Logger myLogger = LoggerFactory.getLogger(Test1Impl.class);
     
         private Test1 test1;
     
         @Override
         public void setAnnotation(Object annotation) {
             this.test1 = (Test1) annotation;
         }
     
     
         @Override
         public void before(Object[] args) {
             myLogger.info("Test1 before "+Arrays.toString(args));
         }
     
         @Override
         public Object around(AopMethod aopMethod) throws Throwable {
             myLogger.info("Test1 around1");
             Object result = aopMethod.methodRun();
             myLogger.info("Test1 get result " + result);
             myLogger.info("Test1 around2");
             return "Test1";
         }
     
         @Override
         public void after(Object result) {
             myLogger.info("Test1 after "+result);
         }
     }
      
     @AopAnnotationImpl(AopAnnotation = Test2.class, order = 2)
     public class Test2Impl extends AopMethodProxy {
         private static Logger myLogger = LoggerFactory.getLogger(Test2Impl.class);
     
         private Test2 test2;
     
         @Override
         public void setAnnotation(Object annotation) {
             this.test2 = (Test2) annotation;
         }
     
         @Override
         public void before(Object[] args) {
             myLogger.info("Test2 before "+ Arrays.toString(args));
         }
     
         @Override
         public Object around(AopMethod aopMethod) throws Throwable {
             myLogger.info("Test2 around1");
             Object result = aopMethod.methodRun();
             myLogger.info("Test2 get result " + result);
             myLogger.info("Test2 around2");
             return "Test2";
         }
     
         @Override
         public void after(Object result) {
             myLogger.info("Test2 after "+result);
         }
     }
     
     @AopAnnotationImpl(AopAnnotation = Test3.class, order = 3)
     public class Test3Impl extends AopMethodProxy {
         private static Logger myLogger = LoggerFactory.getLogger(Test3Impl.class);
         @Override
         public void setAnnotation(Object annotation) {
     
         }
     
         @Override
         public void before(Object[] args) {
             myLogger.info("Test3 before "+ Arrays.toString(args));
         }
     
         @Override
         public Object around(AopMethod aopMethod) throws Throwable {
             myLogger.info("Test3 around1");
             Object result = aopMethod.methodRun();
             myLogger.info("Test3 get result " + result);
             myLogger.info("Test3 around2");
             return "Test3";
         }
     
         @Override
         public void after(Object result) {
             myLogger.info("Test3 after "+result);
         }
     }
     
     @Test1
     @Test2
     @Test3
     public String run(String s, int x, int y){
         myLogger.info("Test0330_1 " + s+" x="+x+" y="+y);
         return "Test0330_1 " + s+" x="+x+" y="+y;
     }
     
     2022-03-31 22:43:17.554 [main] INFO  com.kcang.test.annotation.Test1Impl - Test1 before [kcang, 1, 2]
     2022-03-31 22:43:17.554 [main] INFO  com.kcang.test.annotation.Test1Impl - Test1 around1
     2022-03-31 22:43:17.554 [main] INFO  com.kcang.test.annotation.Test2Impl - Test2 before [kcang, 1, 2]
     2022-03-31 22:43:17.554 [main] INFO  com.kcang.test.annotation.Test2Impl - Test2 around1
     2022-03-31 22:43:17.555 [main] INFO  com.kcang.test.annotation.Test3Impl - Test3 before [kcang, 1, 2]
     2022-03-31 22:43:17.555 [main] INFO  com.kcang.test.annotation.Test3Impl - Test3 around1
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.Test0330_1 - Test0330_1 kcang x=1 y=2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test3Impl - Test3 get result Test0330_1 kcang x=1 y=2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test3Impl - Test3 around2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test3Impl - Test3 after Test3
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test2Impl - Test2 get result Test3
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test2Impl - Test2 around2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test2Impl - Test2 after Test2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test1Impl - Test1 get result Test2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test1Impl - Test1 around2
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.annotation.Test1Impl - Test1 after Test1
     2022-03-31 22:43:17.564 [main] INFO  com.kcang.test.Test - Test1
     
修改代理方法入参

     aopMethod.setArgs(new Object[]{"test1", 1, 1});
     
当你想要你的aop实现类的入参修改作为最终代理方法的真正入参，则可以

     aopMethod.setArgs(new Object[]{"test1", 1, 1},this);
     
修改代理方法返回值则直接在around方法里return你的结果即可
如果你想要你的aop实现所返回的结果作为代理方法的最终返回，则可以

     aopMethod.setResult("Test3", this);

## 已实现的工具注解

       @AsyncRunnable
       @Override
       public void run() {
           run("另一个run");
           try {
               Thread.sleep(5000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
           myLogger.info("Test20220324");
       }

       @AsyncRunnable(type = AsyncType.Synchronous)
       public void run(String name){
           try {
               Thread.sleep(5000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
           myLogger.info(name);
       }
       
       //使用自定义的线程池代替默认的线程池
       @AsyncRunnable(type = AsyncType.CustomThreadPool, name = "ThreadPoolExecutor")
       public void run(String name){
           try {
               Thread.sleep(5000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
           myLogger.info(name);
       }       
       

@AsyncRunnable注解会将当前方法的运行交给内置线程池做异步运行，具体线程池可以去看GlobalThreadPoolExecutor类
的注解和线程池参数配置