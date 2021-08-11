/**
 * yuanlu
 * date: 2021年8月11日
 * file: CmdTpaccept.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.NonNull;
import lombok.Value;
import lombok.val;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.WaitMaintain;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * Tpaccept命令
 * 
 * @author yuanlu
 *
 */
public final class CmdTpaccept extends Cmd {
	/**
	 * 传送请求等待信息
	 * 
	 * @author yuanlu
	 *
	 */
	@Value
	private static final class TpWaitInfo {
		/** 发起请求的玩家全名 */
		@NonNull String	sender;
		/** 发起请求的玩家展示名 */
		@NonNull String	display;
		/** 是否是需要传送到对方位置 */
		boolean			isThere;

		@Override
		public String toString() {
			return sender;
		}
	}

	/** 传送等待 */
	private static final Map<UUID, ArrayList<TpWaitInfo>>	TP_WAIT			= new ConcurrentHashMap<>();

	/** 传送等待超时的玩家 */
	private static final HashSet<UUID>						TP_WAIT_TIMEOUT	= new HashSet<>();

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
			if (player.isOnline()) TP_WAIT_TIMEOUT.add(uid);
		});
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
				Core.listenCallBack(player, Channel.TP, 4, (BoolConsumer) success -> {
					if (success) {
						if (accept && tar.isThere()) Core.tpTo(player, tar.getSender());
					} else cmd.msg("offline", player);
				});
				cmd.msg("success", player, tar.getSender(), tar.getDisplay());
				Main.send(player, Channel.Tp.s3C_tpResp(tar.getSender(), accept));
			}
		} else if (TP_WAIT_TIMEOUT.remove(player.getUniqueId())) cmd.msg("timeout", player);
		else cmd.msg("no-request", player);
	}

	/** @param name 命令名 */
	protected CmdTpaccept(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		handleRequest((Player) sender, args.length > 0 ? args[0] : null, true, this);
		return true;
	}

}
