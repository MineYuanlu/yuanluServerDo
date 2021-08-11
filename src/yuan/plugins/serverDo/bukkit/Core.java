/**
 * yuanlu
 * date: 2021年8月9日
 * file: Core.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class ConfDatas {
		String	noCooldownPermission;
		long	cooldown;
	}

	private static final ConfDatas Conf = new ConfDatas();

	/** 监听回调的对象 */
	@Value
	private static final class ListenCallBackObj {
		/** 检查数据 */
		Object	checker;
		/** 处理器 */
		Object	handler;
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
		private static final ConcurrentHashMap<String, String> WAIT_JOIN_TP = new ConcurrentHashMap<>();

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
					tpTypeReceiverMsg[tpType].send(player, name, display);
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
					if (mover != null) toToLocal(mover, player, false);
					else WAIT_JOIN_TP.put(name, player.getName());
				});
				break;
			case 0xa:
				handler = h -> Channel.Tp.paS_tpReqThirdReceive(buf, (BiPlayerConsumer) h);
				break;
			}
			if (handler != null) callBack(player, type, id, handler);
		}

		/**
		 * 本地传送
		 * 
		 * @param mover      移动者
		 * @param target     目标
		 * @param checkDelay 是否检查用户的延时传送
		 */
		private static void toToLocal(@NonNull Player mover, @NonNull Player target, boolean checkDelay) {
			if (checkDelay) {
				checkDelay(mover, Conf.cooldown, () -> toToLocal(mover, target, false));
				return;
			}
			mover.teleport(target);
		}

		/**
		 * 远程传送
		 * 
		 * @param player     操控玩家
		 * @param mover      移动者
		 * @param target     目标
		 * @param checkDelay 是否检查用户的延时传送
		 */
		private static void tpToRemote(@NonNull Player player, @NonNull String mover, @NonNull String target, boolean checkDelay) {
			if (checkDelay) {
				checkDelay(player, Conf.cooldown, () -> tpToRemote(player, mover, target, false));
				return;
			}
			listenCallBack(player, Channel.TP, 7, (BiBoolConsumer) (success, error) -> {
				if (error) BC_ERROR.send(player);
				else if (!success) BC_PLAYER_OFF.send(player);
			});
			Main.send(player, Channel.Tp.s6C_tpThird(mover, target));
		}

		/**
		 * 检查延时传送
		 * 
		 * @param player 玩家
		 * @param time   延时时长
		 * @param r      检查完成
		 */
		private static void checkDelay(@NonNull Player player, long time, Runnable r) {
			if (Conf.noCooldownPermission != null && player.hasPermission(Conf.noCooldownPermission)) r.run();
			else WaitMaintain.add(BAN_MOVE, player.getUniqueId(), time, r);
		}

		/** 禁止移动的玩家 */
		private static final HashSet<UUID> BAN_MOVE = new HashSet<>();
	}

	/** 单例 */
	public static final Core INSTANCE = new Core();
	/*
	 * tp,tpa,tpahere,tphere tpaccept,tpcancel
	 * 
	 * warp,home,spawn
	 * 
	 */

	/** 回调等待 */
	private static final EnumMap<Channel, Map<UUID, ListenCallBackObj>> CALL_BACK_WAITER = new EnumMap<>(Channel.class);
	static {
		for (val c : Channel.values()) CALL_BACK_WAITER.put(c, new HashMap<>());
	}

	/** 版本检查通过的玩家集合 */
	private static final HashSet<UUID> ALLOW_PLAYERS = new HashSet<>();

	/**
	 * 回调
	 * 
	 * @param player   玩家
	 * @param channel  通道
	 * @param checker  验证数据
	 * @param consumer 处理器接收
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void callBack(@NonNull Player player, @NonNull Channel channel, Object checker, Consumer consumer) {
		val map = CALL_BACK_WAITER.get(channel);
		synchronized (map) {
			val obj = map.remove(player.getUniqueId());
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[callback] obj:" + obj + ", checker: " + checker);
			if (Tool.equals(obj.checker, checker)) {
				if (ShareData.isDEBUG()) ShareData.getLogger().info("[callback] call:" + obj.handler + ", consumer: " + consumer);
				if (consumer != null) try {
					consumer.accept(obj.handler);
				} catch (Throwable e) {
					throw new RuntimeException("回调函数执行失败, 玩家: " + player.getName() + ", 频道: " + channel + ", 标记: " + checker);
				}
			} else map.put(player.getUniqueId(), obj);
		}
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
			WaitMaintain.put(map, player.getUniqueId(), //
					new ListenCallBackObj(checker, handler), maxTime, () -> M_TIME_OUT.send(player, channel));
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
	public static void permissionCheck(@NonNull CommandSender player, @NonNull String permission, @NonNull Runnable next) {
		if (player instanceof ConsoleCommandSender || player.hasPermission(permission)) {
			next.run();
		} else if (player instanceof Player) {
			Player p = ((Player) player);
			listenCallBack(p, Channel.PERMISSION, permission, next);
			Main.send(p, Channel.Permission.sendS(permission));
		}
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求
	 * 
	 * @param mover      移动者
	 * @param target     目标
	 * @param checkDelay 是否检查用户的延时传送
	 */
	public static void tpTo(Player mover, String target, boolean checkDelay) {
		val localTarget = Main.getMain().getServer().getPlayerExact(target);
		if (localTarget != null) TpHandler.toToLocal(mover, localTarget, checkDelay);
		else TpHandler.tpToRemote(mover, mover.getName(), target, checkDelay);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求
	 * 
	 * @param player     操控玩家
	 * @param mover      移动者
	 * @param target     目标
	 * @param checkDelay 是否检查用户的延时传送
	 */
	public static void tpTo(Player player, String mover, String target, boolean checkDelay) {
		val	localMover	= Main.getMain().getServer().getPlayerExact(mover);
		val	localTarget	= Main.getMain().getServer().getPlayerExact(target);
		if (localMover != null && localTarget != null) TpHandler.toToLocal(localMover, localTarget, checkDelay);
		else TpHandler.tpToRemote(player, mover, target, checkDelay);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求
	 * 
	 * @param mover      移动者
	 * @param target     目标
	 * @param checkDelay 是否检查用户的延时传送
	 */
	public static void tpTo(String mover, Player target, boolean checkDelay) {
		val localMover = Main.getMain().getServer().getPlayerExact(mover);
		if (localMover != null) TpHandler.toToLocal(localMover, target, checkDelay);
		else TpHandler.tpToRemote(target, mover, target.getName(), checkDelay);
	}

	/**
	 * 
	 * @param e 事件
	 * @deprecated BUKKIT
	 */
	@Deprecated
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerMove(@NonNull PlayerMoveEvent e) {
		val uid = e.getPlayer().getUniqueId();
		if (!TpHandler.BAN_MOVE.contains(uid)) return;
		val distance = e.getFrom().distanceSquared(e.getTo());
		if (distance < 0.05) return;
		TpHandler.BAN_MOVE.remove(uid);
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
		Bukkit.getScheduler().runTask(Main.getMain(), () -> tpTo(p, to, false));
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
		default:
			break;

		}
	}
}
