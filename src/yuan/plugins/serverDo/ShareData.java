/**
 * yuanlu
 * date: 2021年8月9日
 * file: ShareData.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 共享数据
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShareData {
	/** 日志记录器 */
	private static @Setter @Getter Logger	logger;
	/** 插件名称 用于信息提示 模板自动生成 */
	public final static String				SHOW_NAME	= "元路跨服操作插件";
	/** 配置文件 */
	public static final Charset				CHARSET		= Charset.forName("UTF-8");
	/** BC通道名 */
	public static final String				BC_CHANNEL	= "yuanluSDo";

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
