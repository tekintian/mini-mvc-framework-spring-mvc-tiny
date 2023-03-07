package cn.tekin.demo.controller;

import cn.tekin.demo.service.IDemoService;
import cn.tekin.mvccore.annotation.TAutowrited;
import cn.tekin.mvccore.annotation.TController;
import cn.tekin.mvccore.annotation.TRequestMapping;
import cn.tekin.mvccore.annotation.TRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author tekintian@gmail.com
 * @version v0.0.1
 * @since v0.0.1 2023-03-07 16:10
 */
@TController
@TRequestMapping("/two")
public class DemoTwo {

    @TAutowrited
    private IDemoService demoService;

    @TRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @TRequestParam("name") String name){
        String result= demoService.get(name);
        try {
            resp.getWriter().write(result);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    @TRequestMapping("/edit")
    public void edit(HttpServletRequest req, HttpServletResponse resp,
                     @TRequestParam("id") Integer id){
        try {
            resp.getWriter().write(demoService.edit(id));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
