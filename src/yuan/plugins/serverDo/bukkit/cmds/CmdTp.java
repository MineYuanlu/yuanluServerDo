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
import yuan.plugins.serverDo.bukkit.Core.Permissions;
import yuan.plugins.serverDo.bukkit.Main;

/**
 * tp命令
 * 
 * @author yuanlu
 *
 */
public final class CmdTp extends TabTp {

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
				if (name.isEmpty()) msg("not-found", player, args[0]);
				else {
					msg("sender", player, name, display);
					Core.tpTo(player, name, -1, false);
				}
			});
			Main.send(player, Channel.Tp.s0C_tpReq(args[0], Core.tpReqCode(player, 0)));
			return true;
		case 2:// tp mover target
			if (!Permissions.tpOther_(sender)) return true;
			Core.listenCallBack(player, Channel.TP, 0xa, (BiPlayerConsumer) (mn, md, tn, td) -> {
				if (mn.isEmpty()) msg("not-found", player, args[0]);
				else if (tn.isEmpty()) msg("not-found", player, args[1]);
				else {
					msg("third-tp", player, mn, md, tn, td);
					Core.tpTo(player, mn, tn, -1);
				}
			});
			int code = Permissions.tpSenior(sender) ? 1 : 0;// 是否拥有高级传送权限
			Main.send(player, Channel.Tp.s9C_tpReqThird(args[0], args[1], code));
		}
		return false;
	}
}
