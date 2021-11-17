package yuan.plugins.serverDo;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.val;

/**
 * 等待任务的维护清理
 *
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WaitMaintain {
	/**
	 * 延时监听元素
	 *
	 * @author yuanlu
	 *
	 */
	@Value
	@EqualsAndHashCode(callSuper = true)
	@SuppressWarnings("rawtypes")
	private static final class CElement extends Element {

		/** 图 */
		Collection	set;

		/** 键 */
		Object		k;

		@SuppressWarnings("javadoc")
		public CElement(long expire, Collection set, Object k, Runnable clearListener) {
			super(expire, clearListener);
			this.set	= set;
			this.k		= k;
		}

		/** 处理 */
		@Override
		void handle() {
			if (set.remove(k) && clearListener != null) clearListener.run();
		}
	}

	/**
	 * 延时监听元素
	 *
	 * @author yuanlu
	 *
	 */
	@AllArgsConstructor
	private static abstract class Element implements Delayed {
		/** 单位 */
		private static final TimeUnit	U	= TimeUnit.MILLISECONDS;

		/** 到期时间 */
		long							expire;
		/** 清理监听 */
		Runnable						clearListener;

		@Override
		public int compareTo(Delayed o) {
			return Long.compare(getDelay(U), o.getDelay(U));
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(this.expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}

		/** 处理 */
		abstract void handle();

	}

	/**
	 * 延时监听元素
	 *
	 * @author yuanlu
	 *
	 */
	@Value
	@EqualsAndHashCode(callSuper = true)
	@SuppressWarnings("rawtypes")
	private static final class MapElement extends Element {

		/** 图 */
		Map		map;

		/** 键 */
		Object	k;

		/** 值 */
		Object	old;

		@SuppressWarnings("javadoc")
		public MapElement(long expire, Map map, Object k, Object old, Runnable clearListener) {
			super(expire, clearListener);
			this.map	= map;
			this.k		= k;
			this.old	= old;
		}

		/** 处理 */
		@Override
		void handle() {
			if (map.remove(k, old) && clearListener != null) clearListener.run();
		}
	}

	/**
	 * 延时监听元素
	 *
	 * @author yuanlu
	 *
	 */
	@Value
	@EqualsAndHashCode(callSuper = true)
	@SuppressWarnings("rawtypes")
	private static final class MapMapElement extends Element {

		/** 图 */
		Map		map;

		/** 主键 */
		Object	t;
		/** 副键 */
		Object	k;

		/** 值 */
		Object	v;

		@SuppressWarnings("javadoc")
		public MapMapElement(long expire, Map map, Object t, Object k, Object v, Runnable clearListener) {
			super(expire, clearListener);
			this.map	= map;
			this.t		= t;
			this.k		= k;
			this.v		= v;
		}

		/** 处理 */
		@Override
		void handle() {
			Map m = (Map) map.get(t);
			if (m == null) return;
			boolean clear = m.remove(k, v);
			if (m.isEmpty()) map.remove(t, m);
			if (clear && clearListener != null) clearListener.run();
		}
	}

	/**
	 * 延时监听元素
	 *
	 * @author yuanlu
	 *
	 */
	@Value
	@EqualsAndHashCode(callSuper = true)
	@SuppressWarnings("rawtypes")
	private static final class MutiMapElement extends Element {

		/** 图 */
		Map		map;

		/** 键 */
		Object	k;

		/** 值 */
		Object	old;

		@SuppressWarnings("javadoc")
		public MutiMapElement(long expire, Map map, Object k, Object old, Runnable clearListener) {
			super(expire, clearListener);
			this.map	= map;
			this.k		= k;
			this.old	= old;
		}

		/** 处理 */
		@Override
		void handle() {
			Collection c = (Collection) map.get(k);
			if (c == null) return;
			boolean clear = c.remove(old);
			if (c.isEmpty()) map.remove(k, c);
			if (clear && clearListener != null) clearListener.run();
		}
	}

	/**
	 * 延时监听元素
	 *
	 * @author yuanlu
	 *
	 */
	@Value
	@EqualsAndHashCode(callSuper = true)
	private static final class NElement extends Element {
		/** 原始值 */
		long	originalLong;
		/** 原始值 */
		double	originalDouble;
		/** 更新值 */
		Number	num;

		@SuppressWarnings("javadoc")
		public NElement(long expire, Runnable clearListener, Number num) {
			super(expire, clearListener);
			this.num			= num;
			this.originalDouble	= num.doubleValue();
			this.originalLong	= num.longValue();
		}

		@Override
		void handle() {
			if (num.doubleValue() == originalDouble && //
					num.longValue() == originalLong)
				clearListener.run();
		}

	}

	/** 等待时长(ms) 网络 */
	public static @Getter @Setter long			T_Net	= 5 * 1000;

	/** 等待时长(ms) 用户 */
	public static @Getter @Setter long			T_User	= 120 * 1000;

	/** 队列 */
	private static final DelayQueue<Element>	QUEUE	= new DelayQueue<>();
	static {
		new Thread("YSD-" + WaitMaintain.class) {
			@Override
			public void run() {
				while (true) {
					try {
						val ele = QUEUE.take();
						if (ele != null) ele.handle();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * 将键放入set,并设置最长超时时间, 超时后将被清理
	 *
	 * @param <K>     数据类型
	 * @param set     图
	 * @param k       键
	 * @param maxTime 等待时长
	 * @return return
	 */
	public static <K> boolean add(Collection<K> set, K k, long maxTime) {
		return add(set, k, maxTime, null);
	}

	/**
	 * 将键放入set,并设置最长超时时间, 超时后将被清理
	 *
	 * @param <K>           数据类型
	 * @param set           图
	 * @param k             键
	 * @param maxTime       等待时长
	 * @param clearListener 清理监听
	 * @return return
	 */
	public static <K> boolean add(Collection<K> set, K k, long maxTime, Runnable clearListener) {
		val r = set.add(k);
		QUEUE.add(new CElement(System.currentTimeMillis() + maxTime, set, k, clearListener));
		return r;
	}

	/**
	 * 将一对多键值对放入map,并设置最长超时时间, 超时后将被清理
	 *
	 * @param <K>           数据类型
	 * @param <V>           数据类型
	 * @param <L>           多值列表类型
	 * @param map           图
	 * @param k             键
	 * @param v             值
	 * @param maxTime       等待时长
	 * @param builder       列表构造器
	 * @param clearListener 清理监听
	 * @return return
	 */
	public static <K, V, L extends Collection<V>> boolean add(Map<K, L> map, K k, V v, long maxTime, Supplier<L> builder, Runnable clearListener) {
		L c = map.get(k);
		if (c == null) map.put(k, c = builder.get());
		val r = c.add(v);
		QUEUE.add(new MutiMapElement(System.currentTimeMillis() + maxTime, map, k, v, clearListener));
		return r;
	}

	/**
	 * 监听数值, 并设置最长超时时间, 若超时后数值未发生改变则触发清理
	 *
	 * @param <K>           数据类型
	 * @param num           数值
	 * @param maxTime       等待时长
	 * @param clearListener 清理监听
	 */
	public static <K> void monitor(@NonNull Number num, long maxTime, @NonNull Runnable clearListener) {
		QUEUE.add(new NElement(System.currentTimeMillis() + maxTime, clearListener, num));
	}

	/**
	 * 将键值对放入map,并设置最长超时时间, 超时后将被清理
	 *
	 * @param <K>     数据类型
	 * @param <V>     数据类型
	 * @param map     图
	 * @param k       键
	 * @param v       值
	 * @param maxTime 等待时长
	 * @return return
	 */
	public static <K, V> V put(Map<K, V> map, K k, V v, long maxTime) {
		return put(map, k, v, maxTime, null);
	}

	/**
	 * 将键值对放入map,并设置最长超时时间, 超时后将被清理
	 *
	 * @param <K>           数据类型
	 * @param <V>           数据类型
	 * @param map           图
	 * @param k             键
	 * @param v             值
	 * @param maxTime       等待时长
	 * @param clearListener 清理监听
	 * @return return
	 */
	public static <K, V> V put(Map<K, V> map, K k, V v, long maxTime, Runnable clearListener) {
		val old = map.put(k, v);
		QUEUE.add(new MapElement(System.currentTimeMillis() + maxTime, map, k, v, clearListener));
		return old;
	}

	/**
	 * 将一对多键值对放入map,并设置最长超时时间, 超时后将被清理
	 *
	 * @param <T>           数据类型
	 * @param <K>           数据类型
	 * @param <V>           数据类型
	 * @param <M>           值Map类型
	 * @param map           图
	 * @param t             键1
	 * @param k             键2
	 * @param v             值
	 * @param maxTime       等待时长
	 * @param builder       列表构造器
	 * @param clearListener 清理监听
	 * @return return
	 */
	public static <T, K, V, M extends Map<K, V>> V put(Map<T, M> map, T t, K k, V v, long maxTime, Supplier<M> builder, Runnable clearListener) {
		M m = map.get(t);
		if (m == null) map.put(t, m = builder.get());
		val r = m.put(k, v);
		QUEUE.add(new MapMapElement(System.currentTimeMillis() + maxTime, map, t, k, v, clearListener));
		return r;
	}

}
