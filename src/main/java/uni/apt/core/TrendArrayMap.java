package uni.apt.core;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

//TODO: complete this class
public class TrendArrayMap<K , V> implements Map<K,V> {

    private static class Entry<K , V> {
        public K k;
        public V v;

        public Entry(K k , V v){
            this.k = k;
            this.v = v;
        }

    }

    private final int _maxSize;
    private int _size;

    private final Entry[] entries;

    public TrendArrayMap(int ms){
        if (ms < 0){
            throw new IllegalArgumentException("max size must be > 0");
        }
        _maxSize = ms;

        entries = new Entry[_maxSize];
        _size = 0;
    }

    @Override
    public int size() {
        return _size;
    }

    @Override
    public boolean isEmpty() {
        return _size == 0;
    }

    @Override
    public boolean containsKey(Object key) {

        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public V get(Object key) {
        return null;
    }

    @Override
    public V put(K key, V value) {
        return null;
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return null;
    }
}
