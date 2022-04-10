/**
 * date: 2022年2月27日
 * author: yuanlu
 */
package yuan.plugins.serverDo.velocity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.Tool;

/**
 * Velocity端
 *
 * @author yuanlu
 *
 */
@Plugin(id = "yuanlu-server-do")
public final class Main {
	/** 调试模式 */
	private static @Getter boolean						DEBUG			= false;

	/** main */
	@Getter private static Main							main;
	/** 插件名称 用于信息提示 模板自动生成 */
	public final static String							SHOW_NAME		= ShareData.SHOW_NAME;
	/** BC通道标识符: {@link ShareData#BC_CHANNEL} */
	public static final ChannelIdentifier				BC_CHANNEL		= new LegacyChannelIdentifier(ShareData.BC_CHANNEL);
	/** 数据包队列 */
	private static final Map<ServerInfo, Queue<byte[]>>	PACKET_QUEUE	= new HashMap<>();

	/**
	 * 获取玩家<br>
	 * 模糊搜索, 借鉴Bukkit
	 *
	 * @param sender 发起者, 当其不为null时, 会检查服务器组
	 * @param name   玩家名
	 * @return 玩家
	 */
	public static Player getPlayer(Player sender, @NonNull String name) {
		val		serverConn	= sender == null ? null : sender.getCurrentServer().orElse(null);
		val		server		= serverConn == null ? null : serverConn.getServerInfo().getName();
		Player	found		= getMain().getProxy().getPlayer(name).orElse(null);
		if (found != null) {
			if (Core.canTp(server == null, server, found)) return found;
			return null;
		}

		String	lowerName	= name.toLowerCase(Locale.ENGLISH);
		int		delta		= Integer.MAX_VALUE;
		val		var6		= getMain().getProxy().getAllPlayers().iterator();
		while (var6.hasNext()) {
			val	player	= var6.next();
			val	pn		= player.getUsername();

			if (isDEBUG()) getMain().getLogger().info(String.format("寻找玩家: self=%s, search=%s, now=%s; match=%s, can=%s", //
					sender, name, pn, pn.toLowerCase(Locale.ENGLISH).startsWith(lowerName), Core.canTp(server == null, server, player)));
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
	public static List<Player> getPlayers(Player sender, @NonNull String name) {

		String	lowerName	= name.toLowerCase(Locale.ENGLISH);
		val		list		= new ArrayList<Player>();
		val		var6		= getMain().getProxy().getAllPlayers().iterator();
		val		serverConn	= sender == null ? null : sender.getCurrentServer().orElse(null);
		val		server		= serverConn == null ? null : serverConn.getServerInfo().getName();
		while (var6.hasNext()) {
			val	player	= var6.next();
			val	pn		= player.getUsername();
			if (pn.toLowerCase(Locale.ENGLISH).startsWith(lowerName) && Core.canTp(server == null, server, player)) {
				list.add(player);
			}
		}
		return list;

	}

	/** @return 插件容器 */
	public static PluginContainer getPluginContainer() {
		return getMain().getProxy().getPluginManager().ensurePluginContainer(getMain());
	}

	/**
	 * 发送数据(toServer)
	 *
	 * @param player 玩家
	 * @param buf    数据
	 */
	public static void send(@NonNull Player player, @NonNull byte[] buf) {
		val server = player.getCurrentServer()
				.orElseThrow(() -> new IllegalStateException("Player \"" + player.getUsername() + "\" is not connected to any server."));
		send(server, buf);
		if (isDEBUG()) getMain().getLogger().info("发送: " + server.getServerInfo().getName() + " " + Arrays.toString(buf));
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 * @return true if the message was sent immediately, false otherwise if queue is
	 *         true, it has been queued, if itis false it has been discarded.
	 */
	public static boolean send(@NonNull RegisteredServer server, @NonNull byte[] buf) {
		for (val player : server.getPlayersConnected()) {
			send(player, buf);
			if (isDEBUG()) getMain().getLogger().info("发送: " + server.getServerInfo().getName() + " " + Arrays.toString(buf));
			return true;
		}
		if (isDEBUG()) getMain().getLogger().info("发送(失败): " + server.getServerInfo().getName() + " " + Arrays.toString(buf));
		return false;
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 */
	public static void send(@NonNull ServerConnection server, @NonNull byte[] buf) {
		server.sendPluginMessage(BC_CHANNEL, buf);
		if (isDEBUG()) getMain().getLogger().info("发送: " + server.getServerInfo().getName() + " " + Arrays.toString(buf));
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 * @deprecated 不推荐使用info发送信息,请使用: {@link #send(RegisteredServer, byte[])} 代替
	 * @return true if the message was sent immediately, false otherwise if queue is
	 *         true, it has been queued, if itis false it has been discarded.
	 * @see #send(RegisteredServer, byte[])
	 */
	@Deprecated
	public static boolean send(@NonNull ServerInfo server, @NonNull byte[] buf) {
		val serverConn = getMain().getProxy().getAllPlayers().stream()//
				.map(Player::getCurrentServer)//
				.map(o -> o.orElse(null))//
				.filter(s -> s != null && Objects.equals(server, s.getServerInfo()))//
				.findAny().orElse(null);
		if (serverConn == null) return false;
		send(serverConn, buf);
		return true;
	}

	/**
	 * 发送数据
	 *
	 * @param server 服务器
	 * @param buf    数据
	 * @return true if the message was sent immediately, false otherwise if queue is
	 *         true, it has been queued, if itis false it has been discarded.
	 */
	public static boolean sendQueue(@NonNull RegisteredServer server, @NonNull byte[] buf) {
		for (val player : server.getPlayersConnected()) {
			send(player, buf);
			if (isDEBUG()) getMain().getLogger().info("发送: " + server.getServerInfo().getName() + " " + Arrays.toString(buf));
			return true;
		}
		synchronized (PACKET_QUEUE) {
			PACKET_QUEUE.computeIfAbsent(server.getServerInfo(), //
					i -> new LinkedList<>()).add(buf);
		}
		return false;
	}

	/** server */
	@Getter private final ProxyServer		proxy;

	/** logger */
	@Getter private final Logger			logger;

	/** 插件目录 */
	@Getter private final Path				dataFolder;

	/** bstats */
	@Getter private final Metrics.Factory	metricsFactory;

	/** class loader */
	@Getter private final ClassLoader		classLoader;

	/** 时间修正任务 */
	private ScheduledTask					timeAmendTask;

	@SuppressWarnings("javadoc")
	@Inject
	private Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
		main				= this;
		this.proxy			= server;
		this.logger			= logger;
		this.dataFolder		= dataDirectory;
		this.metricsFactory	= metricsFactory;
		this.classLoader	= getClass().getClassLoader();
	}

	/** */
	private void bstats() {

		Metrics metrics = metricsFactory.make(this, 14492);
		metrics.addCustomChart(new SimplePie("pls_count", () -> {
			int count = 0;
			for (val pl : getProxy().getPluginManager().getPlugins()) {
				if (pl.getDescription().getAuthors().contains("yuanlu")) count++;
			}
			return Integer.toString(count);
		}));

		metrics.addCustomChart(new MultiLineChart("packets", Channel::getPackCount));
	}

	/** 检查中央配置文件 */
	private void checkYuanluConfig() {
		val				configFile	= getDataFolder().getParent().resolve("yuanlu").resolve("config.yml");
		Configuration	config		= null;

		if (Files.isRegularFile(configFile)) try (val in = Files.newInputStream(configFile)) {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(in);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!Files.exists(configFile) || config == null) {
			DEBUG = false;
		} else {
			DEBUG = config.getBoolean("debug", false);
		}
		ShareData.setDEBUG(DEBUG);
		ShareData.setLogger(getLogger());

	}

	/**
	 * 登录事件<br>
	 * 唤起数据包发送队列(对应bc的send.queue实现)
	 *
	 * @param event ServerConnectedEvent
	 * @deprecated EVENT
	 */
	@Deprecated
	@Subscribe
	public void event_callSendQueue(@NonNull ServerPostConnectEvent event) {
		val	player	= event.getPlayer();
		val	server	= player.getCurrentServer().orElseThrow(NullPointerException::new).getServerInfo();
		synchronized (PACKET_QUEUE) {

			val que = PACKET_QUEUE.get(server);
			if (que == null) return;

			try {
				val buf = que.poll();
				if (buf == null) return;
				send(player, buf);
			} finally {
				if (que.isEmpty()) PACKET_QUEUE.remove(server);
			}
		}
	}

	/**
	 * EVENT
	 *
	 * @param e 插件消息
	 * @deprecated BUNGEE
	 */
	@Deprecated
	@Subscribe
	public void event_onPluginMessage(PluginMessageEvent e) {
		if (isDEBUG()) getLogger().info("PluginMessageEvent:" + e.getIdentifier().getId() + " " + ShareData.BC_CHANNEL.equals(e.getIdentifier().getId()));
		if (!ShareData.BC_CHANNEL.equals(e.getIdentifier().getId())) return;
		e.setResult(PluginMessageEvent.ForwardResult.handled());
		if (isDEBUG()) getLogger().info("e.getTar:  " + e.getTarget().getClass());
		if (!(e.getSource() instanceof ServerConnection) || !(e.getTarget() instanceof Player)) return;
		ServerConnection	server	= (ServerConnection) e.getSource();
		Player				player	= (Player) e.getTarget();
		val					message	= e.getData();
		val					id		= ShareData.readInt(message, 0, -1);
		val					type	= Channel.byId(id);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] receive: " + player.getUsername() + "-" + type + ": " + Arrays.toString(message));
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
			if (!allow) Core.BAD_SERVER.add(server.getServerInfo());
			break;
		}
		case COOLDOWN: {
			val	end		= Channel.Cooldown.parseC(message) + Core.getTimeAmend(server.getServerInfo());
			val	uuid	= player.getUniqueId();
			for (val s : getProxy().getAllServers()) {
				send(s, Channel.Cooldown.broadcast(uuid, end - Core.getTimeAmend(s.getServerInfo())));
			}
		}
		case TIME_AMEND: {
			val time = Channel.TimeAmend.parseC(message);
			Core.timeAmendCallback(server.getServerInfo(), time);
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
	 * 初始化
	 *
	 * @param event ProxyInitializeEvent
	 * @deprecated EVENT
	 */
	@Deprecated
	@Subscribe(order = PostOrder.LAST)
	public void event_onProxyInitialization(ProxyInitializeEvent event) {
		bstats();

		checkYuanluConfig();

		Tool.load(Channel.class);
		ConfigManager.init(loadFile("proxy-config.yml"));
		// 注册信道

		getProxy().getChannelRegistrar().register(BC_CHANNEL);
		getLogger().info(SHOW_NAME + "-启动(velocity)");

		startTimeAmendLoop();
	}

	/**
	 * 关闭
	 *
	 * @param event ProxyShutdownEvent
	 * @deprecated EVENT
	 */
	@Deprecated
	@Subscribe(order = PostOrder.LAST)
	public void event_onProxyShutdown(ProxyShutdownEvent event) {
		val timeAmendTask = this.timeAmendTask;
		if (timeAmendTask != null) timeAmendTask.cancel();
	}

	/**
	 * EVENT
	 *
	 * @param e 服务器连接
	 * @deprecated EVENT
	 */
	@Deprecated
	@Subscribe
	public void event_onServerPostConnect(ServerPostConnectEvent e) {
		val server = e.getPlayer().getCurrentServer().orElseThrow(NullPointerException::new);
		send(server, Channel.VersionCheck.sendS());
		ConfigManager.sendBungeeInfoToServer(server);
		Core.autoVanish(e.getPlayer(), server);
	}

	/**
	 * 加载配置
	 *
	 * @param fileName 配置文件名，例如{@code "config.yml"}
	 * @return 配置文件
	 * @author yuanlu
	 */
	public Configuration loadFile(String fileName) {
		try {
			val file = getDataFolder().resolve(fileName);
			if (!Files.exists(file.getParent())) Files.createDirectories(file.getParent());
			if (DEBUG) getLogger().info(file.toAbsolutePath().toString());
			if (!Files.exists(file)) {
				try (val in = classLoader.getResourceAsStream(fileName)) {
					Files.copy(in, file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try (val in = Files.newInputStream(file)) {
				return ConfigurationProvider.getProvider(YamlConfiguration.class).load(in);
			}
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

		val file = getDataFolder().resolve(fileName);
		if (Files.isRegularFile(file)) return loadFile(fileName);

		for (String old : oldNames) {
			val f = getDataFolder().resolve(old);
			try {
				if (!Files.isRegularFile(f)) continue;
				Files.copy(f, file);
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
	 * 接收插件back消息
	 *
	 * @param player 玩家
	 * @param server 服务器
	 * @param buf    数据
	 */
	private void onPluginBackMessage(Player player, ServerConnection server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] Back:" + id);
		switch (id) {
		case 0:
			Channel.Back.p0C_tellTp(buf, back -> {
				final byte[] backPack = Channel.Back.s1S_tellTp( //
						back.getPlayer(), //
						back.getLoc(), //
						player.getCurrentServer() //
								.orElseThrow(() -> new IllegalStateException("Player \"" + player.getUsername() + "\" is not connected to any server.")) //
								.getServerInfo()//
								.getName());
				if (back.isServer()) {// 目标是服务器
					val toServer = getProxy().getServer(back.getTo()).orElse(null);
					if (toServer == null) {
						ShareData.getLogger().warning("[PACKAGE] Can not found back's toServer: " + back.getTo() + //
						", from: " + server.getServerInfo().getName());
						return;
					}
					send(toServer, backPack);
				} else {// 目标是玩家
					val toPlayer = getProxy().getPlayer(back.getTo()).orElse(null);
					if (toPlayer == null) {
						ShareData.getLogger().warning("[PACKAGE] Can not found back's toPlayer: " + back.getTo() + //
						", from: " + server.getServerInfo().getName());
						return;
					}
					send(toPlayer, backPack);
				}
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Back sub id:" + id + ", from: " + server.getServerInfo().getName());
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
	private void onPluginHomeMessage(Player player, ServerConnection server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] Home:" + id);
		switch (id) {
		case 0:
			Channel.Home.p0C_setHome(buf, (name, loc, amount) -> {
				loc.setServer(server.getServerInfo().getName());
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
				val					serverName	= server.getServerInfo().getName();
				val					WARPS		= Core.getHomes(player);
				ArrayList<String>	w1			= new ArrayList<>(), w2 = new ArrayList<>();
				WARPS.forEach((name, loc) -> (Core.canTp(serverName, loc.getServer()) ? w1 : w2).add(name));
				send(player, Channel.Home.s4S_listHomeResp(w1, w2));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Home sub id:" + id + ", from: " + server.getServerInfo().getName());
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
	private void onPluginTpLocMessage(Player player, ServerConnection server, byte[] buf) {
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
			ShareData.getLogger().warning("[PACKAGE]Bad Tp sub id:" + id + ", from: " + server.getServerInfo().getName());
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
	private void onPluginTpMessage(Player player, ServerConnection server, byte[] buf) {
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
					send(player, Channel.Tp.s1S_tpReqReceive(player.getUsername(), player.getUsername()));
				} else {
					send(targetPlayer, Channel.Tp.s2S_tpReq(player.getUsername(), player.getUsername(), type));
					send(player, Channel.Tp.s1S_tpReqReceive(targetPlayer.getUsername(), targetPlayer.getUsername()));
				}
			});
			break;
		case 0x3:
			Channel.Tp.p3C_tpResp(buf, (who, allow) -> {
				val targetPlayer = getProxy().getPlayer(who).orElse(null);
				send(player, Channel.Tp.s4S_tpRespReceive(targetPlayer != null));
				if (targetPlayer != null) send(targetPlayer, Channel.Tp.s5S_tpResp(player.getUsername(), allow));
			});
			break;
		case 0x6:
			Channel.Tp.p6C_tpThird(buf, (mover, target) -> {
				val	m	= getProxy().getPlayer(mover).orElse(null);
				val	t	= getProxy().getPlayer(target).orElse(null);
				if (m == null || t == null) {
					send(player, Channel.Tp.s7S_tpThirdReceive(false, false));
				} else {
					send(t, Channel.Tp.s8S_tpThird(m.getUsername()));
					val targetServer = t.getCurrentServer()//
							.orElseThrow(() -> new IllegalStateException("Player \"" + player.getUsername() + "\" is not connected to any server.")) //
							.getServer();

					m.createConnectionRequest(targetServer);
					player.createConnectionRequest(targetServer).connect().thenAcceptAsync(r -> {
						val success = r.getStatus() == Status.SUCCESS || r.getStatus() == Status.ALREADY_CONNECTED;
						send(player, Channel.Tp.s7S_tpThirdReceive(success, false/* design for BC */));
					});
				}
			});
			break;
		case 0x9:
			Channel.Tp.p9C_tpReqThird(buf, (mover, target, code) -> {
				val	m	= getPlayer((code & 1) > 0 ? null : player, mover);
				val	t	= getPlayer((code & 1) > 0 ? null : player, target);
				if (m != null && t != null) {
					send(m, Channel.Tp.s2S_tpReq(t.getUsername(), t.getUsername(), 4));
					send(t, Channel.Tp.s2S_tpReq(m.getUsername(), m.getUsername(), 5));
				}
				send(player, Channel.Tp.saS_tpReqThirdReceive(m == null ? "" : m.getUsername(), m == null ? "" : m.getUsername(), //
						t == null ? "" : t.getUsername(), t == null ? "" : t.getUsername()));
			});
			break;
		case 0xb:
			Channel.Tp.pbC_cancel(buf, target -> {
				val t = getProxy()//
						.getPlayer(target)//
						.orElseThrow(() -> new IllegalStateException("Player \"" + player.getUsername() + "\" is not connected to any server.")) //
				;
				send(t, Channel.Tp.scS_cancel(player.getUsername()));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Tp sub id:" + id + ", from: " + server.getServerInfo().getName());
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
	private void onPluginWarpMessage(Player player, ServerConnection server, byte[] buf) {
		byte id = Channel.getSubId(buf);
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] Warp:" + id);
		switch (id) {
		case 0:
			Channel.Warp.p0C_setWarp(buf, (name, loc) -> {
				loc.setServer(server.getServerInfo().getName());
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
				val					serverName	= server.getServerInfo().getName();
				val					WARPS		= ConfigManager.WARPS;
				ArrayList<String>	w1			= new ArrayList<>(), w2 = new ArrayList<>();
				WARPS.forEach((name, loc) -> (Core.canTp(serverName, loc.getServer()) ? w1 : w2).add(name));
				send(player, Channel.Warp.s4S_listWarpResp(w1, w2));
			});
			break;
		default:
			ShareData.getLogger().warning("[PACKAGE]Bad Warp sub id:" + id + ", from: " + server.getServerInfo().getName());
			break;
		}
	}

	/**
	 * EVENT<br>
	 * 无效
	 * 
	 * @param e Tab响应
	 * @deprecated EVENT
	 */
	@Deprecated
	@Subscribe
	public void onTab(TabCompleteEvent e) {
		if (isDEBUG()) getLogger().info("Tab Event: " + e);
		TabHandler.TabComplete(e);
	}

	/**
	 * 时间修正
	 */
	private void startTimeAmendLoop() {

		final long timeAmend = ConfigManager.getConfig().getLong("timeAmend", 1000 * 60 * 5);

		if (timeAmend > 0) timeAmendTask = getProxy().getScheduler()//
				.buildTask(this, Core::startTimeAmend)//
				.delay(timeAmend, TimeUnit.MILLISECONDS)//
				.repeat(timeAmend, TimeUnit.MILLISECONDS)//
				.schedule();

	}

}
