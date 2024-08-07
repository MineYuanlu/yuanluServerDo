/**
 * yuanlu
 * date: 2021年8月10日
 * file: CmdTpahere.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.WaitMaintain;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

import java.util.function.BiConsumer;

/**
 * tpahere命令
 *
 * @author yuanlu
 */
public final class CmdTpahere extends TabTp {
	/** @param name 命令名 */
	protected CmdTpahere(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (args.length > 0) {
			Core.listenCallBack(player, Channel.TP, 1, (BiConsumer<String, String>) (name, display) -> {
				if (name.isEmpty()) msg("not-found", player, args[0]);
				else if (name.equals(player.getName())) {
					msg("self-tp", player);
				} else {
					msg("sender", player, name, display);
					CmdTpa.addTpReq(player, name, display);
					Core.listenCallBack(player, Channel.TP, "5-" + name, false, WaitMaintain.T_User, (BoolConsumer) allow -> {
						msg(allow ? "accept" : "deny", player, name, display);
					});
				}
			});
			Main.send(player, Channel.Tp.s0C_tpReq(args[0], Core.tpReqCode(player, 3)));
		} else return msg("help", player);
		return false;
	}

}
