/**
 * yuanlu
 * date: 2021年8月9日
 * file: ShareData.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * 共享数据
 *
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShareData {
	/**
	 * Tab补全类型
	 *
	 * @author yuanlu
	 *
	 */
	@SuppressWarnings("javadoc")
	public enum TabType {
		TP_ALL, TP_NORMAL, WARP, HOME, AT;

		private static final int						length;
		private static final HashMap<String, TabType>	DATAS	= new HashMap<>();
		static {
			val vs = values();
			length = Integer.toString(vs.length - 1, Character.MAX_VALUE).length();
			char[] emptyChar = new char[length];
			Arrays.fill(emptyChar, '_');
			String empty = new String(emptyChar);
			for (val v : vs) {
				v.key = (Integer.toString(v.ordinal(), Character.MAX_VALUE) + empty).substring(0, length);
				DATAS.put(v.key, v);
			}
		}

		public static TabType getType(@NonNull String tab, @NonNull String arg) {
			if (!arg.startsWith(tab)) return null;
			val tlen = tab.length();
			if (arg.length() < tlen + length) return null;
			arg = arg.substring(tlen, tlen + length);
			return DATAS.get(arg);
		}

		public static String getValue(@NonNull String tab, @NonNull String arg) {
			if (!arg.startsWith(tab)) return null;
			val tlen = tab.length() + length;
			if (arg.length() < tlen) return null;
			return arg.substring(tlen).toLowerCase();
		}

		private String key;

		@Override
		public String toString() {
			return key;
		}
	}

	/** 日志记录器 */
	private static @Setter @Getter Logger	logger;
	/** 是否开启DEBUG模式 */
	private static @Setter @Getter boolean	DEBUG		= false;
	/** 插件名称 用于信息提示 模板自动生成 */
	public final static String				SHOW_NAME	= "元路跨服操作插件";
	/** 配置文件 */
	public static final Charset				CHARSET		= Charset.forName("UTF-8");

	/** BC通道名 */
	public static final String				BC_CHANNEL	= "bc:yuanlu-SDo".toLowerCase();

	/**
	 * 读取一个int
	 *
	 * @param bs     byte[]
	 * @param offset 偏移下标
	 * @param def    默认值
	 * @return 读取的int<br>
	 *         null: 超出长度
	 */
	public static int readInt(byte[] bs, int offset, int def) {
		if (bs.length < offset + 4) return def;
		return (bs[offset++] << 24) + (bs[offset++] << 16) + (bs[offset++] << 8) + (bs[offset++] << 0);
	}

}
