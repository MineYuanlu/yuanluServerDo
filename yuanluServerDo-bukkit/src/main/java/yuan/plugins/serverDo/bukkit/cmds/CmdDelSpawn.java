/**
 *
 */
package yuan.plugins.serverDo.bukkit.cmds;

import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BoolConsumer;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * delspawn命令
 *
 * @author yuanlu
 */
public final class CmdDelSpawn extends Cmd {

	/** @param name 命令名 */
	CmdDelSpawn(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val player = (Player) sender;
		Core.listenCallBack(player, Channel.WARP, 1, (BoolConsumer) success -> {
			msg(success ? "success" : "fail", player, CmdSpawn.NAME);
		});
		Main.send(player, Channel.Warp.s1C_delWarp(CmdSpawn.NAME));
		return true;
	}

}
