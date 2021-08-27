/**
 * auto: <br>
 * user: yuanlu<br>
 * date: 星期日 08 12 2019
 */
package yuan.plugins.serverDo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.val;

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
	 * @param <E> 错误类型
	 * @param <T> 输入类型
	 * @param <R> 输出类型
	 *
	 */
	@FunctionalInterface
	public interface ThrowableFunction<E extends Throwable, T, R> {
		/**
		 * Applies this function to the given argument.
		 *
		 * @param t the function argument
		 * @return the function result
		 * @throws E the function error
		 */
		R apply(T t) throws E;
	}

	/**
	 * 可以抛出任何错误的运行接口
	 * 
	 * @author yuanlu
	 * @param <T> 错误类型
	 *
	 */
	@FunctionalInterface
	public interface ThrowableRunnable<T extends Throwable> {
		/**
		 * 运行
		 * 
		 * @throws T 错误
		 */
		void run() throws T;
	}

	/** 空的运行体 */
	public static final Runnable	EMPTY_RUNNABLE	= () -> {
													};

	/** 随机字符串 */
	private static final char[]		RANDOM			= "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM!@#$%^&*()".toCharArray();

	/** 驼峰转换正则 */
	private static Pattern			humpPattern		= Pattern.compile("[A-Z]");

	/**
	 * 反序列化(list)
	 * 
	 * @param <E>       元素类型
	 * @param str       序列化字符串
	 * @param split     分隔符
	 * @param translate 翻译器
	 * @return 反序列化结果
	 */
	public static <E> ArrayList<E> deserializeList(String str, String split, Function<String, E> translate) {
		if (str.isEmpty()) return new ArrayList<>(0);
		String[] ss = str.split(split);
		if (ss.length == 0) return new ArrayList<>(0);
		ArrayList<E> list = new ArrayList<>(ss.length);
		for (String s : ss) {
			list.add(translate.apply(s));
		}
		return list;
	}

	/**
	 * 比较两个对象是否一致<br>
	 * 特别的, 当对象均为Number时, 将会比较器long与double值, 其中double值误差范围为{@code 0x1.0p-1021}
	 * 
	 * @param a 对象
	 * @param b 对象
	 * @return 是否一致
	 */
	public static boolean equals(Object a, Object b) {
		if (Objects.equals(a, b)) return true;
		if (a instanceof Number && b instanceof Number) {
			Number A = (Number) a, B = (Number) b;
			if (A.longValue() == B.longValue() && Math.abs(A.doubleValue() - B.doubleValue()) < 0x1.0p-1021) return true;
		}
		return false;
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
	public static <K, V, VS extends Collection<V>> void forEach(Map<K, VS> map, BiConsumer<K, V> func) {
		map.forEach((k, vs) -> {
			vs.forEach(v -> func.accept(k, v));
		});
	}

	/**
	 * 获取匹配的列表
	 * 
	 * @param str 给定字符串
	 * @param c   内容
	 * @return 匹配的列表
	 */
	public static List<String> getMatchList(@NonNull String str, @NonNull Collection<String> c) {
		ArrayList<String> l = new ArrayList<>();
		str = str.toLowerCase();
		for (val x : c) if (x != null && x.toLowerCase().startsWith(str)) l.add(x);
		return l;
	}

	/**
	 * 获取匹配的列表
	 * 
	 * @param <T>   内容类型
	 * @param str   给定字符串
	 * @param c     内容
	 * @param trans 内容转换
	 * @return 匹配的列表
	 */
	public static <T> List<String> getMatchList(@NonNull String str, @NonNull Collection<T> c, Function<T, String> trans) {
		ArrayList<String> l = new ArrayList<>();
		str = str.toLowerCase();
		for (val y : c) {
			val x = trans.apply(y);
			if (x != null && x.toLowerCase().startsWith(str)) l.add(x);
		}
		return l;
	}

	/**
	 * 驼峰转换
	 * 
	 * @param str    原始字符串
	 * @param joiner 单词间插值
	 * @return 转换结果
	 */
	public static String humpTrans(String str, Object joiner) {
		val	matcher	= humpPattern.matcher(str);
		val	sb		= new StringBuffer();
		val	j		= String.valueOf(joiner);
		while (matcher.find()) {
			matcher.appendReplacement(sb, j + matcher.group(0).toLowerCase());
		}
		matcher.appendTail(sb);
		str = sb.toString();
		if (str.startsWith(j)) str = str.substring(j.length());
		return str;
	}

	/**
	 * 逆向操作驼峰转换
	 * 
	 * @param str    原始字符串
	 * @param joiner 单词间插值
	 * @return 转换结果
	 */
	public static String humpTransBack(String str, Object joiner) {
		StringBuilder sb;
		try {
			val join = String.valueOf(joiner);
			sb = new StringBuilder();
			int	i	= 0;
			int	j	= 0;
			while ((i = str.indexOf(join, i)) >= 0) {
				sb.append(str, j, i);
				sb.append(Character.toUpperCase(str.charAt(i += join.length())));
				j = i + 1;
			}
			sb.append(str, j, str.length());
		} catch (IndexOutOfBoundsException e) {
			return null;
		}

		return sb.toString();
	}

	/**
	 * 判断字符串是否有为true的意图
	 * 
	 * @param s 字符串
	 * @return 是否有为true的意图
	 */
	public static boolean isTrue(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == 't' || c == 'T' || c == 'y' || c == 'Y' || c == '是' || c == '对') return true;
		}
		return false;
	}

	/**
	 * 组合集合
	 * 
	 * @param c         集合
	 * @param frame     框架格式(elements,size)
	 * @param element   元素格式(index,data)
	 * @param delimiter 分隔符
	 * @return 字符串
	 */
	public static String join(Collection<?> c, String frame, String element, String delimiter) {
		StringJoiner	sj	= new StringJoiner(delimiter);
		int				i	= 0;
		for (val x : c) sj.add(String.format(element, ++i, x));
		return String.format(frame, sj, i);
	}

	/**
	 * 加载某类<br>
	 * 由ClassLoader加载
	 * 
	 * @param c 类
	 */
	public static void load(Class<?> c) {
		if (c != null) load(c.getTypeName());
	}

	/**
	 * 不做任何事， 仅用于加载类
	 * 
	 * @param o 任何参数
	 */
	public static void load(Object o) {
		if (o != null) load(o.getClass());
	}

	/**
	 * 加载某类
	 * 
	 * @param str 类名
	 */
	public static void load(String str) {
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
	public static <K, V, R, C extends Collection<R>> C mapToCollection(Map<K, V> map, BiFunction<K, V, R> func, C result) {
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
	public static <K, V, R, RC extends Collection<R>, VC extends Collection<V>> RC mutiMapToCollection(Map<K, VC> map, BiFunction<K, V, R> func, RC result) {
		map.forEach((k, vs) -> {
			vs.forEach(v -> result.add(func.apply(k, v)));
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
			ShareData.getLogger().warning("不能翻译字符串: " + msg);
			e.printStackTrace();
			return msg;
		}
	}

	/**
	 * 随机字符串
	 * 
	 * @param len 长度
	 * @return 字符串
	 */
	public static String randomString(int len) {
		char[] cs = new char[len];
		for (int i = 0; i < len; i++) cs[i] = RANDOM[(int) (Math.random() * RANDOM.length)];
		return new String(cs);
	}

	/**
	 * 模糊搜索
	 * 
	 * 
	 * @param text 给定字符串
	 * @param itr  内容
	 * @return 搜索结果
	 */
	public static <T> T search(@NonNull Object text, Iterator<T> itr) {
		T		found		= null;
		String	lowerName	= text.toString().toLowerCase(Locale.ENGLISH);
		int		delta		= Integer.MAX_VALUE;

		while (itr.hasNext()) {
			val	t	= itr.next();
			val	now	= t.toString();
			if (now.toLowerCase(Locale.ENGLISH).startsWith(lowerName)) {
				int curDelta = Math.abs(now.length() - lowerName.length());
				if (curDelta < delta) {
					found	= t;
					delta	= curDelta;
					if (curDelta == 0) break;
				}
			}
		}
		return found;
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
	public static <E> String serialize(Collection<E> c, char split, Function<? super E, String> translate) {
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
	public static <E, M extends Map<E, E>> M setToMap(Set<E> set, M map) {
		set.forEach(e -> map.put(e, e));
		return map;
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
	public static <T, R> ArrayList<R> translate(List<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		ArrayList<R> result = new ArrayList<>(list.size());
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
	public static <TK, TV, RK, RV, RM extends LinkedHashMap<RK, RV>> RM translate(Map<TK, TV> map, BiConsumer<Entry<TK, TV>, ResultEntry<RK, RV>> func,
			RM result) {
		Objects.requireNonNull(map, "map can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		ResultEntry<RK, RV> resultEntry = new ResultEntry<>();
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
	public static <T, R> HashSet<R> translate(Set<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		HashSet<R> result = new HashSet<>();
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
	public static <T, R> ArrayList<R> translateToList(Collection<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		ArrayList<R> result = new ArrayList<>(list.size());
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
	public static <T, R> HashSet<R> translateToSet(Collection<T> list, Function<T, R> func) {
		Objects.requireNonNull(list, "list can not be null.");
		Objects.requireNonNull(func, "func can not be null.");
		HashSet<R> result = new HashSet<>();
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
	public static void tryRun(boolean needRun, ThrowableRunnable<?> tr) {
		if (needRun) try {
			tr.run();
		} catch (Throwable e) {
			ShareData.getLogger().warning("尝试运行部分代码时出错:");
			e.printStackTrace();
		}
	}

	/**
	 * 尝试运行
	 * 
	 * @param tr 可抛出错误的代码体
	 */
	public static void tryRun(ThrowableRunnable<?> tr) {
		try {
			tr.run();
		} catch (Throwable e) {
			ShareData.getLogger().warning("尝试运行部分代码时出错:");
			e.printStackTrace();
		}
	}

	/**
	 * 尝试运行
	 * 
	 * @param r 代码体
	 */
	public static void tryRunNormal(Runnable r) {
		try {
			r.run();
		} catch (Throwable e) {
			ShareData.getLogger().warning("尝试运行部分代码时出错:");
			e.printStackTrace();
		}
	}

	/**
	 * 禁止实例化
	 */
	private Tool() {
	}

}
