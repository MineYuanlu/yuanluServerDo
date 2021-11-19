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
import java.util.Collection;
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
import java.util.function.IntConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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
import yuan.plugins.serverDo.ShareData.TabType;
import yuan.plugins.serverDo.ShareLocation;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.WaitMaintain;
import yuan.plugins.serverDo.bukkit.cmds.CmdTpaccept;
import yuan.plugins.serverDo.bukkit.cmds.CmdVanish;

/**
 * Bukkit端的核心组件
 *
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Core implements PluginMessageListener, MESSAGE, Listener {
	/**
	 * Back处理器
	 *
	 * @author yuanlu
	 */
	public static final class BackHandler {
		/**
		 * 上一个坐标的保存<br>
		 * 当loc.server为null代表本地, 否则代表其它服务器<br>
		 * 当玩家退出当前服务器时删除(待考虑, 例如临时退出 TODO )
		 */
		private static final ConcurrentHashMap<String, ShareLocation> BACKS = new ConcurrentHashMap<>();
		static {
			registerClearListener(c -> BACKS.remove(c.getName()));
		}

		/**
		 * 获取Back坐标
		 *
		 * @param player 玩家
		 * @return 玩家的back坐标/null
		 */
		public static final ShareLocation getBack(@NonNull Player player) {
			return BACKS.get(player.getName());
		}

		/**
		 * 处理Back数据包
		 *
		 * @param player 玩家
		 * @param type   通道类型(必须为BACK)
		 * @param buf    数据包
		 */
		private static final void handleBackMessage(Player player, Channel type, byte[] buf) {
			if (type != Channel.BACK) throw new InternalError("Bad Type");
			byte id = Channel.getSubId(buf);
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] BACK: " + id);
			switch (id) {
			case 1:
				Channel.Back.p1S_tellTp(buf, BACKS::put);
				break;
			default:
				ShareData.getLogger().warning("[PACKAGE]Bad Back sub id:" + id + ", from: " + player.getName());
				break;
			}
		}

		/**
		 * 记录当前坐标
		 *
		 * @param player 玩家
		 * @param loc    本地坐标
		 */
		private static final void recordLocation(@NonNull Player player, @NonNull Location loc) {
			BACKS.put(player.getName(), toSLoc(loc));
		}

		/**
		 * 记录当前坐标
		 *
		 * @param player   玩家
		 * @param toServer 玩家即将传送的服务器(null为本地)
		 */
		private static final void recordLocation(@NonNull Player player, String toServer) {
			recordLocation(player, toServer, null);
		}

		/**
		 * 记录当前坐标<br>
		 * 注: toServer 与 toPlayer 均为null时为本地
		 *
		 * @param player   玩家
		 * @param toServer 玩家即将传送的服务器
		 * @param toPlayer 玩家即将传送的玩家
		 */
		private static final void recordLocation(@NonNull Player player, String toServer, String toPlayer) {
			val	name	= player.getName();
			val	loc		= toSLoc(player.getLocation());
			if (toServer == null && toPlayer == null) {
				BACKS.put(name, loc);
			} else {
				Main.send(player, Channel.Back.s0C_tellTp(name, loc, toServer != null, toServer != null ? toServer : toPlayer));
			}
		}
	}

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
		String	noDelayPermission			= null;
		/** 传送延时(秒) */
		long	delay						= 3;
		/** 无冷却的权限 */
		String	noCooldownPermission		= null;
		/** 冷却(秒) */
		long	cooldown					= 100;
		/** 超时时长(秒) */
		long	overtime					= 120;
		/** 安全传送 */
		boolean	safeLocation				= false;
		/** tp事件记录back */
		boolean	useTpEvent					= false;
		/** tp事件入服休眠 */
		long	tpEventJoinSleep			= 3000;
		/** tp事件最小记录距离 */
		double	tpEventMinDistance			= 5;
		/** tp事件最小记录距离平方 */
		double	tpEventMinDistanceSquare	= tpEventMinDistance * tpEventMinDistance;
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

	/**
	 * 权限管理器
	 *
	 * @author yuanlu
	 *
	 */
	public static final class Permissions {
		@SuppressWarnings("javadoc")
		@Value
		public static final class PerAmount {
			PerAmountNode[]	nodes;
			int				def;

			/**
			 * 获取数量
			 *
			 * @param sender 检测对象
			 * @return 最大数量
			 */
			public int getMaxAmount(CommandSender sender) {
				for (PerAmountNode node : nodes) if (sender.hasPermission(node.permission)) return node.amount;
				return def;
			}
		}

		@SuppressWarnings("javadoc")
		@Value
		public static final class PerAmountNode implements Comparable<PerAmountNode> {
			String	permission;
			int		amount;

			@Override
			public int compareTo(PerAmountNode o) {
				return Integer.compare(amount, o.amount);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) return true;
				if ((obj == null) || (getClass() != obj.getClass())) return false;
				PerAmountNode other = (PerAmountNode) obj;
				if (permission == null) {
					if (other.permission != null) return false;
				} else if (!permission.equals(other.permission)) return false;
				return true;
			}

			@Override
			public int hashCode() {
				final int	prime	= 31;
				int			result	= 1;
				result = prime * result + ((permission == null) ? 0 : permission.hashCode());
				return result;
			}

		}

		/** 所有权限 */
		private static final HashMap<String, String>	permissions	= new HashMap<>();

		/** 所有数量权限 */
		private static final HashMap<String, PerAmount>	perAmounts	= new HashMap<>();

		/**
		 * 获取数量
		 *
		 * @param sender 检测对象
		 * @param node   权限节点
		 * @return 最大数量/null(无任何权限)
		 */
		public static Integer getMaxAmount(CommandSender sender, String node) {
			val ps = perAmounts.get(node);
			return ps == null ? null : ps.getMaxAmount(sender);
		}

		/**
		 * 检测对象是否有权限
		 *
		 * @param sender 检测对象
		 * @param node   权限节点
		 * @param silent 静默检查
		 * @return 是否有权限
		 */
		public static boolean hasPermission(CommandSender sender, String node, boolean silent) {
			if (sender.isOp()) return true;
			val	p	= Permissions.permissions.get(node);
			val	has	= p != null && (p.isEmpty() || sender.hasPermission(p));
			if (!has && !silent) {
				NO_PERMISSION.send(sender, p == null ? "§4OP§r" : p);
			}
			return has;

		}

		/**
		 * 初始化权限
		 *
		 * @param conf 权限节点
		 */
		private static void init(ConfigurationSection conf) {
			if (conf == null) return;
			for (val k : conf.getKeys(false)) {
				if (conf.isString(k)) {
					val p = conf.getString(k, null);
					if (p != null) permissions.put(k, p);
				} else if (conf.isConfigurationSection(k)) {
					val							ps		= conf.getConfigurationSection(k);
					val							psKeys	= ps.getKeys(false);
					int							def		= 0;
					ArrayList<PerAmountNode>	list	= new ArrayList<>(psKeys.size());
					for (val p : psKeys) {
						if (p.equalsIgnoreCase("default") || p.equalsIgnoreCase("def")) def = ps.getInt(p, def);
						else try {
							val amount = Integer.parseInt(p);
							if (ps.isString(p)) list.add(new PerAmountNode(ps.getString(p), amount));
							else if (ps.isList(p)) for (val permission : ps.getStringList(p)) list.add(new PerAmountNode(permission, amount));
							else ShareData.getLogger()
									.warning("[CONF] Unsupported permission section, need a string or string-list: " + ps.getCurrentPath() + "." + p);
						} catch (NullPointerException | NumberFormatException e) {
							ShareData.getLogger()
									.warning("[CONF] Unsupported permission node, need a number or 'def'/'default': " + ps.getCurrentPath() + "." + p);
						}
					}
					list.sort(Collections.reverseOrder());
					perAmounts.put(k, new PerAmount(list.toArray(new PerAmountNode[list.size()]), def));
				}
			}
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CONF] permissions: " + permissions);
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CONF] perAmounts: " + perAmounts);
		}

		/**
		 * 检测对象是否有权限<br>
		 * <b>非静默</b>
		 *
		 * @param sender 检测对象
		 * @return 是否有权限
		 */
		public static boolean tpOther_(CommandSender sender) {
			return Permissions.hasPermission(sender, "tp-other", false);
		}

		/**
		 * 检测对象是否有权限
		 *
		 * @param sender 检测对象
		 * @return 是否有权限
		 */
		public static boolean tpSenior(CommandSender sender) {
			return Permissions.hasPermission(sender, "tp-senior", true);
		}
	}

	/** tab处理器 */
	public static final class TabHandler {
		/** tab替换内容 */
		private static final HashMap<UUID, String> TAB_REPLACE = new HashMap<>();

		/**
		 * 获取tab内容
		 *
		 * @param p    玩家
		 * @param arg  实际参数
		 * @param type 类型
		 * @return 转换结果
		 */
		public static List<String> getTab(Player p, String arg, TabType type) {
			val tab = TAB_REPLACE.get(p.getUniqueId());
			if (tab == null) return Collections.singletonList(arg);
			return Collections.singletonList(tab + type + arg);
		}

		/**
		 * 获取补全列表
		 *
		 * @param player 玩家
		 * @param arg    输入
		 * @param all    全部列表
		 * @return tabReplace 补全列表(由BC解析)
		 */
		public static List<String> getTabReplaceTp(Player player, String arg, boolean all) {
			return getTab(player, arg, all ? TabType.TP_ALL : TabType.TP_NORMAL);
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
		private static final ConcurrentHashMap<String, String>		WAIT_JOIN_TP		= new ConcurrentHashMap<>();

		/**
		 * 等待加入服务器后传送<br>
		 * 被传送者{@code ->}传送坐标
		 */
		private static final ConcurrentHashMap<String, Location>	WAIT_JOIN_TP_LOC	= new ConcurrentHashMap<>();

		/** 禁止移动的玩家 */
		private static final HashSet<UUID>							BAN_MOVE			= new HashSet<>();

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
		 * 处理TpLoc数据包
		 *
		 * @param player 玩家
		 * @param type   通道类型(必须为TP_LOC)
		 * @param buf    数据包
		 */
		@SuppressWarnings("rawtypes")
		private static void handleTpLocMessage(Player player, Channel type, byte[] buf) {
			if (type != Channel.TP_LOC) throw new InternalError("Bad Type");
			byte		id		= Channel.getSubId(buf);
			Consumer	handler	= null;
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] TP_LOC: " + id);
			switch (id) {
			case 0:
				handler = h -> Channel.TpLoc.p0S_tpLocResp(buf, (BoolConsumer) h);
				break;
			case 1:
				Channel.TpLoc.p1S_tpLoc(buf, (loc, name) -> {
					val mover = Bukkit.getPlayerExact(name);
					if (mover != null) toToLocal(mover, toBLoc(loc), true);
					else WAIT_JOIN_TP_LOC.put(name, toBLoc(loc));
				});
				break;
			default:
				ShareData.getLogger().warning("[PACKAGE]Bad TpLoc sub id:" + id + ", from: " + player.getName());
				break;
			}
			if (handler != null) callBack(player, type, id, handler);
		}

		/**
		 * 处理Tp数据包
		 *
		 * @param player 玩家
		 * @param type   通道类型(必须为TP)
		 * @param buf    数据包
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void handleTpMessage(Player player, Channel type, byte[] buf) {
			if (type != Channel.TP) throw new InternalError("Bad Type");
			byte		id		= Channel.getSubId(buf);
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
					if (mover != null) toToLocal(mover, player, -1, false, true);
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
		 * @param mover    移动者
		 * @param target   目标
		 * @param noRecord 是否不记录位置(用于back)
		 */
		private static void toToLocal(@NonNull Player mover, @NonNull Location target, boolean noRecord) {
			if (!noRecord) BackHandler.recordLocation(mover, (String) null);
			Bukkit.getScheduler().runTask(Main.getMain(), () -> mover.teleport(target, PlayerTeleportEvent.TeleportCause.COMMAND));
		}

		/**
		 * 本地传送
		 *
		 * @param mover        移动者
		 * @param target       目标
		 * @param waitTime     传送等待时间
		 * @param needCooldown 是否需要检查冷却
		 * @param noRecord     是否不记录位置(用于back)
		 */
		private static void toToLocal(@NonNull Player mover, @NonNull Player target, long waitTime, boolean needCooldown, boolean noRecord) {
			if (waitTime > 0) {
				checkDelay(mover, Conf.delay, () -> toToLocal(mover, target, -1, needCooldown, noRecord));
				return;
			}
			if (!noRecord) BackHandler.recordLocation(mover, (String) null);
			Bukkit.getScheduler().runTask(Main.getMain(), () -> mover.teleport(target, PlayerTeleportEvent.TeleportCause.COMMAND));
			if (needCooldown) checkCooldown(mover);
		}

		/**
		 * 远程传送
		 *
		 * @param player   玩家
		 * @param loc      地址
		 * @param noRecord 是否不记录位置(用于back)
		 */
		public static void tpToRemote(@NonNull Player player, @NonNull ShareLocation loc, boolean noRecord) {
			if (loc.getServer() == null) throw new IllegalArgumentException("No server specified: " + loc);
			if (!noRecord) BackHandler.recordLocation(player, loc.getServer());
			listenCallBack(player, Channel.TP_LOC, 0, (BoolConsumer) success -> {
				if (!success) BC_ERROR.send(player);
			});
			Channel.TpLoc.s0C_tpLoc(loc, loc.getServer());
		}

		/**
		 * 远程传送
		 *
		 * @param player       操控玩家
		 * @param mover        移动者
		 * @param target       目标
		 * @param waitTime     传送等待时间
		 * @param needCooldown 是否需要检查冷却
		 * @param noRecord     是否不记录位置(用于back)
		 */
		private static void tpToRemote(@NonNull Player player, @NonNull String mover, @NonNull String target, long waitTime, boolean needCooldown,
				boolean noRecord) {
			if (ShareData.isDEBUG())
				ShareData.getLogger().info(String.format("[tpTo] remote: %s->%s, wait: %s, cd: %s", mover, target, waitTime, needCooldown));
			if (waitTime > 0) {
				checkDelay(player, Conf.delay, () -> tpToRemote(player, mover, target, -1, needCooldown, noRecord));
				return;
			}
			if (!noRecord) BackHandler.recordLocation(player, null, target);
			listenCallBack(player, Channel.TP, 7, (BiBoolConsumer) (success, error) -> {
				if (error) BC_ERROR.send(player);
				else if (!success) BC_PLAYER_OFF.send(player);
			});
			Main.send(player, Channel.Tp.s6C_tpThird(mover, target));
			if (needCooldown) checkCooldown(player);
		}
	}

	/**
	 * 地标(含Home)处理器
	 *
	 * @author yuanlu
	 *
	 */
	public static final class WarpHandler {
		/**
		 * 处理Home数据包
		 *
		 * @param player 玩家
		 * @param type   通道类型(必须为HOME)
		 * @param buf    数据包
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void handleHomeMessage(Player player, Channel type, byte[] buf) {
			if (type != Channel.HOME) throw new InternalError("Bad Type");
			byte		id		= Channel.getSubId(buf);
			Consumer	handler	= null;
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] HOME: " + id);
			switch (Channel.getSubId(buf)) {
			case 0:
				handler = h -> Channel.Home.p0S_setHomeResp(buf, (BoolConsumer) h);
				break;
			case 1:
				handler = h -> Channel.Home.p1S_delHomeResp(buf, (BoolConsumer) h);
				break;
			case 2:
				handler = h -> Channel.Home.p2S_searchHomeResp(buf, (BiConsumer<String, String>) h);
				break;
			case 3:
				handler = h -> Channel.Home.p3S_tpHomeResp(buf, (BoolConsumer) h);
				break;
			case 4:
				handler = h -> Channel.Home.p4S_listHomeResp(buf, (BiConsumer<Collection<String>, Collection<String>>) h);
				break;
			default:
				ShareData.getLogger().warning("[PACKAGE]Bad Home sub id:" + id + ", from: " + player.getName());
				break;
			}
			if (handler != null) callBack(player, type, id, handler);
		}

		/**
		 * 处理Warp数据包
		 *
		 * @param player 玩家
		 * @param type   通道类型(必须为WARP)
		 * @param buf    数据包
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void handleWarpMessage(Player player, Channel type, byte[] buf) {
			if (type != Channel.WARP) throw new InternalError("Bad Type");
			byte		id		= Channel.getSubId(buf);
			Consumer	handler	= null;
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[CHANNEL] WARP: " + id);
			switch (Channel.getSubId(buf)) {
			case 0:
				handler = h -> Channel.Warp.p0S_setWarpResp(buf, (Runnable) h);
				break;
			case 1:
				handler = h -> Channel.Warp.p1S_delWarpResp(buf, (BoolConsumer) h);
				break;
			case 2:
				handler = h -> Channel.Warp.p2S_searchWarpResp(buf, (BiConsumer<String, String>) h);
				break;
			case 3:
				handler = h -> Channel.Warp.p3S_tpWarpResp(buf, (BoolConsumer) h);
				break;
			case 4:
				handler = h -> Channel.Warp.p4S_listWarpResp(buf, (BiConsumer<Collection<String>, Collection<String>>) h);
				break;
			default:
				ShareData.getLogger().warning("[PACKAGE]Bad Warp sub id:" + id + ", from: " + player.getName());
				break;
			}
			if (handler != null) callBack(player, type, id, handler);
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
	private static final HashSet<UUID>					ALLOW_PLAYERS	= new HashSet<>();

	/** 传送冷却 */
	private static final HashMap<UUID, Long>			COOLDOWN		= new HashMap<>();

	/** 清理监听器 */
	private static final ArrayList<Consumer<Player>>	CLEAR_LISTENER	= new ArrayList<>();

	static {
		registerClearListener(p -> {
			val u = p.getUniqueId();
			TabHandler.TAB_REPLACE.remove(u);
			TpHandler.BAN_MOVE.remove(u);
			CALL_BACK_WAITER.values().forEach(m -> m.remove(u));
			ALLOW_PLAYERS.remove(u);
		});
	}
	/** 当启用tp event时，玩家加入服务器后禁用记录数秒 */
	private static final HashSet<UUID> EVENT_JOIN_SLEEP = new HashSet<>(Conf.getTpEventJoinSleep() > 0 ? 8 : 0);

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
			if (objs != null) for (Iterator<ListenCallBackObj> itr = objs.iterator(); itr.hasNext();) {
				val e = itr.next();
				if (Tool.equals(e.getChecker(), checker)) {
					itr.remove();
					if (ShareData.isDEBUG())
						ShareData.getLogger().info("[callback] channel: " + channel + ", call:" + e.getChecker() + ", consumer: " + consumer);
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

	/** @param player 唤起清理 */
	public static void callClear(Player player) {
		synchronized (CLEAR_LISTENER) {
			for (val consumer : CLEAR_LISTENER) {
				consumer.accept(player);
			}
		}
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
		Conf.useTpEvent				= conf.getBoolean("setting.back.use-tp-event", Conf.useTpEvent);
		if (Conf.isUseTpEvent()) {
			Conf.tpEventJoinSleep			= conf.getLong("setting.back.event-after-join", Conf.tpEventJoinSleep);
			Conf.tpEventMinDistance			= conf.getDouble("setting.back.event-ignore-distance", Conf.tpEventMinDistance);
			Conf.tpEventMinDistanceSquare	= Conf.tpEventMinDistance * Conf.tpEventMinDistance;
		} else {
			Conf.tpEventJoinSleep	= -1;
			Conf.tpEventMinDistance	= Conf.tpEventMinDistanceSquare = Double.MAX_VALUE;
		}

		WaitMaintain.setT_User(Conf.getOvertime() * 1000);

		// init Permission
		Permissions.init(conf.getConfigurationSection("setting.permission"));
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

	/** @param c 玩家下线时的清理监听器 */
	static void registerClearListener(Consumer<Player> c) {
		synchronized (CLEAR_LISTENER) {
			CLEAR_LISTENER.add(c);
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
	 * 转换为Bukkit坐标
	 *
	 * @param loc Share坐标
	 * @return Bukkit坐标
	 */
	public static @NonNull Location toBLoc(ShareLocation loc) {
		return new Location(Bukkit.getWorld(loc.getWorld()), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
	}

	/**
	 * 转换为Share坐标
	 *
	 * @param loc Bukkit坐标
	 * @return Share坐标
	 */
	public static @NonNull ShareLocation toSLoc(Location loc) {
		return new ShareLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), loc.getWorld().getName());
	}

	/**
	 * 传送请求码转换<br>
	 * 检测玩家是否拥有高级权限
	 *
	 * @param player   玩家
	 * @param realCode 真实的请求码
	 * @return 转换后的请求码
	 * @see yuan.plugins.serverDo.Channel.Tp#s0C_tpReq(String, int)
	 */
	public static int tpReqCode(Player player, int realCode) {
		if (realCode < 0) return realCode;
		return Permissions.tpSenior(player) ? -realCode : realCode;
	}

	/**
	 * 传送玩家到指定地点<br>
	 * 当loc的server未指定时, 代表本地传送
	 *
	 * @param player 玩家
	 * @param loc    指定坐标
	 */
	public static void tpTo(@NonNull Player player, @NonNull ShareLocation loc) {
		if (loc.getServer() == null) TpHandler.toToLocal(player, toBLoc(loc), false);
		else TpHandler.tpToRemote(player, loc, false);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求 <br>
	 * 适合一般传送
	 *
	 * @param mover        移动者
	 * @param target       目标
	 * @param waitTime     传送等待时间
	 * @param needCooldown 是否需要检查冷却
	 */
	public static void tpTo(Player mover, String target, long waitTime, boolean needCooldown) {
		tpTo(mover, target, waitTime, needCooldown, false);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求<br>
	 * 适合目标服务器加入传送
	 *
	 * @param mover        移动者
	 * @param target       目标
	 * @param waitTime     传送等待时间
	 * @param needCooldown 是否需要检查冷却
	 * @param noRecord     是否不记录位置(用于back)
	 */
	public static void tpTo(Player mover, String target, long waitTime, boolean needCooldown, boolean noRecord) {
		val localTarget = Main.getMain().getServer().getPlayerExact(target);
		if (localTarget != null) TpHandler.toToLocal(mover, localTarget, waitTime, needCooldown, noRecord);
		else TpHandler.tpToRemote(mover, mover.getName(), target, waitTime, needCooldown, noRecord);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求<br>
	 * 适合第三方传送
	 *
	 * @param player   操控玩家
	 * @param mover    移动者
	 * @param target   目标
	 * @param waitTime 传送等待时间
	 */
	public static void tpTo(Player player, String mover, String target, long waitTime) {
		val	localMover	= Main.getMain().getServer().getPlayerExact(mover);
		val	localTarget	= Main.getMain().getServer().getPlayerExact(target);
		if (localMover != null && localTarget != null) TpHandler.toToLocal(localMover, localTarget, waitTime, false, false);
		else TpHandler.tpToRemote(player, mover, target, waitTime, false, false);
	}

	/**
	 * 传送玩家<br>
	 * 将会先检查本地玩家, 若不存在则向BC请求<br>
	 * 适合tphere
	 *
	 * @param mover    移动者
	 * @param target   目标
	 * @param waitTime 传送等待时间
	 */
	public static void tpTo(String mover, Player target, long waitTime) {
		val localMover = Main.getMain().getServer().getPlayerExact(mover);
		if (localMover != null) TpHandler.toToLocal(localMover, target, waitTime, false, false);
		else TpHandler.tpToRemote(target, mover, target.getName(), waitTime, false, false);
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

		if (Conf.getTpEventJoinSleep() > 0) {
			WaitMaintain.add(EVENT_JOIN_SLEEP, p.getUniqueId(), Conf.getTpEventJoinSleep());
		}

		val to = TpHandler.WAIT_JOIN_TP.remove(p.getName());
		if (to != null) {
			tpTo(p, to, -1, false, true);
			return;
		}

		val toLoc = TpHandler.WAIT_JOIN_TP_LOC.remove(p.getName());
		if (toLoc != null) TpHandler.toToLocal(p, toLoc, true);
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

	/**
	 *
	 * @param e 事件
	 * @deprecated BUKKIT
	 */
	@Deprecated
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerQuit(@NonNull PlayerQuitEvent e) {
		callClear(e.getPlayer());
	}

	/**
	 *
	 * @param e 事件
	 * @deprecated BUKKIT
	 */
	@Deprecated
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerTp(@NonNull PlayerTeleportEvent e) {
		if (!Conf.isUseTpEvent()) return;
		val player = e.getPlayer();
		if ((player == null) || EVENT_JOIN_SLEEP.contains(player.getUniqueId())) return;
		val	f	= e.getFrom();
		val	t	= e.getTo();
		if (f == null || t == null || f.distanceSquared(t) < Conf.getTpEventMinDistanceSquare()) return;
		BackHandler.recordLocation(player, f);
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
				else NO_PERMISSION.send(player, permission);
			}));
			break;
		}
		case TP: {
			TpHandler.handleTpMessage(player, type, message);
			break;
		}
		case TP_LOC: {
			TpHandler.handleTpLocMessage(player, type, message);
			break;
		}
		case WARP: {
			WarpHandler.handleWarpMessage(player, type, message);
			break;
		}
		case HOME: {
			WarpHandler.handleHomeMessage(player, type, message);
			break;
		}
		case BACK: {
			BackHandler.handleBackMessage(player, type, message);
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
			TabHandler.TAB_REPLACE.put(player.getUniqueId(), info.getTab().intern());
			val meVer = Main.getMain().getDescription().getVersion();
			if (!meVer.equals(info.getVersion())) VER_NO_RECOMMEND.send(player, meVer, info.getVersion());
			break;
		}
		case VANISH: {
			val isHide = Channel.Vanish.parse(message);
			if (!callBack(player, type, null, h -> ((BoolConsumer) h).accept(isHide)) && isHide) CmdVanish.callback(player);
			break;
		}
		case TRANS_HOME: {
			callBack(player, type, null, h -> Channel.TransHome.parseC(message, (IntConsumer) h));
			break;
		}
		case TRANS_WARP: {
			callBack(player, type, null, h -> Channel.TransWarp.parseC(message, (IntConsumer) h));
			break;
		}
		}
	}
}
