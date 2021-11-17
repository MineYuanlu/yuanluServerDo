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
 * delwarp命令
 *
 * @author yuanlu
 *
 */
public final class CmdDelWarp extends TabWarp {

	/** @param name 命令名 */
	CmdDelWarp(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		if (args.length > 0) {
			val	player	= (Player) sender;
			val	name	= args[0];
			Core.listenCallBack(player, Channel.WARP, 1, (BoolConsumer) success -> {
				msg(success ? "success" : "fail", player, name);
			});
			Main.send(player, Channel.Warp.s1C_delWarp(name));
		} else msg("help", sender);
		return false;
	}

}
