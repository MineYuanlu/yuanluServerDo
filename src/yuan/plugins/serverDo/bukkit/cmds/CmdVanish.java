/**
 * 
 */
package yuan.plugins.serverDo.bukkit.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.val;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * vanish命令
 * 
 * @author yuanlu
 *
 */
public final class CmdVanish extends Cmd {

	/** @param name 命令名 */
	CmdVanish(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val		player	= (Player) sender;
		boolean	isAlway	= false;
		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
			case "a":
			case "alway":
			case "always":
			case "总是":
				isAlway = true;
				break;
			default:
				msg("help", sender);
				return true;
			}
		}
		val node = isAlway ? "always-" : "";
		Core.listenCallBack(player, Channel.VANISH, null, (BoolConsumer) hide -> {
			msg(node + (hide ? "hide" : "show"), player);
		});
		Main.send(player, Channel.Vanish.sendS(true));
		return false;
	}

}
