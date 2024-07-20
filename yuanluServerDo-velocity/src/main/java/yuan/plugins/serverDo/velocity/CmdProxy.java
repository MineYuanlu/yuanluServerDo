package yuan.plugins.serverDo.velocity;

import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.NonNull;
import lombok.val;
import yuan.plugins.serverDo.Channel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CmdProxy {
	private static final Set<String> PROXYED_CMDS = ConcurrentHashMap.newKeySet();
	ProxyServer proxy;

	public static boolean isProxyed(String cmd) {
		return PROXYED_CMDS.contains(cmd);
	}

	public static void addCmds(@NonNull String namespace, @NonNull Collection<String> cmds) {
		ArrayList<String> newCmds = new ArrayList<>(0);
		synchronized (PROXYED_CMDS) {
			for (String cmd : cmds) {
				if (PROXYED_CMDS.add(cmd)) newCmds.add(cmd);
				val ncmd = namespace + ":" + cmd;
				if (PROXYED_CMDS.add(ncmd)) newCmds.add(ncmd);
			}
		}
		if (newCmds.isEmpty()) return;
		val fst = newCmds.remove(newCmds.size() - 1);
		Main.getMain().getProxy().getCommandManager().register(fst, new SuggestCommand(), newCmds.toArray(new String[0]));
	}

	public static void cmdTabCallback(long id, Player player, @NonNull ArrayList<String> tabs) {
		TabHandler.TabComplete(player, tabs);
		val future = SuggestCommand.FUTURE_MAP.remove(id);
		if (future == null) return;
		future.complete(tabs);
	}

	private static class SuggestCommand implements RawCommand {
		private static final Map<Long, CompletableFuture<List<String>>> FUTURE_MAP = new ConcurrentHashMap<>();
		private static final AtomicLong                                 counter    = new AtomicLong(0);

		@Override
		public void execute(Invocation invocation) {
			throw new InternalError("Bad command call, logic error!!");
		}

		@Override
		public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
			CompletableFuture<List<String>> future = new CompletableFuture<>();
			if (invocation.source() instanceof Player) {
				val p = (Player) invocation.source();
				val id = counter.incrementAndGet();
				Main.send(p, Channel.TabParse.sendS(id, invocation.alias() + " " + invocation.arguments()));
				FUTURE_MAP.put(id, future);
			} else {
				future.complete(new ArrayList<>(0));
			}
			return future;
		}
	}
}
