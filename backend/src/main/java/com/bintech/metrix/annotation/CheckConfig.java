package com.bintech.metrix.annotation;

import com.bintech.metrix.enums.ConfigType;

import java.lang.annotation.*;

/**
 * 配置检查注解，标注在Controller方法上，
 * 在执行前检查当前账号是否已配置所需的系统设置。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckConfig {

    ConfigType[] required();

}
