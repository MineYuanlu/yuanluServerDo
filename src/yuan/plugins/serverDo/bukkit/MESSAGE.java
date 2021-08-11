/**
 * auto: <br>
 * user: yuanlu<br>
 * date: 星期三 12 02 2020
 */
package yuan.plugins.serverDo.bukkit;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import yuan.plugins.serverDo.Tool;

/**
 * 语言文件
 * 
 * @author yuanlu
 *
 */
@SuppressWarnings("javadoc")
public interface MESSAGE {

	@Value
	@EqualsAndHashCode(callSuper = false)
	final class JsonMsg extends Msg {
		@NonNull String json, msg;

		public JsonMsg(@NonNull String json, String metaMsg) {
			this.json	= " "/* 减少字符串拼接次数 */ + json;
			msg			= metaMsg;
		}

		@Override
		public JsonMsg replace(String target, String replacement) {
			return new JsonMsg(json.substring(1).replace(target, replacement), msg.replace(target, replacement));
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
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	abstract class Msg {

		public abstract String getMsg();

		/**
		 * 替换部分内容
		 * 
		 * @param target      替换部分
		 * @param replacement 新内容
		 * @return
		 */
		public abstract Msg replace(String target, String replacement);

		/**
		 * 发送一条消息
		 * 
		 * @param sender 发送的对象
		 * @param msg    信息
		 * @param args   参数
		 */
		public abstract void send(CommandSender sender, Map<String, Object> args);

		/**
		 * 发送一条消息
		 * 
		 * @param sender 发送的对象
		 * @param msg    信息
		 * @param args   参数
		 */
		public abstract void send(CommandSender sender, Object... args);

		@Override
		public String toString() {
			return getMsg();
		}
	}

	@AllArgsConstructor
	@Value
	@EqualsAndHashCode(callSuper = false)
	final class StrMsg extends Msg {
		@NonNull String msg;

		@Override
		public StrMsg replace(String target, String replacement) {
			return new StrMsg(msg.replace(target, replacement));
		}

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

	Msg	NO_PERMISSION	= mes("no-permission");

	Msg	CMD_HELP		= mes("cmd.help");

	Msg	NOT_PLAYER		= mes("not-player");

	Msg	M_TIME_OUT		= mes("basic.message-time-out");
	Msg	BAD_VERSION		= mes("basic.version-bad");
	Msg	BC_ERROR		= mes("basic.bungee-error");
	Msg	BC_PLAYER_OFF	= mes("basic.bungee-player-offline");

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
