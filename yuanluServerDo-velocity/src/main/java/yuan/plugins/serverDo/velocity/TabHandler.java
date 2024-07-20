/**
 *
 */
package yuan.plugins.serverDo.velocity;

import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.ShareData.TabType;

import java.util.List;
import java.util.stream.Stream;

/**
 * tab处理器
 *
 * @author yuanlu
 */
@SuppressWarnings("javadoc")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TabHandler {
	private static void onAt(Player player, String request, List<String> list) {
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

	private static void onHome(Player player, String request, List<String> list) {
		val server = player.getCurrentServer().orElse(null);
		if (server == null) return;
		val serverName = server.getServerInfo().getName();
		Core.getHomes(player).forEach((name, loc) -> {
			if (name.toLowerCase().startsWith(request) && Core.canTp(serverName, loc.getServer()))//
				list.add(name);
		});
	}

	private static void onTp(Player player, String request, List<String> list, boolean isAll) {
		val server = player.getCurrentServer().orElse(null);
		if (server == null) return;
		val serverName = server.getServerInfo().getName();
		if (ConfigManager.allowServer(serverName)) for (val p : Main.getMain().getProxy().getAllPlayers()) {
			val name = p.getUsername();
			if (name.toLowerCase().startsWith(request) && Core.canTp(isAll, serverName, p)) //
				list.add(name);
		}
	}

	private static void onWarp(Player player, String request, List<String> list) {
		val server = player.getCurrentServer().orElse(null);
		if (server == null) return;
		val serverName = server.getServerInfo().getName();
		ConfigManager.WARPS.forEach((name, loc) -> {
			if (name.toLowerCase().startsWith(request) && Core.canTp(serverName, loc.getServer())) list.add(name);
		});
	}

	/**
	 * EVENT
	 *
	 * @param list 补全响应
	 */
	public static void TabComplete(Player player, @NonNull List<String> list) {
		if (list.size() != 1 || player == null) return;
		val str = list.get(0);
		if (str != null) {
			val type = TabType.getType(ConfigManager.getTabReplace(), str);
			val request = TabType.getValue(ConfigManager.getTabReplace(), str);
			if (type == null || request == null) return;
			list.clear();
			switch (type) {
			case TP_ALL:
				onTp(player, request, list, true);
				break;
			case TP_NORMAL:
				onTp(player, request, list, false);
				break;
			case WARP:
				onWarp(player, request, list);
				break;
			case HOME:
				onHome(player, request, list);
				break;
			case AT:
				onAt(player, request, list);
				break;

			}
			if (ShareData.isDEBUG()) ShareData.getLogger().info(String.format("Tab: type=%s, req=%s, list=%s", type, request, list));
			if (list.isEmpty()) list.add(request);
		}
	}

	/**
	 * EVENT
	 *
	 * @param e 补全响应
	 */
	public static void TabComplete(TabCompleteEvent e) {
		val list = e.getSuggestions();
		TabComplete(e.getPlayer(), list);
	}

}
