/**
 * 
 */
package yuan.plugins.serverDo.bungee;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import com.google.common.base.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.config.Configuration;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.WaitMaintain;

/**
 * 配置管理器
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

	/**
	 * 配置文件
	 * 
	 * @author yuanlu
	 *
	 */
	@Getter
	public static enum ConfFile {
		/** 自动隐身 */
		ALWAYS_VANISH("alwaysvanish.uid") {
			@Override
			protected void load0() throws IOException {
				try (val in = new BufferedReader(new FileReader(getFile()))) {
					in.lines().forEach(s -> {
						try {
							Core.alwaysVanish.add(UUID.fromString(s));
						} catch (IllegalArgumentException e) {
							ShareData.getLogger().warning("[Conf] " + fname + ": Bad UUID: " + s);
							e.printStackTrace();
						}
					});
				}
			}

			@Override
			protected void save0() throws IOException {
				try (val out = new BufferedWriter(new FileWriter(getFile()))) {
					for (val u : Core.alwaysVanish) {
						out.write(u.toString());
						out.write('\n');
					}
				}
			}

		};

		/** 保存延时 */
		private static final EnumMap<ConfFile, Long>	SAVE_DELAY	= new EnumMap<>(ConfFile.class);

		/** 文件名 */
		protected final String							fname;

		/** @param fname file name */
		private ConfFile(String fname) {
			this.fname = fname;
		}

		/** @return file */
		public File getFile() {
			val folder = Main.getMain().getDataFolder();
			return new File(folder, fname);
		}

		/** 加载 */
		protected void load() {
			try {
				load0();
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				ShareData.getLogger().warning("[Conf] " + fname + ":");
				e.printStackTrace();
			}
		}

		/**
		 * 实际加载
		 * 
		 * @throws IOException IOE
		 */
		protected abstract void load0() throws IOException;

		/** 保存 */
		protected void save() {
			try {
				save0();
			} catch (IOException e) {
				ShareData.getLogger().warning("[Conf] " + fname + ":");
				e.printStackTrace();
			}
		}

		/**
		 * 实际保存
		 * 
		 * @throws IOException IOE
		 */
		protected abstract void save0() throws IOException;
	}

	/** 配置文件 */
	private static @Getter Configuration					config;
	/** tab替换 */
	private static @Getter @Setter String					tabReplaceNor;

	/** tab替换 */
	private static @Getter @Setter String					tabReplaceAll;

	/** 服务器信息 */
	private static byte[]									serverInfo;

	/** 出错 */
	private static @Getter boolean							errorGroup;
	/** 服务器组 */
	private static final HashMap<String, HashSet<String>>	GROUPS		= new HashMap<>();

	/** 禁用的服务器 */
	private static final HashSet<String>					BAN_SERVER	= new HashSet<>();

	/**
	 * 检测是否启用服务器
	 * 
	 * @param server 服务器
	 * @return 此服务器是否启用本插件
	 */
	public static boolean allowServer(String server) {
		return !BAN_SERVER.contains(server);
	}

	/**
	 * 检测是否可以传送
	 * 
	 * @param s1 服务器1
	 * @param s2 服务器2
	 * @return 是否可以传送
	 */
	public static boolean canTp(String s1, String s2) {
		if (BAN_SERVER.contains(s1) || BAN_SERVER.contains(s2)) return false;
		if (Objects.equal(s1, s2)) return true;
		if (errorGroup) {
			if (Main.isDEBUG()) Main.getMain().getLogger().warning("error: 服务器组不存在");
			return false;
		}
		val group = GROUPS.get(s1);
		return group != null && group.contains(s2);
	}

	/**
	 * 关闭时保存
	 */
	public static void closeSave() {
		val list = new ArrayList<>(ConfFile.SAVE_DELAY.keySet());
		ConfFile.SAVE_DELAY.clear();
		list.forEach(cf -> cf.save());
	}

	/**
	 * 初始化
	 * 
	 * @param config config
	 */
	public static void init(Configuration config) {
		ConfigManager.config = config;
		val tabReplace = config.getString("player-tab-replace", "yl★:" + Tool.randomString(8));
		setTabReplaceNor(tabReplace + "-nor");
		setTabReplaceAll(tabReplace + "-all");
		loadGroup(config);

		serverInfo = Channel.ServerInfo.sendS(tabReplaceAll, tabReplaceNor, Main.getMain().getDescription().getVersion());

		Arrays.stream(ConfFile.values()).forEach(c -> c.load());

	}

	/**
	 * 加载组
	 * 
	 * @param config 配置文件
	 */
	private static void loadGroup(Configuration config) {
		val sg = config.getSection("server-group");
		if (sg == null) {
			errorGroup = true;
			Main.getMain().getLogger().warning("[SERVER GROUP] config error!");
			return;
		}
		for (val key : sg.getKeys()) {
			val group = sg.getStringList(key);
			for (val server : group) {
				HashSet<String> canTp = GROUPS.get(server);
				if (canTp == null) GROUPS.put(server, canTp = new HashSet<>());
				canTp.addAll(group);
			}
			if (ShareData.isDEBUG()) ShareData.getLogger().info("加载组 " + key + ": " + group);
		}

		BAN_SERVER.clear();
		BAN_SERVER.addAll(config.getStringList("server-ban"));
	}

	/**
	 * 保存配置
	 * 
	 * @param f 配置类型
	 */
	public static void saveConf(ConfFile f) {
		WaitMaintain.put(ConfFile.SAVE_DELAY, f, System.currentTimeMillis(), 1000 * 60, () -> f.save());
	}

	/**
	 * 发送BC信息给子服务器
	 * 
	 * @param server 服务器
	 */
	public static void sendBungeeInfoToServer(Server server) {
		Main.send(server, serverInfo);
	}
}
