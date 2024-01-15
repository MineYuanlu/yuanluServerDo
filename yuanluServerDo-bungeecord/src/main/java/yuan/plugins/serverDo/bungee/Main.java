/**
 * auto: <br>
 * user: yuanlu<br>
 * date: 星期五 10 04 2020
 */
package yuan.plugins.serverDo.bungee;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.bstats.bungeecord.Metrics;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SimplePie;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import yuan.plugins.serverDo.At;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.Tool;

/**
 * BC端
 *
 * @author yuanlu
 *
 */
public class Main extends Plugin implements Listener {
	/** 实例 */
	private static @Getter Main		main;
	/** 插件名称 用于信息提示 模板自动生成 */
	public final static String		SHOW_NAME	= ShareData.SHOW_NAME;

	/** 调试模式 */
	private static @Getter boolean	DEBUG		= false;

	/** 时间修正循环器 */
	private static Thread			timeAmendLooper;

	/**
	 * 获取玩家<br>
	 * 模糊搜索, 借鉴Bukkit
	 *
	 * @param sender 发起者, 当其不为null时, 会检查服务器组
	 * @param name   玩家名
	 * @return 玩家
	 */
	public static ProxiedPlayer getPlayer(ProxiedPlayer sender, @NonNull String name) {
		val				server	= sender == null ? null : sender.getServer().getInfo().getName();
		ProxiedPlayer	found	= getMain().getProxy().getPlayer(name);
		if (found != null) {
			if (Core.canTp(server == null, server, found)) return found;
			return null;
		}

		String	lowerName	= name.toLowerCase(Locale.ENGLISH);
		int		delta		= Integer.MAX_VALUE;
		val		var6		= getMain().getProxy().getPlayers().iterator();
		while (var6.hasNext()) {
			val	player	= var6.next();
			val	pn		= player.getName();
			if (pn.toLowerCase(Locale.ENGLISH).startsWith(lowerName) && Core.canTp(server == null, server, player)) {
				int curDelta = Math.abs(pn.length() - lowerName.length());
				if (curDelta < delta) {
					found	= player;
					delta	= curDelta;
				}
				if (curDelta == 0) break;
			}
		}
		return found;

	}

	/**
	 * 获取玩家<br>
	 * 模糊搜索, 借鉴Bukkit
	 *
	 * @param sender 发起者, 当其不为null时, 会检查服务器组
	 * @param name   玩家名
	 * @return 玩家
	 */
	public static List<ProxiedPlayer> getPlayers(ProxiedPlayer sender, @NonNull String name) {

		String	lowerName	= name.toLowerCase(Locale.ENGLISH);
		val		list		= new ArrayList<ProxiedPlayer>();
		val		var6		= getMain().getProxy().getPlayers().iterator();
		val		server		= sender == null ? null : sender.getServer().getInfo().getName();
		while (var6.hasNext()) {
			val	player	= var6.next();
			val	pn		= player.getName();
			if (pn.toLowerCase(Locale.ENGLISH).startsWith(lowerName) && Core.canTp(server == null, server, player)) {
				list.add(player);
			}
		}
		return list;

	}

