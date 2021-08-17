/**
 * yuanlu
 * date: 2021年8月11日
 * file: Core.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bungee;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.bungee.ConfigManager.ConfFile;

/**
 * BC端核心
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Core {
	/** 版本不正确的服务器 */
	static final HashSet<ServerInfo>	BAD_SERVER			= new HashSet<>();
	/** 时间戳修正 */
	static final HashMap<String, Long>	TIME_AMEND			= new HashMap<>();

	/** 时间修正回调等待 */
	static final HashMap<String, Long>	TIME_AMEND_WAITER	= new HashMap<>();

	/** 当前隐身 */
	static final HashSet<UUID>			nowVanish			= new HashSet<>();
	/** 自动隐身 */
	static final HashSet<UUID>			alwaysVanish		= new HashSet<>();

	/**
	 * 自动隐身处理
	 * 
	 * @param player 玩家
	 */
	public static void autoVanish(ProxiedPlayer player) {
		val u = player.getUniqueId();
		if (alwaysVanish.contains(u)) {
			nowVanish.add(u);
			Main.send(player, Channel.Vanish.sendC(true));
		}
	}

	/**
	 * 检查TP
	 * 
	 * @param isSenior 是否是高级传送
	 * @param myServer 本服务器
	 * @param target   目标玩家
	 * @return 是否可以传送
	 */
	public static boolean canTp(boolean isSenior, String myServer, ProxiedPlayer target) {
		val targetServer = target.getServer().getInfo().getName();
		if (!ConfigManager.allowServer(targetServer)) return false;
		if (isSenior) return true;

		if (!ConfigManager.canTp(myServer, targetServer)) return false;
		if (hasVanish(target)) return false;

		return true;
	}

	/**
	 * 获取本服务器比子服务器快多少毫秒
	 * 
	 * @param server 服务器
	 * @return 时间戳修正
	 */
	public static long getTimeAmend(ServerInfo server) {
		if (server == null) return 0;
		val amend = TIME_AMEND.get(server.getName());
		return amend == null ? 0 : amend;
	}

	/**
	 * 是否拥有隐身
	 * 
	 * @param player 玩家
	 * @return 是否拥有隐身
	 */
	public static boolean hasVanish(ProxiedPlayer player) {
		return nowVanish.add(player.getUniqueId());
	}

	/**
	 * 开始时间修正检测
	 */
	public static void startTimeAmend() {
		val pack = Channel.TimeAmend.sendC();
		for (val server : Main.getMain().getProxy().getServers().values()) {
			if (server.getPlayers().isEmpty()) continue;
			TIME_AMEND_WAITER.put(server.getName(), System.currentTimeMillis());
			Main.send(server, pack);
		}
	}

	/**
	 * 切换隐身
	 * 
	 * @param player   玩家
	 * @param isAlways 是否自动隐身
	 * @return 切换后状态
	 */
	public static boolean switchVanish(ProxiedPlayer player, boolean isAlways) {
		val		set	= isAlways ? alwaysVanish : nowVanish;
		val		u	= player.getUniqueId();
		boolean	r;
		if (!(r = set.add(u))) set.remove(u);
		ConfigManager.saveConf(ConfFile.ALWAYS_VANISH);
		return r;
	}

	/**
	 * 时间修正回调
	 * 
	 * @param info   子服务器
	 * @param client 子服务器时间
	 */
	public static void timeAmendCallback(ServerInfo info, long client) {
		val start = TIME_AMEND_WAITER.remove(info.getName());
		if (start == null) return;
		val			end		= System.currentTimeMillis();
		final long	time	= end - start;
		if (time > 100) {
			ShareData.getLogger().warning("[时间修正] " + info.getName() + " 通信时间过长: " + time);
			return;
		}
		long shift = end - (client + time / 2);
		TIME_AMEND.put(info.getName(), shift);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[时间修正] " + info.getName() + " 偏移量:" + shift + ", 通信时长: " + time);
	}
}
