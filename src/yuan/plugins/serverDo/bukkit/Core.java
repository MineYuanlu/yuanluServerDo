/**
 * yuanlu
 * date: 2021年8月9日
 * file: Core.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.experimental.FieldDefaults;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BiBoolConsumer;
import yuan.plugins.serverDo.Channel.Package.BiPlayerConsumer;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.WaitMaintain;
import yuan.plugins.serverDo.bukkit.cmds.CmdTpaccept;

/**
 * Bukkit端的核心组件
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Core implements PluginMessageListener, MESSAGE, Listener {
	/** 回调队列 */
	public static final class CallbackQueue implements Runnable {
		/** 队列 */
		Runnable[]	runnables;
		/** 队列指针 */
		int			index;

		@Override
		public synchronized void run() {
			val r = runnables[index++];
			if (index == runnables.length) runnables = null;
			r.run();
		}

		/**
		 * 传入一组任务, 按顺序执行
		 * 
		 * @param tasks 运行任务
		 */
		public synchronized void task(Runnable... tasks) {
			if (runnables != null) throw new IllegalStateException("This callback queue is running");
			runnables	= tasks;
			index		= 0;
			run();
		}

	}

	/** 配置数据 */
	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class DatasConf {
		/** 无传送延时的权限 */
		String	noDelayPermission		= null;
		/** 传送延时(秒) */
		long	delay					= 3;
		/** 无冷却的权限 */
		String	noCooldownPermission	= null;
		/** 冷却(秒) */
		long	cooldown				= 100;
		/** 超时时长(秒) */
		long	overtime				= 120;
		/** 安全传送 */
		boolean	safeLocation			= false;
	}

	/** 监听回调的对象 */
	@Value
	private static final class ListenCallBackObj {
		/** 检查数据 */
		Object	checker;
		/** 处理器 */
		Object	handler;

		@Override
		public boolean equals(Object o) {
			if (o instanceof ListenCallBackObj) return Tool.equals(((ListenCallBackObj) o).checker, checker);
			else return Tool.equals(o, o);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(checker);
		}
	}

	/** tab处理器 */
	public static final class TabHandler {
		/** tab替换内容 */
		private static final HashMap<UUID, String>	TAB_REPLACE_ALL	= new HashMap<>();
		/** tab替换内容 */
		private static final HashMap<UUID, String>	TAB_REPLACE_NOR	= new HashMap<>();

		/**
		 * 获取补全列表
		 * 
		 * @param player 玩家
		 * @param arg    输入
		 * @param all    全部列表
		 * @return tabReplace 补全列表(由BC解析)
		 */
		public static List<String> getTabReplace(Player player, String arg, boolean all) {
			val tab = (all ? TabHandler.TAB_REPLACE_ALL : TabHandler.TAB_REPLACE_NOR).get(player.getUniqueId());
			if (tab == null) return Collections.singletonList(arg);
			return Collections.singletonList(tab + arg);
		}
	}

	/**
	 * Tp数据包处理
	 * 
	 * @author yuanlu
	 *
	 */
	public static final class TpHandler {
		/** 传送类型接收者消息 */
		private static final Msg[] tpTypeReceiverMsg;
		static {
			String str[] = "tp tpa tphere tpahere mover target".split(" ");
			tpTypeReceiverMsg = new Msg[str.length];
			for (int i = 0; i < 4; i++) tpTypeReceiverMsg[i] = Main.getMain().mes("cmd." + str[i] + ".receiver");
			for (int i = 4; i < 6; i++) tpTypeReceiverMsg[i] = Main.getMain().mes("cmd.tp.third-" + str[i]);
		}

		/**
		 * 等待加入服务器后传送<br>
		 * 被传送者{@code ->}传送目标
		 */
		private static final ConcurrentHashMap<String, String>	WAIT_JOIN_TP	= new ConcurrentHashMap<>();

		/** 禁止移动的玩家 */
		private static final HashSet<UUID>						BAN_MOVE		= new HashSet<>();

		/**
		 * 进入冷却
		 * 
		 * @param player 玩家
		 */
		private static void checkCooldown(@NonNull Player player) {
			if (Conf.noCooldownPermission != null && player.hasPermission(Conf.noCooldownPermission)) return;
			val end = System.currentTimeMillis() + Conf.cooldown * 1000;
			Main.send(player, Channel.Cooldown.sendS(end));
		}

		/**
		 * 检查延时传送
		 * 
		 * @param player 玩家
		 * @param time   延时时长
		 * @param r      检查完成
		 */
		private static void checkDelay(@NonNull Player player, long time, Runnable r) {
			WaitMaintain.add(BAN_MOVE, player.getUniqueId(), time * 1000, r);
			System.out.println(BAN_MOVE);
		}

		/**
		 * 处理Tp数据包
		 * 
		 * @param player 玩家
		 * @param type   通道类型(必须为Tp)
		 * @param buf    数据包
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void handleTpMessage(Player player, Channel type, byte[] buf) {
			if (type != Channel.TP) throw new InternalError("Bad Type");
			byte		id		= buf[4/* 包头偏移 */];
			Consumer	handler	= null;
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] TP: " + id);
			switch (id) {
			case 0x1:
				handler = h -> Channel.Tp.p1S_searchResult(buf, (BiConsumer<String, String>) h);
				break;
			case 0x2:
				Channel.Tp.p2S_tpReq(buf, (name, display, tpType) -> {
					tpTypeReceiverMsg[tpType].send(player, name, display, Conf.getOvertime());
					if (tpType == 1 || tpType == 3) {
						CmdTpaccept.addTpReq(player, name, display, tpType == 3);
					}
				});
				break;
			case 0x4:
				handler = h -> Channel.Tp.p4S_tpRespReceive(buf, (BoolConsumer) h);
				break;
			case 0x5:
				Channel.Tp.p5S_tpResp(buf, (who, allow) -> {
					callBack(player, type, id + "-" + who, h -> ((BoolConsumer) h).accept(allow));
				});
				break;
			case 0x7:
				handler = h -> Channel.Tp.p7S_tpThirdReceive(buf, (BiBoolConsumer) h);
				break;
			case 0x8:
				Channel.Tp.p8S_tpThird(buf, name -> {
					val mover = Bukkit.getPlayerExact(name);
					if (mover != null) toToLocal(mover, player, -1, false);
					else WAIT_JOIN_TP.put(name, player.getName());
				});
				break;
			case 0xa:
				handler = h -> Channel.Tp.paS_tpReqThirdReceive(buf, (BiPlayerConsumer) h);
				break;
			case 0xc:
				Channel.Tp.pcS_cancel(buf, from -> CmdTpaccept.cancelReq(player, from));
				break;
			default:
				ShareData.getLogger().warning("[PACKAGE]Bad Tp sub id:" + id + ", from: " + player.getName());
				break;
			}
			if (handler != null) callBack(player, type, id, handler);
		}

		/**
		 * 本地传送
		 * 
		 * @param mover        移动者
		 * @param target       目标
		 * @param waitTime     传送等待时间
		 * @param needCooldown 是否需要检查冷却
		 */
		private static void toToLocal(@NonNull Player mover, @NonNull Player target, long waitTime, boolean needCooldown) {
			if (waitTime > 0) {
				checkDelay(mover, Conf.delay, () -> toToLocal(mover, target, -1, needCooldown));
				return;
			}
			Bukkit.getScheduler().runTask(Main.getMain(), () -> mover.teleport(target));
			if (needCooldown) checkCooldown(mover);
		}

		/**
		 * 远程传送
		 * 
		 * @param player       操控玩家
		 * @param mover        移动者
		 * @param target       目标
		 * @param waitTime     传送等待时间
		 * @param needCooldown 是否需要检查冷却
		 */
		private static void tpToRemote(@NonNull Player player, @NonNull String mover, @NonNull String target, long waitTime, boolean needCooldown) {
			if (ShareData.isDEBUG())
				ShareData.getLogger().info(String.format("[tpTo] remote: %s->%s, wait: %s, cd: %s", mover, target, waitTime, needCooldown));
			if (waitTime > 0) {
				checkDelay(player, Conf.delay, () -> tpToRemote(player, mover, target, -1, needCooldown));
				return;
			}
			listenCallBack(player, Channel.TP, 7, (BiBoolConsumer) (success, error) -> {
				if (error) BC_ERROR.send(player);
				else if (!success) BC_PLAYER_OFF.send(player);
			});
			Main.send(player, Channel.Tp.s6C_tpThird(mover, target));
			if (needCooldown) checkCooldown(player);
		}
	}

	/** 配置数据 */
	private static final DatasConf	Conf		= new DatasConf();

	/** 单例 */
	public static final Core		INSTANCE	= new Core();
	/*
	 * tp,tpa,tpahere,tphere tpaccept,tpcancel
	 * 
	 * warp,home,spawn
	 * 
	 */

	/** 回调等待 */
	private static final EnumMap<Channel, Map<UUID, ArrayList<ListenCallBackObj>>> CALL_BACK_WAITER = new EnumMap<>(Channel.class);
	static {
		for (val c : Channel.values()) CALL_BACK_WAITER.put(c, new HashMap<>());
	}

	/** 版本检查通过的玩家集合 */
	private static final HashSet<UUID>			ALLOW_PLAYERS	= new HashSet<>();

	/** 传送冷却 */
	private static final HashMap<UUID, Long>	COOLDOWN		= new HashMap<UUID, Long>();

	/**
	 * 回调
	 * 
	 * @param player   玩家
	 * @param channel  通道
	 * @param checker  验证数据
	 * @param consumer 处理器接收
	 * @return 是否成功运行
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean callBack(@NonNull Player player, @NonNull Channel channel, Object checker, Consumer consumer) {
		val map = CALL_BACK_WAITER.get(channel);
		synchronized (map) {
			val objs = map.get(player.getUniqueId());
			for (Iterator<ListenCallBackObj> itr = objs.iterator(); itr.hasNext();) {
				val e = itr.next();
				if (Tool.equals(e.getChecker(), checker)) {
					itr.remove();
					if (ShareData.isDEBUG()) ShareData.getLogger().info("[callback] call:" + e.getChecker() + ", consumer: " + consumer);
					if (consumer != null) try {
						consumer.accept(e.getHandler());
					} catch (Throwable err) {
						throw new RuntimeException("回调函数执行失败, 玩家: " + player.getName() + ", 频道: " + channel + ", 标记: " + checker, err);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 获取等待时间
	 * 
	 * @param player 玩家
	 * @return >0: 玩家的等待时间<br>
	 *         =0: 无等待时间<br>
	 *         <0: 玩家还在冷却中
	 */
	public static long getWaitTime(Player player) {
		val	cooldownEnd	= COOLDOWN.get(player.getUniqueId());
		val	now			= System.currentTimeMillis();
		if (cooldownEnd != null && cooldownEnd - now > 0) return now - cooldownEnd;
		if (Conf.getNoDelayPermission() != null && player.hasPermission(Conf.getNoDelayPermission())) return 0;
		val delay = Conf.getDelay();
		return delay > 0 ? delay : 0;
	}

	/**
	 * 初始化数据
	 * 
	 * @param conf 总配置文件
	 */
	public static void init(ConfigurationSection conf) {
		// init Conf
		Conf.noDelayPermission		= conf.getString("setting.permission.no-delay", Conf.noCooldownPermission);
		Conf.delay					= conf.getLong("setting.teleport-delay", Conf.delay);
		Conf.noCooldownPermission	= conf.getString("setting.permission.no-cooldown", Conf.noCooldownPermission);
		Conf.cooldown				= conf.getLong("setting.teleport-cooldown", Conf.cooldown);
		Conf.overtime				= conf.getLong("setting.request-overtime", Conf.overtime);
		Conf.safeLocation			= conf.getBoolean("setting.use-safeLocation", Conf.safeLocation);

		WaitMaintain.setT_User(Conf.getOvertime() * 1000);
	}

	/**
	 * 监听回调
	 * 
	 * @param player  玩家
	 * @param channel 通道
	 * @param checker 验证数据
	 * @param maxTime 等待时间
	 * @param handler 处理器
	 */
	public static void listenCallBack(@NonNull Player player, @NonNull Channel channel, Object checker, long maxTime, @NonNull Object handler) {
		val map = CALL_BACK_WAITER.get(channel);
		synchronized (map) {
			WaitMaintain.add(map, player.getUniqueId(), new ListenCallBackObj(checker, handler), maxTime, ArrayList::new,
					() -> M_TIME_OUT.send(player, channel));
		}

	}

	/**
	 * 监听回调
	 * 
	 * @param player  玩家
	 * @param channel 通道
	 * @param checker 验证数据
	 * @param handler 处理器
	 */
	public static void listenCallBack(@NonNull Player player, @NonNull Channel channel, Object checker, @NonNull Object handler) {
		listenCallBack(player, channel, checker, WaitMaintain.T_Net, handler);
	}

	/**
	 * 权限检查
	 * 
	 * @param player     玩家
	 * @param permission 权限
	 * @param next       成功的回调函数
	 */
	public static void permissionCheck(@NonNull CommandSender player, String permission, @NonNull Runnable next) {
		if (permission == null || permission.isEmpty() || player instanceof ConsoleCommandSender || player.hasPermission(permission)) {
			next.run();
		} else if (player instanceof Player) {
			Player p = ((Player) player);
			listenCallBack(p, Channel.PERMISSION, permission, next);
			Main.send(p, Channel.Permission.sendS(permission));
		}
	}

	/**
	 * 移除回调
	 * 
	 * @param player  玩家
	 * @param channel 通道
	 * @param checker 验证数据
	 * @return 是否成功移除
	 */
	public static boolean removeCallBack(@NonNull Player player, @NonNull Channel channel, Object checker) {
		return callBack(player, channel, checker, null);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求
	 * 
	 * @param mover        移动者
	 * @param target       目标
	 * @param waitTime     传送等待时间
	 * @param needCooldown 是否需要检查冷却
	 */
	public static void tpTo(Player mover, String target, long waitTime, boolean needCooldown) {
		val localTarget = Main.getMain().getServer().getPlayerExact(target);
		if (localTarget != null) TpHandler.toToLocal(mover, localTarget, waitTime, needCooldown);
		else TpHandler.tpToRemote(mover, mover.getName(), target, waitTime, needCooldown);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求
	 * 
	 * @param player   操控玩家
	 * @param mover    移动者
	 * @param target   目标
	 * @param waitTime 传送等待时间
	 */
	public static void tpTo(Player player, String mover, String target, long waitTime) {
		val	localMover	= Main.getMain().getServer().getPlayerExact(mover);
		val	localTarget	= Main.getMain().getServer().getPlayerExact(target);
		if (localMover != null && localTarget != null) TpHandler.toToLocal(localMover, localTarget, waitTime, false);
		else TpHandler.tpToRemote(player, mover, target, waitTime, false);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求
	 * 
	 * @param mover    移动者
	 * @param target   目标
	 * @param waitTime 传送等待时间
	 */
	public static void tpTo(String mover, Player target, long waitTime) {
		val localMover = Main.getMain().getServer().getPlayerExact(mover);
		if (localMover != null) TpHandler.toToLocal(localMover, target, waitTime, false);
		else TpHandler.tpToRemote(target, mover, target.getName(), waitTime, false);
	}

	/**
	 * 
	 * @param e 事件
	 * @deprecated BUKKIT
	 */
	@Deprecated
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(@NonNull PlayerJoinEvent e) {
		val p = e.getPlayer();
		if (p == null) return;
		val to = TpHandler.WAIT_JOIN_TP.remove(p.getName());
		if (to == null) return;
		tpTo(p, to, -1, false);
	}

	/**
	 * 
	 * @param e 事件
	 * @deprecated BUKKIT
	 */
	@Deprecated
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerMove(@NonNull PlayerMoveEvent e) {
		val uid = e.getPlayer().getUniqueId();
		if (!TpHandler.BAN_MOVE.contains(uid)) return;
		val distance = e.getFrom().distanceSquared(e.getTo());
		if (distance < 0.05) return;
		TpHandler.BAN_MOVE.remove(uid);
		TPCANCEL_MOVE.send(e.getPlayer());
	}

	/** @deprecated BUKKIT */
	@Deprecated
	@Override
	public void onPluginMessageReceived(String c, Player player, byte[] message) {
		if (!ShareData.BC_CHANNEL.equals(c)) return;
		Channel type = Channel.byId(ShareData.readInt(message, 0, -1));
		if (!ALLOW_PLAYERS.contains(player.getUniqueId())) {
			if (type != Channel.VERSION_CHECK) return;
			boolean allow = Channel.VersionCheck.parseC(message);
			if (allow) ALLOW_PLAYERS.add(player.getUniqueId());
			else BAD_VERSION.send(player);
			Main.send(player, Channel.VersionCheck.sendC(allow));
			return;
		}
		if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] receive: " + type + ": " + Arrays.toString(message));
		switch (type) {
		case PERMISSION: {
			Channel.Permission.parseS(message, (permission, allow) -> //
			callBack(player, type, permission, handler -> {
				if (allow) ((Runnable) handler).run();
			}));
			break;
		}
		case TP: {
			TpHandler.handleTpMessage(player, type, message);
			break;
		}
		case VERSION_CHECK: {
			Main.send(player, Channel.VersionCheck.sendC(message));
			break;
		}
		case COOLDOWN: {
			Channel.Cooldown.parseS(message, COOLDOWN);
			break;
		}
		case TIME_AMEND: {
			Main.send(player, Channel.TimeAmend.sendS());
			break;
		}
		case SERVER_INFO: {
			val info = Channel.ServerInfo.parseS(message);
			TabHandler.TAB_REPLACE_ALL.put(player.getUniqueId(), info.getTabAll().intern());
			TabHandler.TAB_REPLACE_NOR.put(player.getUniqueId(), info.getTabNor().intern());
			val meVer = Main.getMain().getDescription().getVersion();
			if (!meVer.equals(info.getVersion())) VER_NO_RECOMMEND.send(player, meVer, info.getVersion());
			break;
		}

		}
	}

}
