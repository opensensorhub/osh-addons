package com.MAVLink.Messages;

import java.lang.annotation.*;
import java.lang.reflect.Field;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@SuppressWarnings("unchecked")
/**
 * Description annotation to provide programmatic access to the MAVLink description for each field
 */
public @interface Description {

	String value();

	class Test {

		@Description("The speed of the drone")
		public float speed;

		public static void main(String[] args) throws Exception {
			Field f = Test.class.getField("speed");
			Description anno = (Description) f.getAnnotation(Description.class);
			System.out.println(anno.value());
		}
	}
}
