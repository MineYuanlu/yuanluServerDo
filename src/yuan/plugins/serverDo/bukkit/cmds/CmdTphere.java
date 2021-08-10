/**
 * yuanlu
 * date: 2021年8月10日
 * file: CmdTphere.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.function.BiConsumer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * tphere命令
 * 
 * @author yuanlu
 *
 */
public final class CmdTphere extends Cmd {

	/** @param name 命令名 */
	protected CmdTphere(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (args.length > 0) {
			Core.listenCallBack(player, Channel.TP, 1, (BiConsumer<String, String>) (name, display) -> {
				msg("success", player, name, display);
				Core.tpTo(name, player);
		});
			Main.send(player, Channel.Tp.s0C_tpReq(args[0], 2));
		} else return msg("help", player);
		return false;
	}

}
