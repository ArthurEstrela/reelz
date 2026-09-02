package com.roletadefilmes.support.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCineGiroUserSecurityContextFactory.class)
public @interface WithMockCineGiroUser {

    String userId() default "11111111-1111-1111-1111-111111111111";
}
