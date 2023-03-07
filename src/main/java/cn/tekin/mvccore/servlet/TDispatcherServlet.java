package cn.tekin.mvccore.servlet;

import cn.tekin.mvccore.annotation.*;
import cn.tekin.utils.StrUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 *
 * 自定义MVC Framework Servlet
 *
 * @author tekintian@gmail.com
 * @version v0.0.1
 * @since v0.0.1 2023-03-07 15:18
 */
public class TDispatcherServlet extends HttpServlet {

    private Properties p=new Properties();

    private List<String> classNames=new ArrayList<String>();

    private Map<String,Object> ioc=new HashMap<String,Object>();

    private Map<String, MyHandler> handlerMapping=new HashMap<String,MyHandler>();
    //private List<MyHandler> handlerMapping=new ArrayList<MyHandler>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        // 1. 加载配置信息
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));

        // 2. 扫描所有相关的类
        doScanner(p.getProperty("scanPackage"));

        //3. 实例化所有相关的类,并且将其保存到IOC容器中
        doInstance();

        // 4. 依赖注入
        doAutoWrited();

        // 5. 初始化 HandlerMapping
        initHandlerMapping();
    }

    /**
     * 加载配置信息
     * @param location
     */
    private void doLoadConfig(String location) {
        InputStream fis=null;
        try {
            fis=this.getClass().getClassLoader().getResourceAsStream(location);
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描所有相关的类
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {

        URL url = this.getClass().getClassLoader().getResource(
                "/"+scanPackage.replaceAll("\\.","/"));

        File files=new File(url.getFile());
        //循环遍历所有文件
        for(File f:files.listFiles()){
            // 如果是文件夹,则递归
            if (f.isDirectory()) {
                doScanner(scanPackage+"."+f.getName());
            }else{
                classNames.add(scanPackage+"."+f.getName().replace(".class","").trim());
            }
        }

    }

    /**
     * 实例化所有相关的类,并且将其保存到IOC容器中
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        try {
          for (String className:classNames){
              //通过类名加载类对象
              Class<?> clazz = Class.forName(className);

              // 如果是Controller
              if (clazz.isAnnotationPresent(TController.class)){
                // 默认将首字母小写作为beanName
                  String beanName = StrUtils.lowerFirst(clazz.getName());
                  ioc.put(beanName,clazz.newInstance());
              }else if (clazz.isAnnotationPresent(TService.class)){
                  // 服务类
                  TService service = clazz.getAnnotation(TService.class);

                  String beanName=service.value().trim();
                  //如果注解中设置了名称就用用户设置的
                  if (!"".equals(beanName)){
                      ioc.put(beanName,clazz.newInstance());
                      //退出本次循环
                      continue;
                  }

                  // 如果用户自己没有设置,就按照接口类型创建一个实例
                  // clazz.getInterfaces()获取所有的实现类
                  Class<?>[] interfaces=clazz.getInterfaces();
                  for (Class<?> cl:interfaces){
                      ioc.put(cl.getName(),clazz.newInstance());
                  }
              }else{
                continue;
              }
          }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 自动注入bean
     */
    private void doAutoWrited() {
        if (ioc.isEmpty()){
            return;
        }

        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                // 这里仅注入标注了TAutowrited注解的对象
                if (!field.isAnnotationPresent(TAutowrited.class)){
                    continue;
                }
                TAutowrited taw = field.getAnnotation(TAutowrited.class);
                String beanName = taw.value().trim();
                if (beanName.equals("")) {
                    beanName = field.getType().getName();
                }
                //设置私有属性访问权限
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }


        }
    }

    /**
     * 将url和method进行一对一关联
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String,Object> entry:ioc.entrySet() ) {
            Class<?> clazz=entry.getValue().getClass();

            //如果非@TController 跳过
            if(!clazz.isAnnotationPresent(TController.class)){continue;}

            String baseUrl="";
            // 如果在类上面设置了@TRequestMapping
            if (clazz.isAnnotationPresent(TRequestMapping.class)){
                TRequestMapping tRequestMapping=clazz.getAnnotation(TRequestMapping.class);
                baseUrl = tRequestMapping.value();
            }

            // 获取所有的方法
            Method[] methods = clazz.getMethods();
            for (Method method:methods ) {
                //如果方法上面没有@TRequestMapping 则跳过
                if (!method.isAnnotationPresent(TRequestMapping.class)){
                    continue;
                }
                TRequestMapping tRequestMapping=method.getAnnotation(TRequestMapping.class);
                String methodUrl = tRequestMapping.value().trim();
                String url = (baseUrl+methodUrl).replaceAll("/+","/");

                handlerMapping.put(url,new MyHandler(entry.getValue(),method));
                //handlerMapping.add(new MyHandler(Pattern.compile(url),entry.getValue(),method));

                System.out.println("url:"+url+"  Controller: "+ entry.getValue()+" method:" + method);

            }

        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception, Details:\n\r"+ Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 执行请求分发处理
     * @param req
     * @param resp
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {

        MyHandler myHandler= getMyHandler(req);
        if (myHandler == null) {
            String uri= req.getRequestURI();
            if (uri.equals("/")) {
                resp.getWriter().write(" <h1>Welcome to My Mini MVC Framework Demo</h1>\r\n" +
                        "<br>For Query demo: <a href=/one/query>/one/query</a><br>" +
                        "<br>For param Demo: <a href=/one/query?name=alex>/one/query?name=alex</a>");
            }else{
                resp.getWriter().write("Not Found for Uri: "+uri);
            }

            return;
        }

        // 获取方法的参数列表
        Class<?>[] parameterTypes = myHandler.method.getParameterTypes();

        // 保存所有需要自动赋值的参数值
        Object[] paramValues = new Object[parameterTypes.length];

        Map<String,String[]> params = req.getParameterMap();
        for (Map.Entry<String,String[]> param:params.entrySet()){
            //如果未找到匹配对象,则跳过
            if(!myHandler.paramIndexMapping.containsKey(param.getKey())){continue;}

            // 获取参数的值
            String value=Arrays.toString(param.getValue())
                    .replaceAll("\\[|\\]","")
                    .replaceAll("\\s","");

            //如果找到匹配对象,则开始填充参数值
            int index = myHandler.paramIndexMapping.get(param.getKey());

            paramValues[index]=convert(parameterTypes[index],value);

        }

        // 设置方法中的 request 和response对象
        int reqIndex = myHandler.paramIndexMapping.get(HttpServletRequest.class.getName());
        paramValues[reqIndex]=req;

        int respIndex = myHandler.paramIndexMapping.get(HttpServletResponse.class.getName());
        paramValues[respIndex]=resp;

        myHandler.method.invoke(myHandler.controller, paramValues);

    }

    private Object convert(Class<?> parameterType, String value) {
        if (Integer.class == parameterType){
            int ival=0;
            try {
                ival = Integer.parseInt(value);
            }catch (NumberFormatException ne){
                System.out.println(ne.getMessage());
            }
            return ival;
        }
        return value;
    }

    /**
     * 从请求中获取自定义的Handler
     * @param req
     * @return
     */
    private MyHandler getMyHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()){return null;}

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("//+","/");

        return handlerMapping.get(url);

