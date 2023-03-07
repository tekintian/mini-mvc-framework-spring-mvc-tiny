package cn.tekin.mvccore.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解
 * @author tekintian@gmail.com
 * @version v0.0.1
 * @since v0.0.1 2023-03-07 15:31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
@Documented
public @interface TAutowrited {
    String value() default "";
}
