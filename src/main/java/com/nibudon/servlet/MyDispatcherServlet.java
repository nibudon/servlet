package com.nibudon.servlet;

import com.nibudon.annotation.MyAutoWired;
import com.nibudon.annotation.MyController;
import com.nibudon.annotation.MyRequestMapping;
import com.nibudon.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    private Map<String,Object> ioc = new HashMap<String,Object>();

    private List<String> classNames = new ArrayList<String>();

    private Map<String,Method> handllerMapping = new HashMap<String,Method>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1，加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2，ioc容器初始化,扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3，初始化所有的相关类的实例，并将其保存到ioc容器中
        doInstance();

        //4，完成依赖注入
        doAutoWired();
        
        //5，初始化HandllerMapping
        initHandllerMapping();

        System.out.println("MY SpringFramework inited!");

    }

    private void initHandllerMapping() {
        if(ioc == null){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyController.class)){continue;}

            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(MyRequestMapping.class)){continue;}

                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = requestMapping.value();
                url = ("/" + baseUrl + "/" + url).replaceAll("/+","/");
                handllerMapping.put(url,method);
                System.out.println("Maped : " + url +"," +method);
            }
        }
    }

    private void doAutoWired() {
        if(ioc == null) {return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //包括了私有参数
            Field [] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(MyAutoWired.class)){continue;}

                MyAutoWired autoWired = field.getAnnotation(MyAutoWired.class);

                String beanName = autoWired.name();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                //只要加了注解，私有的属性一样可以被注入
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void doInstance() {
        if(classNames == null){
            return;
        }
        try {
            for (String className : classNames) {
                //如果是自定义注解，需要获取注解的值
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                } else if(clazz.isAnnotationPresent(Service.class)){
                    //1，默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //2，自定义命名要自定义命名优先
                    Service service = clazz.getAnnotation(Service.class);
                    if(!"".equals(service.name())){
                        beanName = service.name();
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    //3，如果是接口，创建的是实现类的对象
                    for (Class<?> i : clazz.getInterfaces()) {
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("the beanName is exist!");
                        }
                        ioc.put(i.getName(),instance);
                    }
                } else{
                    continue;
                }
            }
        } catch (Exception e){

        }

    }

    private String toLowerFirstCase(String simpleName) {
        char []chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());
        for(File file : classPath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else{
                //拿这个类名反射，通过Class.forName
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className = (scanPackage + "." + file.getName()).replaceAll(".class","");
                classNames.add(className);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6，等待用户请求
       try{
           doDispatcher(req,resp);
       } catch (Exception e){
           e.printStackTrace();
           resp.getWriter().write("500 : server error !");
       }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        if(!handllerMapping.containsKey(url)){
            resp.getWriter().write("404 : not found !");
            return;
        }

        Map<String,String []> params = req.getParameterMap();

        Method method = this.handllerMapping.get(url);

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),req,resp,params.get("name")[0]);
    }

    /*@Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println(0000000);
    }*/

    @Override
    public void destroy() {
        System.out.println(9999);
    }
    


}
