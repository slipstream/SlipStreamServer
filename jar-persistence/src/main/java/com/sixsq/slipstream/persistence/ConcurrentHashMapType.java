package com.sixsq.slipstream.persistence;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class ConcurrentHashMapType<V, T> implements UserCollectionType {

	public ConcurrentHashMapType() {
	}

	public PersistentCollection instantiate(SessionImplementor session,
			CollectionPersister persister) {
		return new PersistentMap(session);
	}

	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ConcurrentHashMap<V, T>()
				: new ConcurrentHashMap<V, T>(anticipatedSize
						+ (int) (anticipatedSize * .75f), .75f);
	}

	@SuppressWarnings("unchecked")
	public PersistentCollection wrap(SessionImplementor session,
			Object collection) {
		return new PersistentMap(session, (ConcurrentHashMap<V, T>) collection);
	}

	@SuppressWarnings("unchecked")
	public Iterator<T> getElementsIterator(Object collection) {
		try {
			return ((ConcurrentHashMap<V, T>) collection).values().iterator();
		} catch (ClassCastException cce) {
			return ((PersistentMap) collection).values().iterator();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean contains(Object collection, Object entity) {
		ConcurrentHashMap<V, T> map = (ConcurrentHashMap<V, T>) collection;
		return map.containsKey(entity);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object indexOf(Object collection, Object element) {
		Iterator<T> iter = ((ConcurrentHashMap) collection).entrySet()
				.iterator();
		while (iter.hasNext()) {
			ConcurrentHashMap.Entry me = (ConcurrentHashMap.Entry) iter.next();
			if (me.getValue() == element)
				return me.getKey();
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object replaceElements(Object original, Object target,
			CollectionPersister persister, Object owner, Map copyCache,
			SessionImplementor session) throws HibernateException {

		java.util.Map result = (java.util.Map) target;
		result.clear();

		Iterator iter = ((java.util.Map) original).entrySet().iterator();
		while (iter.hasNext()) {
			java.util.Map.Entry me = (java.util.Map.Entry) iter.next();
			Object key = persister.getIndexType().replace(me.getKey(), null,
					session, owner, copyCache);
			Object value = persister.getElementType().replace(me.getValue(),
					null, session, owner, copyCache);
			result.put(key, value);
		}

		return result;

	}

	@SuppressWarnings("rawtypes")
	public Class getReturnedClass() {
		return ConcurrentHashMap.class;
	}
}
