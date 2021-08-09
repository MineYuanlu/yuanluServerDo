/**
 * yuanlu
 * date: 2021年8月9日
 * file: Core.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.WaitMaintain;

/**
 * Bukkit端的核心组件
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Core implements PluginMessageListener, MESSAGE {
	/** 单例 */
	public static final Core INSTANCE = new Core();
	/*
	 * tp,tpa,tpahere,tphere tpaccept,tpcancel
	 * 
	 * warp,home,spawn
	 * 
	 */

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
	 * 监听回调
	 * 
	 * @param player  玩家
	 * @param channel 通道
	 * @param checker 验证数据
	 * @param handler 处理器
	 */
	private static void listenCallBack(@NonNull Player player, @NonNull Channel channel, Object checker, @NonNull Object handler) {
		WaitMaintain.put(CALL_BACK_WAITER.get(channel), //
				player.getUniqueId(), new ListenCallBackObj(checker, handler), () -> M_TIME_OUT.send(player));
	}

	/** 回调等待 */
	private static final EnumMap<Channel, HashMap<UUID, ListenCallBackObj>> CALL_BACK_WAITER = new EnumMap<>(Channel.class);
	static {
		for (val c : Channel.values()) CALL_BACK_WAITER.put(c, new HashMap<>());
	}

	/** 监听回调的对象 */
	@Value
	private static final class ListenCallBackObj {
		/** 检查数据 */
		Object	checker;
		/** 处理器 */
		Object	handler;
	}

	/** 回调队列 */
	public static final class CallbackQueue implements Runnable {
		/** 队列 */
		Runnable[]	runnables;
		/** 队列指针 */
		int			index;

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

		@Override
		public synchronized void run() {
			runnables[index++].run();
			if (index == runnables.length) runnables = null;
		}

	}

	/** 版本检查通过的玩家集合 */
	private static final HashSet<UUID> ALLOW_PLAYERS = new HashSet<>();

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
		}
	}
}
