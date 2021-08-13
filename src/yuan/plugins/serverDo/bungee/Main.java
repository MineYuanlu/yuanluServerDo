/**
 * auto: <br>
 * user: yuanlu<br>
 * date: 星期五 10 04 2020
 */
package yuan.plugins.serverDo.bungee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
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
	private static @Getter Main			main;
	/** 插件名称 用于信息提示 模板自动生成 */
	public final static String			SHOW_NAME	= ShareData.SHOW_NAME;

	/** 调试模式 */
	private static @Getter boolean		DEBUG		= false;

	/** 数据包统计 */
	private static final AtomicInteger	PACK_COUNT	= new AtomicInteger(0);

	/** 时间修正循环器 */
	private static Thread				timeAmendLooper;

	/**
	 * 获取玩家<br>
	 * 模糊搜索, 借鉴Bukkit
	 * 
	 * @param sender 发起者, 当其不为null时, 会检查服务器组
	 * @param name   玩家名
	 * @return 玩家
	 */
	public static ProxiedPlayer getPlayer(ProxiedPlayer sender, @NonNull String name) {
		ProxiedPlayer found = getMain().getProxy().getPlayer(name);
		if (found != null) return found;

		String	lowerName	= name.toLowerCase(Locale.ENGLISH);
		int		delta		= Integer.MAX_VALUE;
		val		var6		= getMain().getProxy().getPlayers().iterator();
		val		server		= sender == null ? null : sender.getServer().getInfo().getName();
		while (var6.hasNext()) {
			val	player	= var6.next();
			val	pn		= player.getName();
			if (pn.toLowerCase(Locale.ENGLISH).startsWith(lowerName)
					&& (server == null || ConfigManager.canTp(player.getServer().getInfo().getName(), server))) {
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
	 * 发送数据(toServer)
	 * 
	 * @param player 玩家
	 * @param buf    数据
	 */
	public static void send(ProxiedPlayer player, @NonNull byte[] buf) {
		send(player.getServer(), buf);
	}

	/**
	 * 发送数据
	 * 
	 * @param server 服务器
	 * @param buf    数据
	 */
	public static void send(Server server, @NonNull byte[] buf) {
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
	public static boolean send(ServerInfo server, @NonNull byte[] buf) {
		val result = server.sendData(ShareData.BC_CHANNEL, buf, false);
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

		// Optional: Add custom charts
		metrics.addCustomChart(new Metrics.SingleLineChart("packets", () -> PACK_COUNT.getAndSet(0)));
		metrics.addCustomChart(new Metrics.SimplePie("pls_count", () -> {
			int count = 0;
			for (Plugin pl : getMain().getProxy().getPluginManager().getPlugins()) {
				if (pl.getDescription().getAuthor().contains("yuanlu")) count++;
			}
			return Integer.toString(count);
		}));
	}

	/** 检查中央配置文件 */
	private void checkYuanluConfig() {
		val				yuanluFolder	= new File(getDataFolder().getParentFile(), "yuanlu");
		val				configFile		= new File(yuanluFolder, "config.yml");
		Configuration	config			= null;
		try {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
		} catch (IOException e) {
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
	 * EVENT
	 * 
	 * @param e 补全响应
	 * @deprecated BUNGEE
	 */
	@Deprecated
	@EventHandler
	public void event(TabCompleteResponseEvent e) {
		val list = e.getSuggestions();
		if (list.size() != 1) return;
		val	str		= list.get(0);
		val	tabAll	= ConfigManager.getTabReplaceAll();
		val	tabNor	= ConfigManager.getTabReplaceNor();
		if (str != null) {
			final String	request;
			final boolean	isAll;
			if (str.startsWith(tabAll)) {
				request	= str.substring(tabAll.length()).toLowerCase();
				isAll	= true;
			} else if (str.startsWith(tabNor)) {
				request = str.substring(tabNor.length()).toLowerCase();
				isAll	= false;
			} else return;
			list.clear();
			val server = (Server) e.getSender();
			if (server == null) return;
			val target = server.getInfo().getName();
			for (val p : getProxy().getPlayers()) {
				val	info	= p.getServer().getInfo().getName();
				val	name	= p.getName();
				if (name.toLowerCase().startsWith(request) && //
						(isAll || ConfigManager.canTp(info, target))) //
					list.add(name);
			}
			if (list.isEmpty()) list.add(request);
		}
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

	@Override
	public void onDisable() {
		if (timeAmendLooper != null) timeAmendLooper = null;
	}

	@Override
	public void onEnable() {
		main = this;
		bstats();

		checkYuanluConfig();

		Tool.load(Channel.class);
		ConfigManager.setConfig(loadFile("bc-config.yml"));
		// 注册信道
		getProxy().registerChannel(ShareData.BC_CHANNEL);
		getProxy().getPluginManager().registerListener(this, this);
		getLogger().info(SHOW_NAME + "-启动(bungee)");

		startTimeAmendLoop();
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
		System.out.println("CHANNEL:" + e.getTag() + ", " + ShareData.BC_CHANNEL.equals(e.getTag()) + ", " + (e.getSender() instanceof Server));
		if (!ShareData.BC_CHANNEL.equals(e.getTag())) return;
		e.setCancelled(true);
		if (!(e.getSender() instanceof Server)) return;
		ProxiedPlayer	player	= (ProxiedPlayer) e.getReceiver();
		Server			server	= (Server) e.getSender();
		val				message	= e.getData();
		val				type	= Channel.byId(ShareData.readInt(message, 0, -1));
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] receive: " + player.getName() + "-" + type + ": " + Arrays.toString(message));
		switch (type) {
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
		case SERVER_INFO:
		default:
			ShareData.getLogger().warning("[channel] BAD PACKAGE: " + type);
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
		byte id = buf[4/* 包头偏移 */ ];
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] TP:" + id);
		switch (id) {
		case 0x0:
			Channel.Tp.p0C_tpReq(buf, (target, type) -> {
				val targetPlayer = getPlayer(type == 1 || type == 3 ? player : null, target);
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
					m.connect(t.getServer().getInfo(), (success, e) -> {
						if (e != null) e.printStackTrace();
						send(player, Channel.Tp.s7S_tpThirdReceive(success, e != null));
					}, Reason.COMMAND);
				}
			});
			break;
		case 0x9:
			Channel.Tp.p9C_tpReqThird(buf, (mover, target) -> {
				val	m	= getPlayer(null, mover);
				val	t	= getPlayer(null, target);
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
	}

	/**
	 * 
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
