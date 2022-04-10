/**
 * yuanlu
 * date: 2021年8月10日
 * file: CmdTpa.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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
 * tpa命令
 *
 * @author yuanlu
 *
 */
public final class CmdTpa extends TabTp {
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

		@Override
		public String toString() {
			return sender;
		}
	}

	/** 传送等待 */
	private static final Map<UUID, ArrayList<TpWaitInfo>> TP_WAIT = new ConcurrentHashMap<>();

	/**
	 * 增加传送请求(本地)
	 *
	 * @param player  发起玩家
	 * @param target  指向者
	 * @param display 指向者展示名
	 */
	static void addTpReq(@NonNull Player player, @NonNull String target, String display) {
		WaitMaintain.add(TP_WAIT, player.getUniqueId(), new TpWaitInfo(target, display), WaitMaintain.T_User, ArrayList::new, null);
	}

	/**
	 * 获取玩家请求列表
	 *
	 * @param sender 目标
	 * @return 对此目标请求的玩家列表
	 */
	static List<String> getReqList(CommandSender sender) {
		if (sender instanceof Player) {
			Player	player	= (Player) sender;
			val		list	= TP_WAIT.get(player.getUniqueId());
			if (list != null && !list.isEmpty()) return Tool.translate(list, i -> i.sender);
		}
		return null;
	}

	/**
	 * 移除请求
	 *
	 * @param player         发起玩家
	 * @param target         指向者
	 * @param nameAndDisplay 回调函数
	 */
	static void removeTpReq(@NonNull Player player, String target, @NonNull BiConsumer<String, String> nameAndDisplay) {
		val list = TP_WAIT.get(player.getUniqueId());
		if (list == null || list.isEmpty()) nameAndDisplay.accept(null, null);
		else {
			final TpWaitInfo tar;
			if (target != null) list.remove(tar = Tool.search(target, list.iterator()));
			else tar = list.remove(list.size() - 1);
			if (tar == null) nameAndDisplay.accept("", "");
			else nameAndDisplay.accept(tar.getSender(), tar.getDisplay());
		}
	}

	/** @param name 命令名 */
	protected CmdTpa(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (args.length > 0) {
			val wait = Core.getWaitTime(player);
			if (wait < 0) {
				msg("cooldown", player, -wait / 1000.0);
				return true;
			}
			Core.listenCallBack(player, Channel.TP, 1, (BiConsumer<String, String>) (name, display) -> {
				if (name.isEmpty()) msg("not-found", player, args[0]);
				else if (name.equals(player.getName())) {
					msg("self-tp", player);
				} else {
					msg("sender", player, name, display);
					addTpReq(player, name, display);
					Core.listenCallBack(player, Channel.TP, "5-" + name, false, WaitMaintain.T_User, (BoolConsumer) allow -> {
						if (allow) {
							Core.tpTo(player, name, wait, true);
							msg(wait > 0 ? "accept-wait" : "accept", player, name, display, wait);
						} else {
							msg("deny", player, name, display);
						}
					});
				}
			});
			Main.send(player, Channel.Tp.s0C_tpReq(args[0], Core.tpReqCode(player, 1)));
			return true;
		} else return msg("help", player);
	}

}