//        for (MyHandler handler: handlerMapping){
//            try {
//                Matcher matcher = handler.pattern.matcher(url);
//                if (!matcher.matches()){
//                    continue;
//                }
//                return handler;
//            }catch (Exception e){
//                throw  e;
//            }
//        }
//        return null;
    }


    /**
     * 自定义的处理内部类
     */
    private class MyHandler{
        //保存方法对应的实例
       protected Object controller;
       //保存映射的方法
       protected Method method;
       // url规则
       protected  Pattern pattern;
       //参数顺序
       protected Map<String,Integer> paramIndexMapping;

        /**
         * 构造方法
         * @param controller
         * @param method
         */
        public MyHandler(Object controller, Method method) {
            this.controller=controller;
            this.method=method;

            paramIndexMapping=new HashMap<String,Integer>();
            putParamIndexMapping(method);
        }
        /**
         * 构造方法
         * @param pattern
         * @param controller
         * @param method
         */
       protected MyHandler(Pattern pattern, Object controller, Method method){
           this.controller=controller;
           this.method=method;
           this.pattern=pattern;

           paramIndexMapping=new HashMap<String,Integer>();
           putParamIndexMapping(method);
       }



        private void putParamIndexMapping(Method method) {

           //提取方法中加了注解的参数
            Annotation[][] pa=method.getParameterAnnotations();// 获取所有参数注解
            for (int i = 0; i < pa.length; i++) {

                for (Annotation a:pa[i]){
                    if (a instanceof TRequestParam){
                        String paramName = ((TRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName,i);
                        }
                    }
                }

            }

            //提取方法中 request和respoonse参数
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length ; i++) {
                Class<?> type=paramTypes[i];

                if(type == HttpServletRequest.class ||
                type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }

        }

    }
}
