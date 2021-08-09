/**
 * auto: <br>
 * user: yuanlu<br>
 * date: 星期日 08 12 2019
 */
package yuan.plugins.mould;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 工具类<br>
 * A类
 * 
 * @author yuanlu
 *
 */
public final class Tool {
	/**
	 * 结果映射
	 * 
	 * @author yuanlu
	 *
	 * @param <K> 键
	 * @param <V> 值
	 */
	public static final class ResultEntry<K, V> {
		/**
		 * 键
		 */
		private K	k;
		/**
		 * 值
		 */
		private V	v;

		/**
		 * 私有
		 */
		private ResultEntry() {
		}

		/**
		 * 清理节点
		 */
		private void clear() {
			this.k	= null;
			this.v	= null;
		}

		/**
		 * @param k K
		 */
		public void setKey(K k) {
			this.k = k;
		}

		/**
		 * @param v V
		 */
		public void setValue(V v) {
			this.v = v;
		}
	}

	/**
	 * 可以抛出任何错误的运行接口
	 * 
	 * @author yuanlu
	 *
	 */
	@FunctionalInterface
	public static interface ThrowableRunnable {
		/**
		 * 运行
		 * 
		 * @throws Throwable 任何错误
		 */
		void run() throws Throwable;
	}

	/**
	 * 反序列化(list)
	 * 
	 * @param <E>       元素类型
	 * @param str       序列化字符串
	 * @param split     分隔符
	 * @param translate 翻译器
	 * @return 反序列化结果
	 */
	public static final <E> ArrayList<E> deserializeList(String str, String split, Function<String, E> translate) {
		if (str.isEmpty()) return new ArrayList<E>(0);
		String[] ss = str.split(split);
		if (ss.length == 0) return new ArrayList<E>(0);
		ArrayList<E> list = new ArrayList<E>(ss.length);
		for (String s : ss) {
			list.add(translate.apply(s));
		}
		return list;
	}

	/**
	 * 迭代<br>
	 * 对映射图进行深度迭代
	 * 
	 * @param <K>  键
	 * @param <V>  值
	 * @param <VS> 值集合
	 * @param map  映射图
	 * @param func 工具
	 */
	public static final <K, V, VS extends Collection<V>> void forEach(Map<K, VS> map, BiConsumer<K, V> func) {
		map.forEach((k, vs) -> {
			vs.forEach((v) -> func.accept(k, v));
		});
	}

