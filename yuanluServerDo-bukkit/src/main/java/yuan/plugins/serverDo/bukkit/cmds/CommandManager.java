/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;

import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.bukkit.MESSAGE;
import yuan.plugins.serverDo.bukkit.Main;
import yuan.plugins.serverDo.bukkit.PackageUtil;

/**
 * 命令管理器
 *
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandManager implements MESSAGE {
	/** 命令信息 */
	@Value
	static class CmdInfo {
		/**
		 * 获取命令信息
		 *
		 * @param conf 配置节点
		 * @return 命令信息
		 */
		private static @NonNull CmdInfo getCmdInfo(@NonNull ConfigurationSection conf) {
			val	names			=conf.isString("names")? Collections.singletonList(conf.getString("names",null)) : conf.getStringList("names");
			val	permission		= conf.getString("permission", null);
			val	usageMessage	= conf.getString("usageMessage", "");
			val	description		= conf.getString("description", "");
			return new CmdInfo(description, usageMessage, names.stream().filter(Objects::nonNull).filter(s->!s.isEmpty()).collect(Collectors.toList()), permission, conf);
		}

		/** 描述 */
		String					description;
		/** 使用方法 */
		String					usageMessage;
		/** 所有名称 */
		List<String>			names;

		/** 权限 */
		String					permission;
		/** 额外数据 */
		ConfigurationSection	extra;
	}

	/**
	 * 所有的命令<br>
	 * {@code 命令名->命令信息}
	 */
	static final HashMap<String, CmdInfo> INFOS = new HashMap<>();

	/**
	 * 初始化所有命令
	 *
	 * @param conf 配置文件
	 */
	@SuppressWarnings("unchecked")
	public static void init(ConfigurationSection conf) {
		if (conf == null) return;
		val names=PackageUtil.getClassName(Cmd.class.getPackage().getName(),false);
		for (val name : names) {
			try {
				val c = Class.forName( name);
				if (c == Cmd.class || !Cmd.class.isAssignableFrom(c)) {
//					ShareData.getLogger().warning("[CMD] 非法命令: " + name);
					continue;
				}
				init(conf, (Class<? extends Cmd>) c);
			} catch (ClassNotFoundException e) {
				ShareData.getLogger().warning("[CMD] 未知命令: " + name);
				e.printStackTrace();
			}
		}

	}

	/**
	 * 初始化一条命令
	 *
	 * @param conf 配置文件
	 * @param c    目标类
	 */
	private static void init(ConfigurationSection conf, Class<? extends Cmd> c) {
		try {
			val cmd = Cmd.getCmdName(c);
			conf = conf.getConfigurationSection(cmd);
			if (conf == null) {
				ShareData.getLogger().warning("关闭命令: " + cmd);
				return;
			}
			val	constructor	= c.getDeclaredConstructor(String.class);
			val	info		= CmdInfo.getCmdInfo(conf);
			if (info.getNames().isEmpty()) ShareData.getLogger().warning("关闭命令: " + cmd);
			else for (val cmdName : info.getNames()) {
				INFOS.put(cmdName, info);
				register(constructor.newInstance(cmdName));
			}
		} catch (Exception e) {
			ShareData.getLogger().warning("注册命令时出错: " + c);
			e.printStackTrace();
		}
	}

	/**
	 * 注册命令<br>
	 * 将命令注册到Bukkit上
	 *
	 * @param <T> cmd
	 *
	 * @param cmd 命令
	 * @return input
	 */
	public static <T extends Command> T register(T cmd) {
		try {
			Method		method	= Bukkit.getServer().getClass().getMethod("getCommandMap");
			CommandMap	cmdm	= (CommandMap) method.invoke(Bukkit.getServer());
			val			b		= cmdm.register(Main.getMain().getName(), cmd);
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[D] register cmd: " + b + ", fbn:" + Main.getMain().getName() + ": " + cmd.getName());
		} catch (Exception e2) {
			System.err.println("CAN NOT REGISTER COMMAND: " + e2);
			Main.getMain().getLogger().warning("CAN NOT REGISTER COMMAND: " + e2);
		}
		return cmd;
	}
}
