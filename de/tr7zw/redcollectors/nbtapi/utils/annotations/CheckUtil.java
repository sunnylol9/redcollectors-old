package de.tr7zw.redcollectors.nbtapi.utils.annotations;

import java.lang.reflect.Method;

import de.tr7zw.redcollectors.nbtapi.NbtApiException;
import de.tr7zw.redcollectors.nbtapi.utils.MinecraftVersion;

public class CheckUtil {

	public static boolean isAvaliable(Method method) {
		if(MinecraftVersion.getVersion().getVersionId() < method.getAnnotation(AvaliableSince.class).version().getVersionId())
			throw new NbtApiException("The Method '" + method.getName() + "' is only avaliable for the Versions " + method.getAnnotation(AvaliableSince.class).version() + "+, but still got called!");
		return true;
	}
	
}
