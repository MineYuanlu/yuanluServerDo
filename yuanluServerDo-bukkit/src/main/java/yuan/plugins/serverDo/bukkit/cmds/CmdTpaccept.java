/**
 * yuanlu
 * date: 2021年8月11日
 * file: CmdTpaccept.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.WaitMaintain;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * tpaccept命令
 *
 * @author yuanlu
 */
public final class CmdTpaccept extends Cmd {
	/** 传送等待 */
	private static final Map<UUID, ArrayList<TpWaitInfo>> TP_WAIT         = new ConcurrentHashMap<>();
	/** 传送等待超时的玩家 */
	private static final HashMap<UUID, Long>              TP_WAIT_TIMEOUT = new HashMap<>();
	/** message */
	private static final Msg                              M_R_F           = msg(CmdTpcancel.class, "remote-fail");
	/** message */
	private static final Msg                              M_R_S           = msg(CmdTpcancel.class, "remote-success");

	/** @param name 命令名 */
	protected CmdTpaccept(String name) {
		super(name);
	}

	/**
	 * 增加传送请求
	 *
	 * @param player  指向玩家
	 * @param sender  发起者
	 * @param display 发起者展示名
	 * @param isThere 是否传送到对方位置
	 */
	public static void addTpReq(@NonNull Player player, @NonNull String sender, String display, boolean isThere) {
		val uid = player.getUniqueId();
		TP_WAIT_TIMEOUT.remove(uid);
		WaitMaintain.add(TP_WAIT, uid, new TpWaitInfo(sender, display, isThere), WaitMaintain.T_User, ArrayList::new, () -> {
			if (player.isOnline()) TP_WAIT_TIMEOUT.put(uid, System.currentTimeMillis());
		});
	}

	/**
	 * 取消传送请求
	 *
	 * @param player 指向玩家
	 * @param sender 发起者
	 *
	 * @return 是否成功取消
	 */
	public static boolean cancelReq(@NonNull Player player, @NonNull String sender) {
		val list = TP_WAIT.get(player.getUniqueId());
		if (list != null) {
			for (Iterator<TpWaitInfo> itr = list.iterator(); itr.hasNext(); ) {
				TpWaitInfo info = itr.next();
				if (sender.equals(info.getSender())) {
					M_R_S.send(player, sender, info.display);
					itr.remove();
					return true;
				}
			}
		}
		M_R_F.send(player, sender);
		return false;
	}

	/**
	 * 获取玩家请求列表
	 *
	 * @param sender 目标
	 *
	 * @return 对此目标请求的玩家列表
	 */
	static List<String> getReqList(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			val list = TP_WAIT.get(player.getUniqueId());
			if (list != null && !list.isEmpty()) return Tool.translate(list, i -> i.sender);
		}
		return null;
	}

	/**
	 * 处理请求
	 *
	 * @param player 指向玩家
	 * @param who    请求发起者
	 * @param accept 是否是接受请求
	 * @param cmd    执行命令
	 */
	static void handleRequest(@NonNull Player player, String who, boolean accept, @NonNull Cmd cmd) {
		val list = TP_WAIT.get(player.getUniqueId());
		if (list != null && !list.isEmpty()) {
			final TpWaitInfo tar;
			if (who != null) list.remove(tar = Tool.search(who, list.iterator()));
			else tar = list.remove(list.size() - 1);

			if (tar == null) cmd.msg("not-found", player, who);
			else {
				val wait = Core.getWaitTime(player);
				if (tar.isThere() && accept && wait < 0) {
					msg(CmdTpahere.class, "cooldown").send(player, -wait / 1000.0);
					return;
				}
				Core.listenCallBack(player, Channel.TP, 4, (BoolConsumer) success -> {
					if (success) {
						cmd.msg(accept && tar.isThere() && wait > 0 ? "success-wait" : "success", player, tar.getSender(), tar.getDisplay(), wait);
						if (accept && tar.isThere()) Core.tpTo(player, tar.getSender(), wait, true);
					} else cmd.msg("offline", player);
				});
				Main.send(player, Channel.Tp.s3C_tpResp(tar.getSender(), accept));
			}
		} else {
			val timeout = TP_WAIT_TIMEOUT.remove(player.getUniqueId());
			if (timeout != null) cmd.msg("timeout", player, (System.currentTimeMillis() - timeout) / 1000.0);
			else cmd.msg("no-request", player);
		}
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		handleRequest((Player) sender, args.length > 0 ? args[0] : null, true, this);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return getReqList(sender);
	}

	/**
	 * 传送请求等待信息
	 *
	 * @author yuanlu
	 */
	@Value
	@AllArgsConstructor
	private static final class TpWaitInfo {
		/** 发起请求的玩家全名 */
		@NonNull
		String sender;
		/** 发起请求的玩家展示名 */
		@NonNull
		String display;
		/** 是否是需要传送到对方位置 */
		boolean isThere;

		@Override
		public int hashCode() {
			return sender.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TpWaitInfo) return sender.equals(((TpWaitInfo) obj).sender);
			return sender.equals(obj);
		}

		@Override
		public String toString() {
			return sender;
		}
	}

}
