package yuan.plugins.serverDo.bukkit.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.val;
import yuan.plugins.serverDo.bukkit.Core;

/**
 * back命令
 *
 * @author yuanlu
 */
public final class CmdBack extends Cmd {

	/** @param name 命令名 */
	CmdBack(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val	player	= (Player) sender;
		val	loc		= Core.BackHandler.getBack(player);
		if (loc == null) return msg("non-back", sender);
		msg("backing", sender);
		Core.tpTo(player, loc);

		return true;
	}
}
