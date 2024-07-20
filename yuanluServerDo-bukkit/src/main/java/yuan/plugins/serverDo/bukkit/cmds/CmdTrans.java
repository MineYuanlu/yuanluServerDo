/**
 *
 */
package yuan.plugins.serverDo.bukkit.cmds;

import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.bukkit.third.Third;
import yuan.plugins.serverDo.bukkit.third.Third.TransMethods;

import java.util.List;

/**
 * trans命令
 *
 * @author yuanlu
 */
public final class CmdTrans extends Cmd {

	/** @param name 命令名 */
	CmdTrans(String name) {
		super(name);
	}

	@Override
	protected boolean execute0(CommandSender sender, String[] args) {
		val player = (Player) sender;
		if (args.length <= 0) return msg("help", sender);

		val third = Third.get(args[0]);
		if (third == null) return msg("not-found", sender, args[0]);
		if (!third.isValid()) return msg("invalid", sender, third.getName());

		if (args.length <= 1) return msg("help-" + third.getName(), sender);
		val tm = TransMethods.getByName(args[1]);
		if (!third.canDo(tm)) return msg("cannot", sender, third.getName(), tm);

		val title = msg("title", 1).getMsg();
		val subtitle = msg("sub-title", 1).getMsg();
		tm.handle(third, player, (n, a) -> {
			player.sendTitle(String.format(title, tm.getName()), String.format(subtitle, n, Math.abs(a)), 0, 100, 0);
			if (a <= 0) msg("success", sender, n, -a);
		});
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		if (args.length == 1) {// plugin
			return Tool.getMatchList(args[0], Third.THIRDS_VIEW);
		} else if (args.length == 2) {// func
			val third = Third.get(args[0]);
			if (third != null) return Tool.getMatchList(args[1], third.getCanDos(), c -> c.getName());
		}
		return super.tabComplete(sender, alias, args);
	}

}
