package ch.njol.skript.util;

import java.lang.annotation.*;
import java.util.Iterator;

/**
 * Represents a class which is a container, i.e. something like a collection.<br>
 * If this is used, a {@link ContainerType} annotation must be added to the implementing class which holds the class instance the containser holds.
 *
 * @author Peter Güttinger
 */
public interface Container<T> {

	@SuppressWarnings("null")
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public static @interface ContainerType {
		Class<?> value();
	}

	/**
	 * @return All element within this container in no particular order
	 */
	public Iterator<T> containerIterator();

}