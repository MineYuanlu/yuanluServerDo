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

/**
 * setwarp命令
 *
 * @author yuanlu
 */
public final class CmdSetSpawn extends Cmd {

	/** @param name 命令名 */
	CmdSetSpawn(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val player = (Player) sender;
		val loc = Core.toSLoc(player.getLocation());
		Core.listenCallBack(player, Channel.WARP, 0, (Runnable) () -> {
			msg("success", player, CmdSpawn.NAME);
		});
		Main.send(player, Channel.Warp.s0C_setWarp(CmdSpawn.NAME, loc));
		return true;
	}

}
