/**
 * yuanlu
 * date: 2021年8月10日
 * file: CmdTpa.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.function.BiConsumer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.WaitMaintain;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * tpa命令
 * 
 * @author yuanlu
 *
 */
public final class CmdTpa extends Cmd {
	/** @param name 命令名 */
	protected CmdTpa(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (args.length > 0) {
			Core.listenCallBack(player, Channel.TP, 1, (BiConsumer<String, String>) (name, display) -> {
				msg("send", player, name, display);
				Core.listenCallBack(player, Channel.TP, 5, WaitMaintain.T_User, (BoolConsumer) allow -> {
					msg(allow ? "accept" : "deny", player, name, display);
					if (allow) Core.tpTo(player, name);
				});
			});
			Main.send(player, Channel.Tp.s0C_tpReq(args[0], 1));
			return true;
		} else return msg("help", player);
	}

}
