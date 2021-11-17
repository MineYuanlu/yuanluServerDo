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
import yuan.plugins.serverDo.bukkit.Core.Permissions;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * sethome命令
 *
 * @author yuanlu
 *
 */
public final class CmdSetHome extends Cmd {

	/** @param name 命令名 */
	CmdSetHome(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val	player	= (Player) sender;
		val	name	= args.length > 0 ? args[0] : "home";
		val	loc		= Core.toSLoc(player.getLocation());
		val	a		= Permissions.getMaxAmount(sender, "home");
		Core.listenCallBack(player, Channel.HOME, 0, (BoolConsumer) success -> {
			msg(success ? "success" : "fail", player, name, a);
		});
		Main.send(player, Channel.Home.s0C_setHome(name, loc, a));
		return false;
	}

}
