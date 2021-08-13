/**
 * 
 */
package yuan.plugins.serverDo.bungee;

import java.util.HashMap;
import java.util.HashSet;

import com.google.common.base.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.config.Configuration;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.Tool;

/**
 * 配置管理器
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

	/** 配置文件 */
	private static @Getter Configuration					config;
	/** tab替换 */
	private static @Getter @Setter String					tabReplaceNor;
	/** tab替换 */
	private static @Getter @Setter String					tabReplaceAll;

	/** 服务器信息 */
	private static byte[]									serverInfo;

	/** 出错 */
	private static @Getter boolean							errorGroup;

	/** 服务器组 */
	private static final HashMap<String, HashSet<String>>	GROUPS		= new HashMap<>();
	/** 禁用的服务器 */
	private static final HashSet<String>					BAN_SERVER	= new HashSet<>();

	/**
	 * 检测是否可以传送
	 * 
	 * @param s1 服务器1
	 * @param s2 服务器2
	 * @return 是否可以传送
	 */
	public static boolean canTp(String s1, String s2) {
		if (BAN_SERVER.contains(s1) || BAN_SERVER.contains(s2)) return false;
		if (Objects.equal(s1, s2)) return true;
		if (errorGroup) {
			if (Main.isDEBUG()) Main.getMain().getLogger().warning("error: 服务器组不存在");
			return false;
		}
		val group = GROUPS.get(s1);
		return group != null && group.contains(s2);
	}

	/**
	 * 加载组
	 * 
	 * @param config 配置文件
	 */
	private static void loadGroup(Configuration config) {
		val sg = config.getSection("server-group");
		if (sg == null) {
			errorGroup = true;
			Main.getMain().getLogger().warning("[SERVER GROUP] config error!");
			return;
		}
		for (val key : sg.getKeys()) {
			val group = sg.getStringList(key);
			for (val server : group) {
				HashSet<String> canTp = GROUPS.get(server);
				if (canTp == null) GROUPS.put(server, canTp = new HashSet<>());
				canTp.addAll(group);
			}
			if (ShareData.isDEBUG()) ShareData.getLogger().info("加载组 " + key + ": " + group);
		}

		BAN_SERVER.clear();
		BAN_SERVER.addAll(config.getStringList("server-ban"));
	}

	/**
	 * 发送BC信息给子服务器
	 * 
	 * @param server 服务器
	 */
	public static void sendBungeeInfoToServer(Server server) {
		Main.send(server, serverInfo);
	}

	/**
	 * 设置conf
	 * 
	 * @param config 要设置的 config
	 */
	public static void setConfig(Configuration config) {
		ConfigManager.config = config;
		val tabReplace = config.getString("player-tab-replace", "yl★:" + Tool.randomString(8));
		setTabReplaceNor(tabReplace + "-nor");
		setTabReplaceAll(tabReplace + "-all");
		loadGroup(config);

		serverInfo = Channel.ServerInfo.sendS(tabReplaceAll, tabReplaceNor, Main.getMain().getDescription().getVersion());
	}

	/**
	 * 检测是否启用服务器
	 * 
	 * @param server 服务器
	 * @return 此服务器是否启用本插件
	 */
	public static boolean allowServer(String server) {
		return !BAN_SERVER.contains(server);
	}

}
