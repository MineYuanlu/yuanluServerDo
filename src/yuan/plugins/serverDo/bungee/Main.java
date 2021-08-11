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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;

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

	/**
	 * 获取玩家<br>
	 * 模糊搜索, 借鉴Bukkit
	 * 
	 * @param name 玩家名
	 * @return 玩家
	 */
	public static ProxiedPlayer getPlayer(@NonNull String name) {
		ProxiedPlayer found = getMain().getProxy().getPlayer(name);
		if (found != null) return found;

		String	lowerName	= name.toLowerCase(Locale.ENGLISH);
		int		delta		= Integer.MAX_VALUE;
		val		var6		= getMain().getProxy().getPlayers().iterator();

		while (var6.hasNext()) {
			val player = var6.next();
			if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(lowerName)) {
				int curDelta = Math.abs(player.getName().length() - lowerName.length());
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
	}

	/**
	 * 发送数据
	 * 
	 * @param server 服务器
	 * @param buf    数据
	 */
	public static void send(ServerInfo server, @NonNull byte[] buf) {
		server.sendData(ShareData.BC_CHANNEL, buf);
	}

	/** */
	private void bstats() {
		// All you have to do is adding the following two lines in your onEnable method.
		// You can find the plugin ids of your plugins on the page
		// https://bstats.org/what-is-my-plugin-id
		int		pluginId	= 7158;							// <-- Replace with the id of your plugin!
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
	public void onEnable() {
		main = this;
		bstats();

		checkYuanluConfig();

		// 注册信道
		getProxy().registerChannel(ShareData.BC_CHANNEL);
		getProxy().getPluginManager().registerListener(this, this);
		getLogger().info(SHOW_NAME + "-启动(bungee)");
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
		val				type	= Channel.byId(ShareData.readInt(message, 0, -1));
		switch (type) {
		case PERMISSION: {
			val	permission	= Channel.Permission.parseC(message);
			val	allow		= player.hasPermission(permission);
			send(server, Channel.Permission.sendC(permission, allow));
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
		default:
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
		switch (id) {
		case 0x0:
			Channel.Tp.p0C_tpReq(buf, (target, type) -> {
				val targetPlayer = getPlayer(target);
				// TODO 搜索不到玩家
				send(targetPlayer, Channel.Tp.s2S_tpReq(player.getName(), player.getDisplayName(), type));
				send(player, Channel.Tp.s1S_tpReqReceive(targetPlayer.getName(), targetPlayer.getDisplayName()));
			});
			break;
		case 0x3:
			Channel.Tp.p3C_tpResp(buf, (who, allow) -> {
				val targetPlayer = getProxy().getPlayer(who);
				// TODO 搜索不到玩家
				send(player, Channel.Tp.s4S_tpRespReceive());
				send(targetPlayer, Channel.Tp.s5S_tpResp(player.getName(), allow));
			});
			break;
		case 0x6:
			Channel.Tp.p6C_tpThird(buf, (mover, target) -> {
				val	m	= getProxy().getPlayer(mover);
				val	t	= getProxy().getPlayer(target);
				// TODO 搜索不到玩家
				send(t, Channel.Tp.s8S_tpThird(m.getName()));
				send(player, Channel.Tp.s7S_tpThirdReceive());
			});
			break;
		case 0x9:
			Channel.Tp.p9C_tpReqThird(buf, (mover, target) -> {
				val	m	= getProxy().getPlayer(mover);
				val	t	= getProxy().getPlayer(target);
				// TODO 搜索不到玩家
				send(m, Channel.Tp.s2S_tpReq(t.getName(), t.getDisplayName(), 4));
				send(t, Channel.Tp.s2S_tpReq(m.getName(), m.getDisplayName(), 5));
				send(player, Channel.Tp.saS_tpReqThirdReceive(m.getName(), m.getDisplayName(), t.getName(), t.getDisplayName()));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Tp sub id:" + id + ", from: " + server.getInfo().getName());
			break;
		}
	}
}
