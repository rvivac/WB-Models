package com.wbscouting.api.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    /**
     * Identificador canônico da operação (ex: CREATE, UPDATE, DELETE, APPROVE, REJECT, LOGIN, LOGOUT).
     */
    String action();

    /**
     * Tipo do recurso afetado (ex: SCOUTING_CANDIDATE, MODEL, ADMIN_USER, INSTITUTIONAL_CONTENT, AUTH).
     */
    String resource();

    /**
     * Descrição legível do evento ou modelo de evento.
     */
    String description() default "";
}
