/**
 * 
 */
package yuan.plugins.serverDo.bungee;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import yuan.plugins.serverDo.ShareData.TabType;

/**
 * tab处理器
 * 
 * @author yuanlu
 *
 */
@SuppressWarnings("javadoc")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TabHandler {
	private static void onHome(TabCompleteResponseEvent e, String request, List<String> list) {
		val	server	= (Server) e.getSender();
		val	player	= (ProxiedPlayer) e.getReceiver();
		if (server == null || player == null) return;
		val serverName = server.getInfo().getName();
		Core.getHomes(player).forEach((name, loc) -> {
			if (Core.canTp(serverName, loc.getServer())) list.add(name);
		});
	}

	private static void onTp(TabCompleteResponseEvent e, String request, List<String> list, boolean isAll) {
		val server = (Server) e.getSender();
		if (server == null) return;
		val target = server.getInfo().getName();
		if (ConfigManager.allowServer(target)) for (val p : Main.getMain().getProxy().getPlayers()) {
			val name = p.getName();
			if (name.toLowerCase().startsWith(request) && Core.canTp(isAll, target, p)) //
				list.add(name);
		}
	}

	private static void onWarp(TabCompleteResponseEvent e, String request, List<String> list) {
		val server = (Server) e.getSender();
		if (server == null) return;
		val serverName = server.getInfo().getName();
		ConfigManager.WARPS.forEach((name, loc) -> {
			if (Core.canTp(serverName, loc.getServer())) list.add(name);
		});
	}

	/**
	 * EVENT
	 * 
	 * @param e 补全响应
	 */
	public static void TabCompleteResponse(TabCompleteResponseEvent e) {
		val list = e.getSuggestions();
		if (list.size() != 1) return;
		val str = list.get(0);
		if (str != null) {
			val	type	= TabType.getType(ConfigManager.getTabReplace(), str);
			val	request	= TabType.getValue(ConfigManager.getTabReplace(), str);
			if (type == null || request == null) return;
			list.clear();
			switch (type) {
			case TP_ALL:
				onTp(e, request, list, true);
				break;
			case TP_NORMAL:
				onTp(e, request, list, false);
				break;
			case WARP:
				onWarp(e, request, list);
				break;
			case HOME:
				onHome(e, request, list);
				break;

			}
			if (list.isEmpty()) list.add(request);
		}
	}

}
