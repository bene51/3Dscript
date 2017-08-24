package renderer3d;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;



@Retention(RetentionPolicy.RUNTIME)
// @Target(ElementType.TYPE)
public @interface RenderingProperty {

	String label();
}
