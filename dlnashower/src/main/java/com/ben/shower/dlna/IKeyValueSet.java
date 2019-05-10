package com.ben.shower.dlna;

public interface IKeyValueSet<T, K,V> {
	T putKeyValue(T target, K key, V value);
	V getKeyValue(T target, K key);
}
