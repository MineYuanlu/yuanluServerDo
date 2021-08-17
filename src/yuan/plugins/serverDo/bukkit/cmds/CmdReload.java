/**
 * 
 */
package yuan.plugins.serverDo.bukkit.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import yuan.plugins.serverDo.bukkit.Main;

/**
 * @author yuanlu
 *
 */
public final class CmdReload extends Cmd {

	/** @param name 命令名 */
	CmdReload(String name) {
		super(name);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			Main.getMain().reload();
			msg("success", sender);
		} else {
			msg("only-console", sender);
		}
		return false;
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		throw new InternalError();
	}

}
