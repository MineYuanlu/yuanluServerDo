package yuan.plugins.serverDo;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.val;

/**
 * 最近最少使用
 * 
 * @author sun.misc
 *
 * @param <K> K
 * @param <V> V
 */
@SuppressWarnings("unchecked")
public abstract class LRUCache<K, V> {
	/**
	 * 节点
	 * 
	 * @author yuanlu
	 *
	 * @param <K> 键
	 * @param <V> 值
	 */
	@AllArgsConstructor
	@SuppressWarnings("javadoc")
	private static final class Node<K, V> {
		K	k;
		V	v;

		void set(K k, V v) {
			this.k	= k;
			this.v	= v;
		}
	}

	/** 缓存使用数 */
	public static final AtomicInteger CACHE_USE = new AtomicInteger();

	/**
	 * 将元素移到最前端
	 * 
	 * @param objs  数据
	 * @param index 要移动的数据
	 */
	private static void moveToFront(Object[] objs, int index) {
		Object obj = objs[index];
		for (int i = index; i > 0; i--) {
			objs[i] = objs[(i - 1)];
		}
		objs[0] = obj;
	}

	/** 缓存 */
	private Object[]	cache	= null;

	/** 缓存大小 */
	private int			size;

	/** @param size 缓存大小 */
	public LRUCache(int size) {
		this.size = size;
	}

	/**
	 * 检出元素, 不会对缓存进行任何修改
	 * 
	 * @param k 键
	 * @return 值, 当不存在时返回null
	 */
	public synchronized final V check(K k) {
		CACHE_USE.addAndGet(1);
		if (cache != null) {
			for (int i = 0; i < size; i++) {
				Node<K, V> now = (Node<K, V>) cache[i];
				if (now == null) break;
				if (Objects.equals(k, now.k)) return now.v;
			}
		}
		return null;
	}

	/**
	 * 清空缓存
	 * 
	 * @return 是否有元素被清除
	 */
	public synchronized final boolean clearCache() {
		if (cache == null) return false;
		int i;
		for (i = 0; i < size; i++) {
			Node<K, V> node = (Node<K, V>) cache[i];
			if (node == null) break;
			clearHandle(node.k, node.v);
		}
		Arrays.fill(cache, 0, i, null);
		return i != 0;
	}

	/**
	 * 清除处理<br>
	 * 当一个缓存被清除时触发
	 * 
	 * @param k 键
	 * @param v 值
	 */
	protected void clearHandle(K k, V v) {

	}

	/**
	 * 创建对象
	 * 
	 * @param k 键
	 * @return 值
	 */
	protected abstract V create(K k);

	/**
	 * 获取元素
	 * 
	 * @param k 键
	 * @return 值
	 * 
	 */
	public synchronized final V get(K k) {
		CACHE_USE.addAndGet(1);
		if (cache == null) cache = new Object[size];
		else {
			for (int i = 0; i < size; i++) {
				Node<K, V> now = (Node<K, V>) cache[i];
				if (now == null) break;
				if (Objects.equals(k, now.k)) {
					if (i > 0) moveToFront(cache, i);
					return now.v;
				}
			}
		}
		V			v		= create(k);
		Node<K, V>	node	= (Node<K, V>) cache[size - 1];
		if (node != null) {
			clearHandle(node.k, node.v);
			node.set(k, v);
		} else cache[size - 1] = new Node<>(k, v);
		moveToFront(cache, size - 1);
		return v;
	}

	/**
	 * 重新调整缓冲区大小
	 * 
	 * @param size 大小
	 */
	public synchronized final void resize(int size) {
		if (cache != null) {
			if (cache.length < size || cache.length > size * 2) {
				val old = cache;
				cache = new Node[size];
				System.arraycopy(old, 0, cache, 0, Math.min(old.length, size));
			} else Arrays.fill(cache, size, cache.length, null);
		}
		this.size = size;
	}
}