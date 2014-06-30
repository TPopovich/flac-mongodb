package com.mongodb.flac.converter;

/**
 * FLACProperty annotation to use the FLACPropertyProcessor to automatically fetch FLAC security attributes.
 */

import java.lang.annotation.*;
import java.util.List;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FLACProperty {
    String attributeNameInSl() default "";
}
