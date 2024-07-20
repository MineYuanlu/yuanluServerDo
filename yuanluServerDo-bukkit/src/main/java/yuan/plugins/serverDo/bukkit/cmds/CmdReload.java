/**
 *
 */
package yuan.plugins.serverDo.bukkit.cmds;

import lombok.NonNull;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * reload命令
 *
 * @author yuanlu
 */
public final class CmdReload extends Cmd {

	/** @param name 命令名 */
	CmdReload(String name) {
		super(name);
	}

	@Override
	public boolean execute(@NonNull CommandSender sender, @NonNull String commandLabel, String @NonNull [] args) {
		if (sender instanceof ConsoleCommandSender) {
			Main.getMain().reload();
			msg("success", sender);
		} else {
			msg("only-console", sender);
		}
		return true;
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		throw new InternalError();
	}

}
