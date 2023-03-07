package cn.tekin.demo.service.impl;

import cn.tekin.demo.service.IDemoService;
import cn.tekin.mvccore.annotation.TService;

/**
 * @author tekintian@gmail.com
 * @version v0.0.1
 * @since v0.0.1 2023-03-07 15:59
 */
@TService
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {

        String p = null==name?"Demo":name;
        return "My MVC "+p;
    }

    @Override
    public String edit(Integer id) {
        int dd = null ==id?0:id;
        return "Edit "+dd;
    }
}
