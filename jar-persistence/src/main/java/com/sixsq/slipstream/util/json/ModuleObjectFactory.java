package com.sixsq.slipstream.util.json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.persistence.Module;

import flexjson.ObjectBinder;
import flexjson.ObjectFactory;

public class ModuleObjectFactory implements ObjectFactory {

	public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
		if (value instanceof HashMap) {
			@SuppressWarnings("unchecked")
			HashMap<String, String> map = (HashMap<String, String>) value;
			String category = map.get("category");
			String name = map.get("name");
			if(category == null || name == null) {
				return null;
			}
			try {
				return Class.forName("com.sixsq.slipstream.persistence." + category + "Module").getConstructor(String.class)
						.newInstance(name);
			} catch (ClassNotFoundException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			} catch (InstantiationException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			} catch (IllegalAccessException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			} catch (IllegalArgumentException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			} catch (InvocationTargetException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			} catch (NoSuchMethodException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			} catch (SecurityException e) {
				throw new SlipStreamRuntimeException("cannot deserialize object", e);
			}
		} else {
			throw context.cannotConvertValueToTargetType(value, Module.class);
		}
	}

}
