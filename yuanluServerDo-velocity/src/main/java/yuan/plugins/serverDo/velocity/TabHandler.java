/**
 *
 */
package yuan.plugins.serverDo.velocity;

import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.ShareData.TabType;

import java.util.List;
import java.util.stream.Stream;

/**
 * tab处理器
 *
 * @author yuanlu
 * @deprecated Velocity不支持tab
 */
@SuppressWarnings("javadoc")
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TabHandler {
	private static void onAt(TabCompleteEvent e, String request, List<String> list) {
		if (!ConfigManager.isUseAt()) {
			list.add("@" + request);
			return;
		}

		Stream<String> stream = Main.getMain().getProxy().getAllPlayers()//
				.stream()//
				.map(Player::getUsername)//
				.map(String::toLowerCase);
		if (!request.isEmpty()) stream = stream.filter(request::startsWith);

		stream.map("@"::concat).forEach(list::add);
	}

	private static void onHome(TabCompleteEvent e, String request, List<String> list) {
		val player = e.getPlayer();
		if (player == null) return;
		val server = player.getCurrentServer().orElse(null);
		if (server == null) return;
		val serverName = server.getServerInfo().getName();
		Core.getHomes(player).forEach((name, loc) -> {
			if (name.toLowerCase().startsWith(request) && Core.canTp(serverName, loc.getServer()))//
				list.add(name);
		});
	}

	private static void onTp(TabCompleteEvent e, String request, List<String> list, boolean isAll) {
		val server = e.getPlayer().getCurrentServer().orElse(null);
		if (server == null) return;
		val serverName = server.getServerInfo().getName();
		if (ConfigManager.allowServer(serverName)) for (val p : Main.getMain().getProxy().getAllPlayers()) {
			val name = p.getUsername();
			if (name.toLowerCase().startsWith(request) && Core.canTp(isAll, serverName, p)) //
				list.add(name);
		}
	}

	private static void onWarp(TabCompleteEvent e, String request, List<String> list) {
		val server = e.getPlayer().getCurrentServer().orElse(null);
		if (server == null) return;
		val serverName = server.getServerInfo().getName();
		ConfigManager.WARPS.forEach((name, loc) -> {
			if (name.toLowerCase().startsWith(request) && Core.canTp(serverName, loc.getServer())) list.add(name);
		});
	}

	/**
	 * EVENT
	 *
	 * @param e 补全响应
	 */
	public static void TabComplete(TabCompleteEvent e) {
		val list = e.getSuggestions();
		if (list.size() != 1) return;
		val str = list.get(0);
		if (str != null) {
			val type = TabType.getType(ConfigManager.getTabReplace(), str);
			val request = TabType.getValue(ConfigManager.getTabReplace(), str);
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
			case AT:
				onAt(e, request, list);
				break;

			}
			if (ShareData.isDEBUG()) ShareData.getLogger().info(String.format("Tab: type=%s, req=%s, list=%s", type, request, list));
			if (list.isEmpty()) list.add(request);
		}
	}

}
