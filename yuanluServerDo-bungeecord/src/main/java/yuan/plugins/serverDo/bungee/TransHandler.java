/**
 *
 */
package yuan.plugins.serverDo.bungee;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.TransHome;
import yuan.plugins.serverDo.Channel.TransWarp;
import yuan.plugins.serverDo.ShareLocation;

/**
 * 转换处理器
 *
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransHandler {
	/** 所有接收到的家 */
	private static final HashMap<UUID, LinkedHashMap<String, ShareLocation>>	HOMES			= new HashMap<>();
	/** 所有接收到的家 */
	private static final Function<UUID, LinkedHashMap<String, ShareLocation>>	HOMES_MF		= x -> new LinkedHashMap<>();
	/** 所有接收到的家计数器 */
	private static int															HOMES_COUNTER	= 0;
	/** 所有接收到的地标 */
	private static final LinkedHashMap<String, ShareLocation>					WARPS			= new LinkedHashMap<>();
	/** 所有接收到的地标计数器 */
	private static int															WARPS_COUNTER	= 0;

	/**
	 * 接收家数据
	 *
	 * @param player 通道玩家
	 * @param home   家信息
	 */
	public static synchronized void receiveHome(ProxiedPlayer player, TransHome home) {
		if (home == null) {
			Main.send(player, Channel.TransHome.sendC(HOMES_COUNTER));
			HOMES_COUNTER = 0;

			HOMES.forEach((uuid, homes) -> //
			homes.forEach((name, loc) -> //
			Core.setHome(uuid, name, loc)));
			HOMES.clear();
		} else {
			HOMES_COUNTER++;
			HOMES.computeIfAbsent(home.getPlayer(), HOMES_MF).put(home.getName(), setServer(player, home.getLoc()));
		}
	}

	/**
	 * 接收地标数据
	 *
	 * @param player 通道玩家
	 * @param warp   地标信息
	 */
	public static synchronized void receiveWarp(ProxiedPlayer player, TransWarp warp) {
		if (warp == null) {
			Main.send(player, Channel.TransWarp.sendC(WARPS_COUNTER));
			WARPS_COUNTER = 0;

			WARPS.forEach(Core::setWarp);
			WARPS.clear();
		} else {
			WARPS_COUNTER++;
			WARPS.put(warp.getName(), setServer(player, warp.getLoc()));
		}
	}

	/**
	 * 设置坐标所在的服务器
	 *
	 * @param player 玩家
	 * @param loc    坐标
	 * @return 传入的坐标
	 */
	private static ShareLocation setServer(ProxiedPlayer player, ShareLocation loc) {
		loc.setServer(player.getServer().getInfo().getName());
		return loc;
	}

}
