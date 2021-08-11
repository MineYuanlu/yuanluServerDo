/**
 * yuanlu
 * date: 2021年8月11日
 * file: Core.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bungee;

import java.util.HashSet;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;

/**
 * BC端核心
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Core {
	/** 版本不正确的服务器 */
	static final HashSet<ServerInfo> BAD_SERVER = new HashSet<>();
}
