package com.bintech.metrix.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解，用于标记需要自动记录审计日志的接口方法。
 * 可标注在类或方法上，方法级优先级高于类级。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    /** 操作动作，为空则根据HTTP方法自动推断 */
    String action() default "";

    /** 资源类型，为空则从类注解或@RequestMapping推断 */
    String resourceType() default "";

    /** 操作描述（将写入detail字段） */
    String description() default "";
}
