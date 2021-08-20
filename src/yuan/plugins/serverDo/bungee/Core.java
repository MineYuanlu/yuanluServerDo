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
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.ShareLocation;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.bungee.ConfigManager.ConfFile;
import yuan.plugins.serverDo.bungee.ConfigManager.PlayerConfFile;

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
	 * @param server 玩家所在服务器
	 */
	public static void autoVanish(ProxiedPlayer player, Server server) {
		val u = player.getUniqueId();
		if (alwaysVanish.contains(u)) {
			nowVanish.add(u);
			Main.send(server, Channel.Vanish.sendC(true));
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
	 * 检查服务器间是否可以传送
	 * 
	 * @param myServer     本服务器
	 * @param targetServer 目标服务器
	 * @return 是否可以传送
	 */
	public static boolean canTp(String myServer, String targetServer) {
		if (!ConfigManager.allowServer(targetServer)) return false;
		if (!ConfigManager.canTp(myServer, targetServer)) return false;
		return true;
	}

	/**
	 * 获取地标
	 * 
	 * @param player 玩家
	 * @param name   名称
	 * @return 坐标
	 */
	public static ShareLocation getHome(@NonNull ProxiedPlayer player, @NonNull String name) {
		return ConfigManager.HOMES.get(player.getUniqueId()).get(name);
	}

	/**
	 * 获取家集合
	 * 
	 * @param player 玩家
	 * @return homes
	 */
	public static HashMap<String, ShareLocation> getHomes(@NonNull ProxiedPlayer player) {
		return ConfigManager.HOMES.get(player.getUniqueId());
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
	 * 获取地标
	 * 
	 * @param name 名称
	 * @return 坐标
	 */
	public static ShareLocation getWarp(@NonNull String name) {
		return ConfigManager.WARPS.get(name);
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
	 * 搜索家
	 * 
	 * @param player 玩家
	 * @param name   搜索名
	 * @return 匹配名
	 */
	public static String searchHome(@NonNull ProxiedPlayer player, @NonNull String name) {
		return Tool.search(name, ConfigManager.HOMES.get(player.getUniqueId()).keySet().iterator());
	}

	/**
	 * 搜索地标
	 * 
	 * @param name 搜索名
	 * @return 匹配名
	 */
	public static String searchWarp(@NonNull String name) {
		return Tool.search(name, ConfigManager.WARPS.keySet().iterator());
	}

	/**
	 * 设置/删除家
	 * 
	 * @param player 玩家
	 * @param name   家名称
	 * @param loc    家坐标
	 * @param amount 家最大数量
	 * @return true: 成功删除/成功设置<br>
	 *         false:不存在/已满
	 */
	public static boolean setHome(@NonNull ProxiedPlayer player, @NonNull String name, ShareLocation loc, int amount) {
		val		home	= ConfigManager.HOMES.get(player.getUniqueId());
		boolean	result;
		if (loc == null) result = home.remove(name) != null;
		else if ((loc = loc.clone()).getServer() == null) throw new IllegalArgumentException("[HOME] Null sever: " + name);
		else {
			result = (home.containsKey(name) || home.size() < amount);
			if (result) home.put(name, loc);
		}

		ConfigManager.saveConf(PlayerConfFile.HOME, player);
		return result;
	}

	/**
	 * 设置/删除地标
	 * 
	 * @param name 地标名称
	 * @param loc  地标坐标
	 * @return true: 成功删除/覆盖<br>
	 *         false:不存在/新建
	 */
	public static boolean setWarp(@NonNull String name, ShareLocation loc) {
		boolean result;
		if (loc == null) result = ConfigManager.WARPS.remove(name) != null;
		else if ((loc = loc.clone()).getServer() == null) throw new IllegalArgumentException("[WARP] Null sever: " + name);
		else result = ConfigManager.WARPS.put(name, loc) != null;
		ConfigManager.saveConf(ConfFile.WARP);
		return result;
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

	/**
	 * 传送坐标
	 * 
	 * @param player   玩家
	 * @param loc      目标点
	 * @param callback 传送回调数据生成
	 */
	public static void tpLocation(@NonNull ProxiedPlayer player, ShareLocation loc, @NonNull Function<Boolean, byte[]> callback) {
		val	nowServer		= player.getServer();
		val	targetServer	= loc == null ? null : Main.getMain().getProxy().getServerInfo(loc.getServer());
		if (targetServer == null) {
			Main.send(nowServer, callback.apply(false));
			return;
		}
		Main.sendQueue(targetServer, Channel.TpLoc.s1S_tpLoc(loc, player.getName()));
		if (nowServer.getInfo().getName().equals(targetServer.getName())) {
			Main.send(nowServer, callback.apply(true));
		} else player.connect(targetServer, (success, e) -> {
			Main.send(nowServer, callback.apply(success));
			if (e != null) e.printStackTrace();
		}, Reason.COMMAND);
	}

}
