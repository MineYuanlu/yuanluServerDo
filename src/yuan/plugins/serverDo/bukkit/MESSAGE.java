/**
 * auto: <br>
 * user: yuanlu<br>
 * date: 星期三 12 02 2020
 */
package yuan.plugins.serverDo.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import yuan.plugins.serverDo.Tool;

/**
 * 语言文件
 *
 * @author yuanlu
 *
 */
@SuppressWarnings("javadoc")
public interface MESSAGE {
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class EmptyMsg extends MsgReal {
		public static final EmptyMsg INSTANCE = new EmptyMsg();

		@Override
		public String getMsg() {
			return "";
		}

		@Override
		public void send(CommandSender sender, Map<String, Object> args) {

		}

		@Override
		public void send(CommandSender sender, Object... args) {

		}

	}

	final class JsonMsg extends MsgReal {
		private final @NonNull @Getter String json, msg;

		private JsonMsg(@NonNull String json, String metaMsg) {
			this.json	= " "/* 减少字符串拼接次数 */ + json;
			msg			= metaMsg;
		}

		@Override
		public void send(CommandSender sender, Map<String, Object> args) {
			String cmd = Tool.parseVar(json, '<', '>', args);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + cmd);
		}

		@Override
		public void send(CommandSender sender, Object... args) {
			String cmd = this.json;
			if (args.length != 0) try {
				cmd = String.format(cmd, args);
			} catch (IllegalArgumentException e) {
				Main.getMain().getLogger().warning("错误的格式化: " + cmd + ", 参数: " + Arrays.deepToString(args));
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + cmd);
		}

	}

	/**
	 * 消息
	 *
	 * @see StrMsg
	 * @see JsonMsg
	 */
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	final class Msg {
		private static final Map<String, MsgReal>	MSG_REALS	= new ConcurrentHashMap<>();

		private static final Map<String, Msg>		MSGS		= new ConcurrentHashMap<>();

		private static Msg cache(String node, int type, MsgReal instance) {
			val key = type + "-" + node;
			MSG_REALS.put(key, instance);
			return MSGS.computeIfAbsent(key, Msg::new);
		}

		static Msg get(String node, int type) {
			return cache(node, type, EmptyMsg.INSTANCE);
		}

		static Msg get(@NonNull String node, int type, @NonNull String msg) {
			return cache(node, type, new StrMsg(msg));
		}

		static Msg get(@NonNull String node, int type, @NonNull String json, String metaMsg) {
			return cache(node, type, new JsonMsg(json, metaMsg));
		}

		static void reload() {
			val	msgs	= new ArrayList<>(MSGS.values());
			val	m		= Main.getMain();
			for (val msg : msgs) {
				val arg = msg.key.split("-", 2);
				m.mes(arg[1], Integer.parseInt(arg[0]));
			}
		}

		private @NonNull final String key;

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if ((obj == null) || (getClass() != obj.getClass())) return false;
			Msg other = (Msg) obj;
			return key.equals(other.key);
		}

		public String getMsg() {
			return getReal().getMsg();
		}

		@NonNull
		private MsgReal getReal() {
			val real = MSG_REALS.get(key);
			if (real == null) throw new InternalError("Bad node:" + key);
			return real;
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}

		/**
		 * 发送一条消息
		 *
		 * @param sender 发送的对象
		 * @param args   参数
		 */
		public void send(CommandSender sender, Map<String, Object> args) {
			getReal().send(sender, args);
		}

		/**
		 * 发送一条消息
		 *
		 * @param sender 发送的对象
		 * @param args   参数
		 */
		public void send(CommandSender sender, Object... args) {
			getReal().send(sender, args);
		}

		@Override
		public String toString() {
			return getMsg();
		}
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	abstract class MsgReal {
		public abstract String getMsg();

		/**
		 * 发送一条消息
		 *
		 * @param sender 发送的对象
		 * @param args   参数
		 */
		public abstract void send(CommandSender sender, Map<String, Object> args);

		/**
		 * 发送一条消息
		 *
		 * @param sender 发送的对象
		 * @param args   参数
		 */
		public abstract void send(CommandSender sender, Object... args);

		@Override
		public String toString() {
			return getMsg();
		}

	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	final class StrMsg extends MsgReal {
		private final @NonNull @Getter String msg;

		@Override
		public void send(CommandSender sender, @NonNull Map<String, Object> args) {
			String msg = Tool.parseVar(this.msg, '<', '>', args);
			sender.sendMessage(msg);
		}

		@Override
		public void send(CommandSender sender, Object... args) {
			String msg = this.msg;
			if (args.length != 0) try {
				msg = String.format(msg, args);
			} catch (IllegalArgumentException e) {
				Main.getMain().getLogger().warning("错误的格式化: " + msg + ", 参数: " + Arrays.deepToString(args));
			}
			sender.sendMessage(msg);
		}

	}

	Msg	NO_PERMISSION		= mes("no-permission");

	Msg	CMD_HELP			= mes("cmd.help");

	Msg	NOT_PLAYER			= mes("not-player");

	Msg	M_TIME_OUT			= mes("basic.message-time-out");
	Msg	BAD_VERSION			= mes("basic.version-bad");
	Msg	BC_ERROR			= mes("basic.bungee-error");
	Msg	BC_PLAYER_OFF		= mes("basic.bungee-player-offline");

	Msg	TPCANCEL_MOVE		= mes("tpcancel-move");
	Msg	VER_NO_RECOMMEND	= mes("version-no-recommend");

	static Msg mes(String node) {
		return Main.getMain().mes(node);
	}

	static Msg mes(String node, int type) {
		return Main.getMain().mes(node, type);
	}

	static String strMes(String node, int type) {
		return mes(node, type).getMsg();
	}
}
