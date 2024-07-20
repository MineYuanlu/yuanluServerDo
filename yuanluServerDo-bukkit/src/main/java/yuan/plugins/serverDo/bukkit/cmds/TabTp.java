/**
 *
 */
package yuan.plugins.serverDo.bukkit.cmds;

import lombok.NonNull;
import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Core.Permissions;

import java.util.Collections;
import java.util.List;

/**
 * tab
 *
 * @author yuanlu
 */
public abstract class TabTp extends Cmd {

	/** @param name 命令名 */
	TabTp(String name) {
		super(name);
	}

	@Override
	public @NonNull List<String> tabComplete(@NonNull CommandSender sender, @NonNull String alias, String @NonNull [] args) {
		if (sender instanceof Player) {
			val player = (Player) sender;
			val l = Core.TabHandler.getTabReplaceTp(player, args.length > 0 ? args[args.length - 1] : "", Permissions.tpSenior(player));
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[TAB] " + l);
			return l;
		}
		return Collections.emptyList();
	}

}
