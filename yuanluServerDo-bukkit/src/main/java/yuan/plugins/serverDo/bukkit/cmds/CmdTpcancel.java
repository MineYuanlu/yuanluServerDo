/**
 *
 */
package yuan.plugins.serverDo.bukkit.cmds;

import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

import java.util.List;

/**
 * tpcancel命令
 *
 * @author yuanlu
 */
public final class CmdTpcancel extends Cmd {
	/** @param name 命令名 */
	protected CmdTpcancel(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val player = (Player) sender;
		val who = args.length > 0 ? args[0] : null;
		CmdTpa.removeTpReq(player, who, (name, display) -> {
			if (name == null) msg("no-request", player);
			else if (name.isEmpty()) msg("not-found", player, who);
			else {
				Core.removeCallBack(player, Channel.TP, "5-" + name);
				Main.send(player, Channel.Tp.sbC_cancel(name));
				msg("success", player, name, display);
			}
		});
		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return CmdTpa.getReqList(sender);
	}
}
