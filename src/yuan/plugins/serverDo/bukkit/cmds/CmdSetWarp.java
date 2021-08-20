/**
 * 
 */
package yuan.plugins.serverDo.bukkit.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.val;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * setwarp命令
 * 
 * @author yuanlu
 *
 */
public final class CmdSetWarp extends Cmd {

	/** @param name 命令名 */
	CmdSetWarp(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		if (args.length > 0) {
			val	player	= (Player) sender;
			val	name	= args[0];
			val	loc		= Core.toSLoc(player.getLocation());
			Core.listenCallBack(player, Channel.WARP, 0, (Runnable) () -> {
				msg("success", player, name);
			});
			Main.send(player, Channel.Warp.s0C_setWarp(name, loc));
		} else msg("help", sender);
		return false;
	}

}
