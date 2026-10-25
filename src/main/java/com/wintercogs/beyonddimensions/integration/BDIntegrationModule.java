package com.wintercogs.beyonddimensions.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BDIntegrationModule
{
    String modId();

    /**
     * 模组版本的范围，Maven 写法，如 "[6.0.0,)"；为空时不限版本。版本不在范围内时不加载这个模块
     */
    String versionRange() default "";
}
