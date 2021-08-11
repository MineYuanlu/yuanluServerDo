/**
 * yuanlu
 * date: 2021年8月11日
 * file: Tpdeny.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.NonNull;

/**
 * Tpdeny命令
 * 
 * @author yuanlu
 *
 */
public final class Tpdeny extends Cmd {

	/** @param name 命令名 */
	protected Tpdeny(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		CmdTpaccept.handleRequest((@NonNull Player) sender, args.length > 0 ? args[0] : null, false, this);
		return false;
	}

}
