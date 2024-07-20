/**
 *
 */
package yuan.plugins.serverDo.velocity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Objects;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.LRUCache;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.ShareLocation;
import yuan.plugins.serverDo.Tool;
import yuan.plugins.serverDo.Tool.ThrowableFunction;
import yuan.plugins.serverDo.Tool.ThrowableRunnable;
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
	@AllArgsConstructor
	public enum ConfFile {
		/** 自动隐身 */
		ALWAYS_VANISH("alwaysvanish.uid") {
			@Override
			protected void load0() throws IOException {
				try (BufferedReader in = new BufferedReader(new FileReader(getFile()))) {
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
				try (BufferedWriter out = new BufferedWriter(new FileWriter(getFile()))) {
					for (UUID u : Core.alwaysVanish) {
						out.write(u.toString());
						out.write('\n');
					}
				}
			}

		},
		/** 传送地标 */
		WARP("warp.yml") {

			@Override
			protected void load0() throws IOException {
				Configuration warps = YAML.load(getFile());
				for (String name : warps.getKeys()) {
					Configuration	warp	= warps.getSection(name);

					String	world	= warp.getString("world", null);
					String	server	= warp.getString("server", null);
					double	x		= warp.getDouble("x", Double.NaN);
					double	y		= warp.getDouble("y", Double.NaN);
					double	z		= warp.getDouble("z", Double.NaN);
					float	Y		= warp.getFloat("yaw", Float.NaN);
					float	P		= warp.getFloat("pitch", Float.NaN);
					if (world == null || server == null || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Float.isNaN(Y) || Float.isNaN(P)) {
						ShareData.getLogger().warning(String.format("[WARPS] 错误的warp数据: %s %s [%s, %s, %s] [%s,%s]", server, world, x, y, z, Y, P));
					} else {
						WARPS.put(name, new ShareLocation(x, y, z, Y, P, world, server));
					}
				}
			}

			@Override
			protected void save0() throws IOException {
				Configuration warps = new Configuration();
				for (Map.Entry<String, ShareLocation> e : WARPS.entrySet()) {
					String	name	= e.getKey();
					ShareLocation	warp	= e.getValue();
					warps.set(name + ".world", warp.getWorld());
					warps.set(name + ".server", warp.getServer());
					warps.set(name + ".x", warp.getX());
					warps.set(name + ".y", warp.getY());
					warps.set(name + ".z", warp.getZ());
					warps.set(name + ".yaw", warp.getYaw());
					warps.set(name + ".pitch", warp.getPitch());
				}
				YAML.save(warps, getFile());
			}

		};

		/** 保存延时 */
		private static final EnumMap<ConfFile, Long>	SAVE_DELAY	= new EnumMap<>(ConfFile.class);

		/** Yaml处理器 */
		protected static final ConfigurationProvider	YAML		= ConfigurationProvider.getProvider(YamlConfiguration.class);
		/** 文件名 */
		protected final String							fname;

		/** @return file */
		public File getFile() {
			val folder = Main.getMain().getDataFolder();
			return folder.resolve(fname).toFile();
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

	/**
	 * Home
	 *
	 * @author yuanlu
	 *
	 */
	public static final class HomesLRU extends LRUCache<UUID, HashMap<String, ShareLocation>> {

		/** 配置文件 */
		private static final PlayerConfFile HOME = PlayerConfFile.HOME;

		/** @param size size */
		private HomesLRU(int size) {
			super(size);
		}

		@Override
		protected void clearHandle(UUID k, HashMap<String, ShareLocation> v) {
			HOME.save(k, () -> save0(k, v));
		}

		@Override
		protected HashMap<String, ShareLocation> create(UUID k) {
			HashMap<String, ShareLocation> data = HOME.load(k, this::load0);
			return data == null ? new HashMap<>() : data;
		}

		/**
		 * load
		 *
		 * @param uid UUID
		 * @return data
		 * @throws IOException IOE
		 */
		private HashMap<String, ShareLocation> load0(@NonNull UUID uid) throws IOException {
			HashMap<String, ShareLocation>	m		= new HashMap<>();
			val								f		= HOME.getFile(uid, false);
			val								warps	= PlayerConfFile.YAML.load(f);
			for (val name : warps.getKeys()) {
				val	warp	= warps.getSection(name);

				val	world	= warp.getString("world", null);
				val	server	= warp.getString("server", null);
				val	x		= warp.getDouble("x", Double.NaN);
				val	y		= warp.getDouble("y", Double.NaN);
				val	z		= warp.getDouble("z", Double.NaN);
				val	Y		= warp.getFloat("yaw", Float.NaN);
				val	P		= warp.getFloat("pitch", Float.NaN);
				if (world == null || server == null || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Float.isNaN(Y) || Float.isNaN(P)) {
					ShareData.getLogger()
							.warning(String.format("[HOMES] 错误的home数据: %s: %s %s [%s, %s, %s] [%s,%s]", f.getName(), server, world, x, y, z, Y, P));
				} else {
					m.put(name, new ShareLocation(x, y, z, Y, P, world, server));
				}
			}
			return m;
		}

		/**
		 * save
		 *
		 * @param uid uuid
		 * @param map data
		 * @throws IOException IOE
		 */
		private void save0(@NonNull UUID uid, HashMap<String, ShareLocation> map) throws IOException {
			val warps = new Configuration();
			for (val e : map.entrySet()) {
				val	name	= e.getKey();
				val	warp	= e.getValue();
				warps.set(name + ".world", warp.getWorld());
				warps.set(name + ".server", warp.getServer());
				warps.set(name + ".x", warp.getX());
				warps.set(name + ".y", warp.getY());
				warps.set(name + ".z", warp.getZ());
				warps.set(name + ".yaw", warp.getYaw());
				warps.set(name + ".pitch", warp.getPitch());
			}
			PlayerConfFile.YAML.save(warps, PlayerConfFile.HOME.getFile(uid, true));
		}
	}

	/**
	 * 玩家配置文件<br>
	 * load: 被动式加载, 由LRU调用, 通过 {@link #load(Object, ThrowableFunction) 框架函数}
	 * 调用实际加载函数, 加载数据<br>
	 * save: 被动式保存, 由LRU调用, 通过 {@link #save(UUID, ThrowableRunnable) 框架函数} 调用实际保存函数,
	 * 保存数据<br>
	 * save: 主动式保存, 通过 {@link #save(UUID)} 触发, 由具体配置指定LRU, 调用其保存函数
	 *
	 * @author yuanlu
	 *
	 */
	@Getter
	@AllArgsConstructor
	public enum PlayerConfFile {
		/** 传送家 */
		HOME("home.yml") {
			@Override
			protected void save(UUID u) {
				HashMap<String, ShareLocation> map = HOMES.check(u);
				if (map != null) HOMES.clearHandle(u, map);
			}
		};

		/** Yaml处理器 */
		protected static final ConfigurationProvider	YAML		= ConfigurationProvider.getProvider(YamlConfiguration.class);

		/** 保存延时 */
		private final ConcurrentHashMap<UUID, Long>		SAVE_DELAY	= new ConcurrentHashMap<>();

		/** 文件名 */
		protected final String							fname;
		/** 需要保存 */
		protected final HashSet<UUID>					needSave	= new HashSet<>();

		/**
		 * @param u  UUID
		 * @param mk 是否创建文件夹
		 * @return file
		 */
		public File getFile(UUID u, boolean mk) {
			val	folder	= Main.getMain().getDataFolder();
			val	uuid	= u.toString();
			folder.resolve(uuid.substring(0, 2)).resolve(uuid);
			if (mk) folder.toFile().mkdirs();
			return folder.resolve(fname).toFile();
		}

		/**
		 * 加载
		 *
		 * @param <T> T
		 * @param <R> R
		 * @param t   输入数据
		 * @param r   运行体
		 * @return result
		 */
		protected <T, R> R load(T t, ThrowableFunction<IOException, T, R> r) {
			try {
				return r.apply(t);
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				ShareData.getLogger().warning("[Conf] " + fname + ":");
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * 强制保存<br>
		 * 由配置文件实现
		 *
		 * @param u UUID
		 */
		protected abstract void save(UUID u);

		/**
		 * 保存
		 *
		 * @param u UUID
		 * @param r 运行体
		 */
		protected void save(UUID u, ThrowableRunnable<IOException> r) {
			if (!needSave.remove(u)) return;
			try {
				r.run();
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				ShareData.getLogger().warning("[Conf] " + fname + ":");
				e.printStackTrace();
			}
		}
	}

	/** 配置文件 */
	private static @Getter Configuration					config;

	/** tab替换 */
	private static @Getter @Setter String					tabReplace;

	/** 服务器信息 */
	private static byte[]									serverInfo;
	/** 出错 */
	private static @Getter boolean							errorGroup;

	/** 服务器组 */
	private static final HashMap<String, HashSet<String>>	GROUPS		= new HashMap<>();
	/** 禁用的服务器 */
	private static final HashSet<String>					BAN_SERVER	= new HashSet<>();
	/** 地标点 */
	static final HashMap<String, ShareLocation>				WARPS		= new LinkedHashMap<>();

	/** 家点 */
	static final HomesLRU									HOMES		= new HomesLRU(32);

	/** 自动保存延时 */
	private @Getter @Setter static long						saveDelay	= 1000 * 60;

	/** 是否使用AT功能 */
	private @Getter @Setter static boolean					useAt		= true;

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
		list.forEach(ConfFile::save);
		for (val v : PlayerConfFile.values()) {
			if (v.SAVE_DELAY.isEmpty()) continue;
			val l = new ArrayList<>(v.SAVE_DELAY.keySet());
			v.SAVE_DELAY.clear();
			l.forEach(v::save);
		}
	}

	/**
	 * 初始化
	 *
	 * @param config config
	 */
	public static void init(Configuration config) {
		ConfigManager.config = config;
		val tabReplace = config.getString("player-tab-replace", "yl★:" + Tool.randomString(8));
		setTabReplace(tabReplace);
		setSaveDelay(config.getLong("save-delay", getSaveDelay()));
		setUseAt(config.getBoolean("use-at", isUseAt()));
		loadGroup(config);
		serverInfo = Channel.ServerInfo.sendS(tabReplace, Main.getPluginContainer().getDescription().getVersion().orElse("Unknown"), Channel.ServerInfo.ProxyType.Velocity);

		Arrays.stream(ConfFile.values()).forEach(ConfFile::load);

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
				HashSet<String> canTp = GROUPS.computeIfAbsent(server, k -> new HashSet<>());
				canTp.addAll(group);
			}
			if (ShareData.isDEBUG()) ShareData.getLogger().info("加载组 " + key + ": " + group);
		}

		BAN_SERVER.clear();
		BAN_SERVER.addAll(config.getStringList("server-ban"));
	}

	/**
	 * 保存配置<br>
	 * 将会延时保存
	 *
	 * @param f 配置类型
	 */
	public static void saveConf(ConfFile f) {
		WaitMaintain.put(ConfFile.SAVE_DELAY, f, System.currentTimeMillis(), saveDelay, f::save);
	}

	/**
	 * 保存配置<br>
	 * 将会延时保存
	 *
	 * @param f      配置类型
	 * @param player 对应玩家
	 * @see #saveConf(PlayerConfFile, UUID)
	 */
	public static void saveConf(PlayerConfFile f, Player player) {
		saveConf(f, player.getUniqueId());
	}

	/**
	 * 保存配置<br>
	 * 将会延时保存
	 *
	 * @param f 配置类型
	 * @param u 对应玩家UUID
	 */
	public static void saveConf(PlayerConfFile f, UUID u) {
		f.needSave.add(u);
		WaitMaintain.put(f.SAVE_DELAY, u, System.currentTimeMillis(), saveDelay, () -> f.save(u));
	}

	/**
	 * 发送BC信息给子服务器
	 *
	 * @param server 服务器
	 */
	public static void sendBungeeInfoToServer(ServerConnection server) {
		Main.send(server, serverInfo);
	}
}