	/**
	 * 发送数据(toServer)
	 *
	 * @param player 玩家
	 * @param buf    数据
	 */
	public static void send(ProxiedPlayer player, byte @NonNull [] buf) {
		val server = player.getServer();
		send(server, buf);
		if (isDEBUG()) getMain().getLogger().info("发送: " + server.getInfo().getName() + " " + Arrays.toString(buf));
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 */
	public static void send(Server server, byte @NonNull [] buf) {
		server.sendData(ShareData.BC_CHANNEL, buf);
		if (isDEBUG()) getMain().getLogger().info("发送: " + server.getInfo().getName() + " " + Arrays.toString(buf));
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 * @return true if the message was sent immediately, false otherwise if queue is
	 *         true, it has been queued, if itis false it has been discarded.
	 */
	public static boolean send(ServerInfo server, byte @NonNull [] buf) {
		val result = server.sendData(ShareData.BC_CHANNEL, buf, false);
		if (isDEBUG()) getMain().getLogger().info("发送: " + server.getName() + " " + Arrays.toString(buf));
		return result;
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 * @return true if the message was sent immediately, false otherwise if queue is
	 *         true, it has been queued, if itis false it has been discarded.
	 */
	public static boolean sendQueue(ServerInfo server, byte @NonNull [] buf) {
		val result = server.sendData(ShareData.BC_CHANNEL, buf, true);
		if (isDEBUG()) getMain().getLogger().info("发送: " + server.getName() + " " + Arrays.toString(buf));
		return result;
	}

	/** */
	private void bstats() {
		// All you have to do is adding the following two lines in your onEnable method.
		// You can find the plugin ids of your plugins on the page
		// https://bstats.org/what-is-my-plugin-id
		int		pluginId	= 12396;						// <-- Replace with the id of your plugin!
		Metrics	metrics		= new Metrics(this, pluginId);

		metrics.addCustomChart(new SimplePie("pls_count", () -> {
			int count = 0;
			for (Plugin pl : getMain().getProxy().getPluginManager().getPlugins()) {
				if (pl.getDescription().getAuthor().contains("yuanlu")) count++;
			}
			return Integer.toString(count);
		}));

		metrics.addCustomChart(new MultiLineChart("packets", Channel::getPackCount));
	}

	/** 检查中央配置文件 */
	private void checkYuanluConfig() {
		val				yuanluFolder	= new File(getDataFolder().getParentFile(), "yuanlu");
		val				configFile		= new File(yuanluFolder, "config.yml");
		Configuration	config			= null;
		try {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
		} catch (FileNotFoundException ignored) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!configFile.exists() || config == null) {
			DEBUG = false;
		} else {
			DEBUG = config.getBoolean("debug", false);
		}
		ShareData.setDEBUG(DEBUG);
		ShareData.setLogger(getLogger());

	}

	/**
	 * 加载配置
	 *
	 * @param fileName 配置文件名，例如{@code "config.yml"}
	 * @return 配置文件
	 * @author yuanlu
	 */
	public Configuration loadFile(String fileName) {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		try {
			File file = new File(getDataFolder(), fileName);
			if (DEBUG) getLogger().info(file.getAbsolutePath());
			if (!file.exists()) {
				try (InputStream in = getResourceAsStream(fileName)) {
					Files.copy(in, file.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 加载配置
	 *
	 * @param fileName 配置文件名，例如{@code "config.yml"}
	 * @param oldNames 配置文件旧的命名, 如果发现则会自动重命名
	 * @return 配置文件
	 * @author yuanlu
	 */
	public Configuration loadFile(String fileName, String... oldNames) {

		File file = new File(getDataFolder(), fileName);
		if (file.exists()) return loadFile(fileName);

		for (String old : oldNames) {
			val f = getDataFolder().toPath().resolve(old);
			try {
				if (!Files.isRegularFile(f)) continue;
				Files.copy(f, file.toPath());
				Files.move(f, f.getParent().resolve(f.getFileName() + ".old"));
				return loadFile(fileName);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		return loadFile(fileName);
	}

	/**
	 * EVENT
	 *
	 * @param e Chat
	 * @deprecated BUNGEE
	 */
	@Deprecated
	@EventHandler
	public void onChat(ChatEvent e) {
		if (!ConfigManager.isUseAt()) return;
		val	ats		= At.at(e.getMessage(), name -> getProxy().getPlayer(name) != null);
		val	pack	= Channel.PlaySound.play(Channel.PlaySound.Sounds.AT);
		ats.distinct().map(getProxy()::getPlayer).forEach(player -> send(player, pack));
	}

	@Override
	public void onDisable() {
		if (timeAmendLooper != null) timeAmendLooper = null;
		ConfigManager.closeSave();
	}

	@Override
	public void onEnable() {
		main = this;
		bstats();

		checkYuanluConfig();

		Tool.load(Channel.class);
		ConfigManager.init(loadFile("proxy-config.yml", "bc-config.yml"));
		// 注册信道
		getProxy().registerChannel(ShareData.BC_CHANNEL);
		getProxy().getPluginManager().registerListener(this, this);
		getLogger().info(SHOW_NAME + "-启动(bungee)");

		startTimeAmendLoop();
	}

	/**
	 * 接收插件back消息
	 *
	 * @param player 玩家
	 * @param server 服务器
	 * @param buf    数据
	 */
	private void onPluginBackMessage(ProxiedPlayer player, Server server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] Back:" + id);
		switch (id) {
		case 0:
			Channel.Back.p0C_tellTp(buf, back -> {
				if (back.isServer()) {// 目标是服务器
					val toServer = getProxy().getServerInfo(back.getTo());
					if (toServer == null) {
						ShareData.getLogger().warning("[PACKAGE] Can not found back's toServer: " + back.getTo() + ", from: " + server.getInfo().getName());
						return;
					}
					send(toServer, Channel.Back.s1S_tellTp(back.getPlayer(), back.getLoc(), player.getServer().getInfo().getName()));
				} else {// 目标是玩家
					val toPlayer = getProxy().getPlayer(back.getTo());
					if (toPlayer == null) {
						ShareData.getLogger().warning("[PACKAGE] Can not found back's toPlayer: " + back.getTo() + ", from: " + server.getInfo().getName());
						return;
					}
					send(toPlayer, Channel.Back.s1S_tellTp(back.getPlayer(), back.getLoc(), player.getServer().getInfo().getName()));
				}
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Back sub id:" + id + ", from: " + server.getInfo().getName());
			break;
		}
	}

	/**
	 * 接收插件home消息
	 *
	 * @param player 玩家
	 * @param server 服务器
	 * @param buf    数据
	 */
	private void onPluginHomeMessage(ProxiedPlayer player, Server server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] Home:" + id);
		switch (id) {
		case 0:
			Channel.Home.p0C_setHome(buf, (name, loc, amount) -> {
				loc.setServer(server.getInfo().getName());
				val result = Core.setHome(player, name, loc, amount);
				send(player, Channel.Home.s0S_setHomeResp(result));
			});
			break;
		case 1:
			Channel.Home.p1C_delHome(buf, name -> {
				val success = Core.setHome(player, name, null, 0);
				send(player, Channel.Home.s1S_delHomeResp(success));
			});
			break;
		case 2:
			Channel.Home.p2C_searchHome(buf, name -> {
				name = Core.searchHome(player, name);
				if (name == null) {
					send(player, Channel.Home.s2S_searchHomeResp("", ""));
				} else {
					send(player, Channel.Home.s2S_searchHomeResp(name, Core.getHome(player, name).getServer()));
				}

			});
			break;
		case 3:
			Channel.Home.p3C_tpHome(buf, name -> Core.tpLocation(player, Core.getHome(player, name), Channel.Home::s3S_tpHomeResp));
			break;
		case 4:
			Channel.Home.p4C_listHome(buf, () -> {
				val					serverName	= server.getInfo().getName();
				val					WARPS		= Core.getHomes(player);
				ArrayList<String>	w1			= new ArrayList<>(), w2 = new ArrayList<>();
				WARPS.forEach((name, loc) -> (Core.canTp(serverName, loc.getServer()) ? w1 : w2).add(name));
				send(player, Channel.Home.s4S_listHomeResp(w1, w2));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Home sub id:" + id + ", from: " + server.getInfo().getName());
			break;
		}
	}

	/**
	 * EVENT
	 *
	 * @param e 插件消息
	 * @deprecated BUNGEE
	 */
	@Deprecated
	@EventHandler
	public void onPluginMessage(PluginMessageEvent e) {
		if (!ShareData.BC_CHANNEL.equals(e.getTag())) return;
		e.setCancelled(true);
		if (!(e.getSender() instanceof Server)) return;
		ProxiedPlayer	player	= (ProxiedPlayer) e.getReceiver();
		Server			server	= (Server) e.getSender();
		val				message	= e.getData();
		val				id		= ShareData.readInt(message, 0, -1);
		val				type	= Channel.byId(id);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] receive: " + player.getName() + "-" + type + ": " + Arrays.toString(message));
		switch (Objects.requireNonNull(type, "unknown type id: " + id)) {
		case PERMISSION: {
			val	permission	= Channel.Permission.parseC(message);
			val	allow		= player.hasPermission(permission);
			send(player, Channel.Permission.sendC(permission, allow));
			break;
		}
		case TP: {
			onPluginTpMessage(player, server, message);
			break;
		}
		case VERSION_CHECK: {
			val allow = Channel.VersionCheck.parseS(message);
			if (!allow) Core.BAD_SERVER.add(server.getInfo());
			break;
		}
		case COOLDOWN: {
			val	end		= Channel.Cooldown.parseC(message) + Core.getTimeAmend(server.getInfo());
			val	uuid	= player.getUniqueId();
			for (val s : getProxy().getServers().values()) {
				send(s, Channel.Cooldown.broadcast(uuid, end - Core.getTimeAmend(s)));
			}
		}
		case TIME_AMEND: {
			val time = Channel.TimeAmend.parseC(message);
			Core.timeAmendCallback(server.getInfo(), time);
			break;
		}
		case VANISH: {
			val	always	= Channel.Vanish.parse(message);
			val	inHide	= Core.switchVanish(player, always);
			send(player, Channel.Vanish.sendC(inHide));
			break;
		}
		case TP_LOC: {
			onPluginTpLocMessage(player, server, message);
			break;
		}
		case WARP: {
			onPluginWarpMessage(player, server, message);
			break;
		}
		case HOME: {
			onPluginHomeMessage(player, server, message);
			break;
		}
		case BACK: {
			onPluginBackMessage(player, server, message);
			break;
		}
		case TRANS_HOME: {
			Channel.TransHome.parseS(message, h -> TransHandler.receiveHome(player, h));
			break;
		}
		case TRANS_WARP: {
			Channel.TransWarp.parseS(message, w -> TransHandler.receiveWarp(player, w));
			break;
		}
		case SERVER_INFO:
		default:
			ShareData.getLogger().warning("[channel] BAD PACKAGE: " + type);
			break;

		}
	}

	/**
	 * 接收插件tpLoc消息
	 *
	 * @param player 玩家
	 * @param server 服务器
	 * @param buf    数据
	 */
	private void onPluginTpLocMessage(ProxiedPlayer player, Server server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] TPLoc:" + id);
		switch (id) {
		case 0:
			Channel.TpLoc.p0C_tpLoc(buf, (loc, targetServer) -> {
				loc.setServer(targetServer);
				Core.tpLocation(player, loc, Channel.TpLoc::s0S_tpLocResp);
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Tp sub id:" + id + ", from: " + server.getInfo().getName());
			break;
		}
	}

	/**
	 * 接收插件tp消息
	 *
	 * @param player 玩家
	 * @param server 服务器
	 * @param buf    数据
	 */
	private void onPluginTpMessage(ProxiedPlayer player, Server server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] TP:" + id);
		switch (id) {
		case 0x0:
			Channel.Tp.p0C_tpReq(buf, (target, type) -> {
				val isSenior = type < 1;
				if (isSenior) type = -type;
				val targetPlayer = getPlayer(isSenior ? null : player, target);
				if (targetPlayer == null) {
					send(player, Channel.Tp.s1S_tpReqReceive("", ""));
				} else if (targetPlayer.equals(player)) {
					send(player, Channel.Tp.s1S_tpReqReceive(player.getName(), player.getDisplayName()));
				} else {
					send(targetPlayer, Channel.Tp.s2S_tpReq(player.getName(), player.getDisplayName(), type));
					send(player, Channel.Tp.s1S_tpReqReceive(targetPlayer.getName(), targetPlayer.getDisplayName()));
				}
			});
			break;
		case 0x3:
			Channel.Tp.p3C_tpResp(buf, (who, allow) -> {
				val targetPlayer = getProxy().getPlayer(who);
				send(player, Channel.Tp.s4S_tpRespReceive(targetPlayer != null));
				if (targetPlayer != null) send(targetPlayer, Channel.Tp.s5S_tpResp(player.getName(), allow));
			});
			break;
		case 0x6:
			Channel.Tp.p6C_tpThird(buf, (mover, target) -> {
				val	m	= getProxy().getPlayer(mover);
				val	t	= getProxy().getPlayer(target);
				if (m == null || t == null) {
					send(player, Channel.Tp.s7S_tpThirdReceive(false, false));
				} else {
					send(t, Channel.Tp.s8S_tpThird(m.getName()));
					val targetServer = t.getServer().getInfo();
//					if (server.getInfo().getName().equals(targetServer.getName())) {//TODO BUG 未实际执行m.connect
//						send(player, Channel.Tp.s7S_tpThirdReceive(true, false));
//					} else

					m.connect(targetServer, (success, e) -> {
						if (e != null) e.printStackTrace();
						send(player, Channel.Tp.s7S_tpThirdReceive(success, e != null));
					}, Reason.COMMAND);
				}
			});
			break;
		case 0x9:
			Channel.Tp.p9C_tpReqThird(buf, (mover, target, code) -> {
				val	m	= getPlayer((code & 1) > 0 ? null : player, mover);
				val	t	= getPlayer((code & 1) > 0 ? null : player, target);
				if (m != null && t != null) {
					send(m, Channel.Tp.s2S_tpReq(t.getName(), t.getDisplayName(), 4));
					send(t, Channel.Tp.s2S_tpReq(m.getName(), m.getDisplayName(), 5));
				}
				send(player, Channel.Tp.saS_tpReqThirdReceive(m == null ? "" : m.getName(), m == null ? "" : m.getDisplayName(), //
						t == null ? "" : t.getName(), t == null ? "" : t.getDisplayName()));
			});
			break;
		case 0xb:
			Channel.Tp.pbC_cancel(buf, target -> {
				val t = getProxy().getPlayer(target);
				send(t, Channel.Tp.scS_cancel(player.getName()));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Tp sub id:" + id + ", from: " + server.getInfo().getName());
			break;
		}
	}

	/**
	 * 接收插件warp消息
	 *
	 * @param player 玩家
	 * @param server 服务器
	 * @param buf    数据
	 */
	private void onPluginWarpMessage(ProxiedPlayer player, Server server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] Warp:" + id);
		switch (id) {
		case 0:
			Channel.Warp.p0C_setWarp(buf, (name, loc) -> {
				loc.setServer(server.getInfo().getName());
				Core.setWarp(name, loc);
				send(player, Channel.Warp.s0S_setWarpResp());
			});
			break;
		case 1:
			Channel.Warp.p1C_delWarp(buf, name -> {
				val success = Core.setWarp(name, null);
				send(player, Channel.Warp.s1S_delWarpResp(success));
			});
			break;
		case 2:
			Channel.Warp.p2C_searchWarp(buf, name -> {
				name = Core.searchWarp(name);
				if (name == null) {
					send(player, Channel.Warp.s2S_searchWarpResp("", ""));
				} else {
					send(player, Channel.Warp.s2S_searchWarpResp(name, Core.getWarp(name).getServer()));
				}

			});
			break;
		case 3:
			Channel.Warp.p3C_tpWarp(buf, name -> Core.tpLocation(player, Core.getWarp(name), Channel.Warp::s3S_tpWarpResp));
			break;
		case 4:
			Channel.Warp.p4C_listWarp(buf, () -> {
				val					serverName	= server.getInfo().getName();
				val					WARPS		= ConfigManager.WARPS;
				ArrayList<String>	w1			= new ArrayList<>(), w2 = new ArrayList<>();
				WARPS.forEach((name, loc) -> (Core.canTp(serverName, loc.getServer()) ? w1 : w2).add(name));
				send(player, Channel.Warp.s4S_listWarpResp(w1, w2));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Warp sub id:" + id + ", from: " + server.getInfo().getName());
			break;
		}
	}

	/**
	 * EVENT
	 *
	 * @param e 服务器连接
	 * @deprecated BUNGEE
	 */
	@Deprecated
	@EventHandler
	public void onServerConnected(ServerConnectedEvent e) {
		send(e.getServer(), Channel.VersionCheck.sendS());
		ConfigManager.sendBungeeInfoToServer(e.getServer());
		Core.autoVanish(e.getPlayer(), e.getServer());
	}

	/**
	 * EVENT
	 *
	 * @param e Tab响应
	 * @deprecated BUNGEE
	 */
	@Deprecated
	@EventHandler
	public void onTab(TabCompleteResponseEvent e) {
		TabHandler.TabCompleteResponse(e);
	}

	/**
	 * 时间修正
	 */
	private void startTimeAmendLoop() {
		val timeAmend = ConfigManager.getConfig().getLong("timeAmend", 1000 * 60 * 5);
		if (timeAmend > 0) {

			timeAmendLooper = new Thread("yuanluServerDo-timeAmend") {
				Thread thread;

				@Override
				public void run() {
					thread = timeAmendLooper;
					while (true) {
						if (thread != timeAmendLooper) return;
						try {
							Core.startTimeAmend();
							sleep(timeAmend);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			timeAmendLooper.start();
		}
	}

}
