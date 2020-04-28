package com.github.mikucat0309.command.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Nonnull
@TypeQualifierDefault(
        {
                ElementType.FIELD,
                ElementType.METHOD,
                ElementType.PARAMETER
        }
)
@Retention(RetentionPolicy.RUNTIME)
public @interface NonnullByDefault {

}