	/**
	 * 判断字符串是否有为true的意图
	 * 
	 * @param s 字符串
	 * @return 是否有为true的意图
	 */
	public static final boolean isTrue(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == 't' || c == 'T' || c == 'y' || c == 'Y' || c == '是' || c == '对') return true;
		}
		return false;
	}

	/**
	 * 加载某类<br>
	 * 由ClassLoader加载
	 * 
	 * @param c 类
	 */
	public static final void load(Class<?> c) {
		// byh zzm bkx wyl
		if (c != null) load(c.getTypeName());
	}

	/**
	 * 不做任何事， 仅用于加载类
	 * 
	 * @param o 任何参数
	 */
	public static final void load(Object o) {
		if (o != null) load(o.getClass());
	}

	/**
	 * 加载某类
	 * 
	 * @param str 类名
	 */
	public static final void load(String str) {
		try {
			Class.forName(str);
		} catch (ClassNotFoundException e) {
			System.err.println(str);
			e.printStackTrace();
		}
	}

	/**
	 * 将Map{@code <K,V>}转为Collection{@code <R>}
	 * 
	 * @param <K>    map键
	 * @param <V>    map值
	 * @param <R>    列表项
	 * @param <C>    集合类型
	 * @param map    图
	 * @param func   转换工具
	 * @param result 结果存储的集合
	 * @return 传入的存储集合
	 */
	public static final <K, V, R, C extends Collection<R>> C mapToCollection(Map<K, V> map, BiFunction<K, V, R> func, C result) {
		map.forEach((k, v) -> result.add(func.apply(k, v)));
		return result;
	}

	/**
	 * 将Map{@code <K,Collection<V>>}转为Collection{@code <R>}
	 * 
	 * @param <K>    map键类型
	 * @param <V>    map值类型
	 * @param <R>    列表内容类型
	 * @param <RC>   结果集合类型
	 * @param <VC>   映射集合类型
	 * @param map    图
	 * @param func   转换工具
	 * @param result 结果存储的集合
	 * @return 传入的存储集合
	 */
	public static final <K, V, R, RC extends Collection<R>, VC extends Collection<V>> RC mutiMapToCollection(Map<K, VC> map, BiFunction<K, V, R> func,
			RC result) {
		map.forEach((k, vs) -> {
			vs.forEach((v) -> result.add(func.apply(k, v)));
		});
		return result;
	}

	/**
	 * 解析字符串含义<br>
	 * 使用指定字符包围的字符串将作为变量名解析为对应数据<br>
	 * 例子:
	 * 
	 * <pre>
	 * msg: 你好{@code <player>}, 我是{@code <who>}, 现在已经{@code <time>}点了！
	 * start: {@code '<'}
	 * end: {@code '>'}
	 * vars:{
	 *     player: yuanlu,
	 *     who: Administrators
	 *     time: 7
	 * }
	 * 
	 * return: 你好yuanlu, 我是Administrators, 现在已经7点了！
	 * </pre>
	 * 
	 * @param msg   原消息
	 * @param start 变量开始字符
	 * @param end   变量结束字符
	 * @param vars  变量集合
	 * @return 解析后字符串
	 */
	public static String parseVar(final String msg, char start, char end, Map<String, Object> vars) {
		int startIndex;
		if (msg == null || (startIndex = msg.indexOf(start)) < 0) return msg;
		try {
			StringBuilder sb = new StringBuilder();
			if (startIndex > 0) sb.append(msg, 0, startIndex);
			for (int i = startIndex; i < msg.length(); i++) {
				char c = msg.charAt(i);

				if (c == start) {// 找到开始标记
					int e = msg.indexOf(end, i + 1);// 寻找结束标记
					if (e < 0) {// 没有结束标记
						sb.append(msg.substring(i));
						return sb.toString();// 直接返回
					}

					String var_str = msg.substring(i + 1, e);// 变量名

					if (var_str.isEmpty()) {// 变量名为空
						if (start == end) sb.append(start);// 转意
						else sb.append(start).append(end);// 不是变量
					} else {

						if (vars.containsKey(var_str)) {// 是变量
							Object var = vars.get(var_str);
							sb.append(var);
						} else {// 不是变量
							sb.append(start).append(var_str).append(end);
						}

					}
					i = e;// 跳过变量部分
				} else {
					sb.append(c);// 普通字符
				}
			}
			return sb.toString();
		} catch (Throwable e) {
			Main.getMain().getLogger().warning("不能翻译字符串: " + msg);
			e.printStackTrace();
			return msg;
		}
	}

	/**
	 * 序列化
	 * 
	 * @param <E>       序列化集合元素类型
	 * @param c         序列化集合
	 * @param split     元素分隔符
	 * @param translate 元素翻译器
	 * @return 序列化字符串
	 */
	public static final <E> String serialize(Collection<E> c, char split, Function<? super E, String> translate) {
		if (c.isEmpty()) return "";
		StringBuilder	sb	= new StringBuilder();
		Iterator<E>		itr	= c.iterator();
		while (true) {
			sb.append(translate.apply(itr.next()));
			if (!itr.hasNext()) return sb.toString();
			sb.append(split);
		}
	}

	/**
	 * 将Set转换为Map
	 * 
	 * @param <E> Set元素类型
	 * @param <M> Map类型
	 * @param set 元素集合
	 * @param map 元素映射图(结果)
	 * @return 传入的元素映射图
	 */
	public static final <E, M extends Map<E, E>> M setToMap(Set<E> set, M map) {
		set.forEach((e) -> map.put(e, e));
		return map;
	}

	/**
	 * 一个测试方法: 使用反射将某实例内字段放入一个map集合内
	 * 
	 * @param obj 实例
	 * @return map
	 * @throws IllegalAccessException   Exception
	 * @throws IllegalArgumentException Exception
	 */
	public static Map<String, Object> test(Object obj) throws IllegalArgumentException, IllegalAccessException {
		HashMap<String, Object>	m	= new HashMap<String, Object>();
		Field[]					fs	= obj.getClass().getDeclaredFields();
		for (Field field : fs) {
			field.setAccessible(true);
			String	name	= field.getName();
			Object	data	= field.get(obj);
			m.put(name, data);
		}
		return m;
	}

	/**
	 * 翻译列表
	 * 
	 * @param <T>  要翻译的类型
	 * @param <R>  结果类型
	 * @param list 要翻译的列表
	 * @param func 翻译工具
	 * @return 结果列表
	 */
	public static final <T, R> ArrayList<R> translate(List<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		ArrayList<R> result = new ArrayList<R>(list.size());
		for (T t : list) {
			R r = func.apply(t);
			result.add(r);
		}
		return result;
	}

	/**
	 * 翻译映射图
	 * 
	 * @param <TK>   要翻译的类型键
	 * @param <TV>   要翻译的类型值
	 * @param <RK>   结果类型键
	 * @param <RV>   结果类型值
	 * @param <RM>   结果映射图类型
	 * @param map    要翻译的映射图
	 * @param func   翻译工具
	 * @param result 结果
	 * @return 传入的结果映射图
	 */
	public static final <TK, TV, RK, RV, RM extends LinkedHashMap<RK, RV>> RM translate(Map<TK, TV> map, BiConsumer<Entry<TK, TV>, ResultEntry<RK, RV>> func,
			RM result) {
		Objects.requireNonNull(map, "map can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		ResultEntry<RK, RV> resultEntry = new ResultEntry<RK, RV>();
		for (Entry<TK, TV> entry : map.entrySet()) {
			func.accept(entry, resultEntry);
			result.put(resultEntry.k, resultEntry.v);
			resultEntry.clear();
		}
		return result;
	}

	/**
	 * 翻译集合
	 * 
	 * @param <T>  要翻译的类型
	 * @param <R>  结果类型
	 * @param list 要翻译的列表
	 * @param func 翻译工具
	 * @return 结果集合
	 */
	public static final <T, R> HashSet<R> translate(Set<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		HashSet<R> result = new HashSet<R>();
		for (T t : list) {
			R r = func.apply(t);
			result.add(r);
		}
		return result;
	}

	/**
	 * 翻译为列表
	 * 
	 * @param <T>  要翻译的类型
	 * @param <R>  结果类型
	 * @param list 要翻译的集合
	 * @param func 翻译工具
	 * @return 结果列表
	 */
	public static final <T, R> ArrayList<R> translateToList(Collection<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		ArrayList<R> result = new ArrayList<R>(list.size());
		for (T t : list) {
			R r = func.apply(t);
			result.add(r);
		}
		return result;
	}

	/**
	 * 翻译为集合
	 * 
	 * @param <T>  要翻译的类型
	 * @param <R>  结果类型
	 * @param list 要翻译的集合
	 * @param func 翻译工具
	 * @return 结果集合
	 */
	public static final <T, R> HashSet<R> translateToSet(Collection<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		HashSet<R> result = new HashSet<R>();
		for (T t : list) {
			R r = func.apply(t);
			result.add(r);
		}
		return result;
	}

	/**
	 * 尝试运行
	 * 
	 * @param needRun 是否需要运行<br>
	 *                若为false则什么都不做
	 * 
	 * @param tr      可抛出错误的代码体
	 */
	public static final void tryRun(boolean needRun, ThrowableRunnable tr) {
		if (needRun) try {
			tr.run();
		} catch (Throwable e) {
			Main.getMain().getLogger().warning("尝试运行部分代码时出错:");
			e.printStackTrace();
		}
	}

	/**
	 * 尝试运行
	 * 
	 * @param tr 可抛出错误的代码体
	 */
	public static final void tryRun(ThrowableRunnable tr) {
		try {
			tr.run();
		} catch (Throwable e) {
			Main.getMain().getLogger().warning("尝试运行部分代码时出错:");
			e.printStackTrace();
		}
	}

	/**
	 * 尝试运行
	 * 
	 * @param r 代码体
	 */
	public static final void tryRunNormal(Runnable r) {
		try {
			r.run();
		} catch (Throwable e) {
			Main.getMain().getLogger().warning("尝试运行部分代码时出错:");
			e.printStackTrace();
		}
	}

	/**
	 * 禁止实例化
	 */
	private Tool() {
	}

}
