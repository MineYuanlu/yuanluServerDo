package yuan.plugins.serverDo;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

/**
 * At的帮助类, 用于分析msg中的at字段
 *
 * @author yuanlu
 *
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class At {
	/** At字符 */
	public static final char	AT_CHAR		= '@';
	/** At字符 */
	public static final String	AT_STR		= String.valueOf(AT_CHAR);
	/** At颜色 */
	public static final String	AT_COLOR	= ChatColor.AQUA.toString();

	/**
	 * 获取msg中所有at的玩家
	 *
	 * @param msg   实际的聊天信息
	 * @param names 玩家名判断
	 * @return at玩家的流
	 */
	public static Stream<String> at(@NonNull String msg, @NonNull Predicate<String> names) {
		if (msg.indexOf(AT_CHAR) < 0) return Stream.empty();
		return Arrays.stream(msg.split(AT_STR, 0))//
				.skip(1)//
				.map(At::first)//
				.filter(name -> names.test(name));
	}

	/**
	 * 获取首字段
	 *
	 * @param msg msg
	 * @return 首字段
	 */
	private static String first(String msg) {
		return msg.split(" ", 2)[0];
	}

	/**
	 * 处理msg
	 *
	 * @param format 由format给定msg的颜色
	 * @param msg    实际的聊天信息
	 * @param names  玩家名判断
	 * @return 处理后的实际聊天信息
	 */
	public static String format(String format, @NonNull String msg, @NonNull Predicate<String> names) {
		if (msg.indexOf(AT_CHAR) < 0) return msg;

		format = format == null ? "" : getFormatColor(format);
		if (format.isEmpty()) format = ChatColor.RESET.toString();

		val ats = Arrays.stream((format + msg).split(AT_STR, -1))// 按At分割
				.map(At::new)//
				.toArray(At[]::new);
		for (int i = 1; i < ats.length; i++) ats[i].previous(ats[i - 1]);

		val msgNew = new StringBuilder();
		Arrays.stream(ats)//
				.map(at -> at.toString(names))//
				.forEach(msgNew::append);
		return msgNew.substring(msgNew.indexOf(AT_STR) + 1 + format.length());
	}

	/**
	 * 获取聊天格式的msg颜色
	 *
	 * @param format 聊天格式
	 * @return msg的颜色
	 */
	private static String getFormatColor(String format) {
		format = String.format(format, "", "\0");
		return ChatColor.getLastColors(format.substring(0, format.indexOf('\0')));
	}

	/** 当前字段的完整字符 */
	String	str;

	/** 当前字段的首字段(假设为name) */
	String	first;

	/** 当前字段的颜色 */
	String	color;

	/**
	 * 分析一个msg
	 *
	 * @param msg 分段的msg
	 */
	private At(String msg) {
		this(msg, first(msg), ChatColor.getLastColors(msg));
	}

	/**
	 * 由前一个at字段的颜色传播至此at字段
	 *
	 * @param at 前一个at字段
	 */
	private void previous(At at) {
		color = ChatColor.getLastColors(at.color + color);
	}

	/**
	 * 检测名字并转为at段
	 *
	 * @param names 所有玩家名
	 * @return 字符串
	 */
	private String toString(Predicate<String> names) {
		if (names.test(first)) {
			return String.format("%s%c%s%s%s", //
					AT_COLOR, //
					AT_CHAR, //
					first, //
					ChatColor.getLastColors(ChatColor.COLOR_CHAR + "r" + color), //
					str.substring(first.length())//
			);
		} else {
			return AT_STR + str;
		}
	}

	@Override
	public String toString() {
		return String.format("At [str=%s, first=%s, color=%s]", str, first, color.replace(ChatColor.COLOR_CHAR, '&'));
	}
}