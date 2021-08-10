/**
 * yuanlu
 * date: 2021年8月10日
 * file: CmdTp.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.function.BiConsumer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BiPlayerConsumer;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * tp命令
 * 
 * @author yuanlu
 *
 */
public final class CmdTp extends Cmd {

	/** @param name 命令名 */
	protected CmdTp(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		switch (args.length) {
		case 0:
			return msg("help", player);
		case 1:// tp target
			Core.listenCallBack(player, Channel.TP, 1, (BiConsumer<String, String>) (name, display) -> {
				msg("tp", player, name, display);
				Core.tpTo(player, name);
			});
			Main.send(player, Channel.Tp.s0C_tpReq(args[0], 0));
			return true;
		case 2:// tp mover target
			Core.listenCallBack(player, Channel.TP, 0xa, (BiPlayerConsumer) (mn, md, tn, td) -> {
				msg("tp-third", player, mn, md, tn, td);
				Core.tpTo(player, mn, tn);
			});
			Main.send(player, Channel.Tp.s9C_tpReqThird(args[0], args[1]));
		}
		return false;
	}

}
