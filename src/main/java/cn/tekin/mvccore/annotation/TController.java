package cn.tekin.mvccore.annotation;

import java.lang.annotation.*;

/**
 * @author tekintian@gmail.com
 * @version v0.0.1
 * @since v0.0.1 2023-03-07 15:56
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TController {
    String value() default "";
}
