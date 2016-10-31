package com.hpe.caf.worker.testing;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by oloughli on 22/07/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface UseAsTestName {

    int idx() default 0;
}
