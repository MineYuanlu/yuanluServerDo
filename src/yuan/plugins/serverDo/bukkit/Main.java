package yuan.plugins.serverDo.bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import lombok.val;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.bukkit.MESSAGE.Msg;
import yuan.plugins.serverDo.bukkit.cmds.CommandManager;

/**
 * 主类
 * 
 * @author yuanlu
 *
 */
public class Main extends JavaPlugin implements Listener {

	/** 语言文件丢失时显示的信息 模板自动生成 */
	public static final String		LANG_LOST	= "§c§l[语言文件缺失]§4§l(完全损坏)§c§l节点:%node%";

	/** 语言文件丢失时显示的信息 模板自动生成 */
	private static String			langLost;

	/** 插件前缀 模板自动生成 */
	private static String			prefix		= "";

	/** 插件主体 */
	private static @Getter Main		main;

	/** 调试模式 */
	private static @Getter boolean	DEBUG;
	/** 强制替换文件 */
	private static @Getter boolean	FORECE_OUT_FILE;

	/**
	 * 向玩家(BC端)发送数据
	 * 
	 * @param player 玩家
	 * @param data   数据
	 */
	public static void send(Player player, byte[] data) {
		if (isDEBUG()) getMain().getLogger().info("发送: " + player.getName() + " " + Arrays.toString(data));
		player.sendPluginMessage(getMain(), ShareData.BC_CHANNEL, data);
	}

	/**
	 * 翻译字符串<br>
	 * 将文字中的彩色字符串进行翻译
	 * 
	 * @param s 字符串
	 * @return 翻译后的字符串
	 */
	public static final String t(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	/** 语言缺失的节点 */
	private FileConfiguration			MESSAGE_LOST_NODE;

	/** 插件配置文件 */
	private @Getter FileConfiguration	config;

	/**
	 * bstats数据收集<br>
	 * 在{@link #onEnable()}调用
	 */
	private void bstats() {

		// 注册bstats
		Metrics metrics = new Metrics(this);
		metrics.addCustomChart(new Metrics.SimplePie("pls_count", () -> {
			int count = 0;
			for (Plugin pl : getServer().getPluginManager().getPlugins()) {
				if (pl.getName().startsWith("yuanlu")) count++;
			}
			return Integer.toString(count);
		}));

	}

	/** 检查中央配置文件 */
	private void checkYuanluConfig() {
		val	yuanluFolder	= new File(getDataFolder().getParentFile(), "yuanlu");
		val	configFile		= new File(yuanluFolder, "config.yml");
		if (!configFile.exists()) {
			DEBUG			= false;
			FORECE_OUT_FILE	= false;
		} else {
			YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
			DEBUG			= config.getBoolean("debug", false);
			FORECE_OUT_FILE	= config.getBoolean("force-override-file", false);
		}
	}

	/**
	 * 从配置文件获取信息<br>
	 * 用于tab列表或lore说明<br>
	 * 注意: 返回的list均为副本<br>
	 * 容量为list的大小, 若长期保存, 建议在添加新元素(如果有)后进行长度剪裁({@link ArrayList#trimToSize()})
	 *
	 * @param node 节点
	 * @return 获取到的字符串
	 */
	public ArrayList<String> list(String node) {
		node = "message." + node;
		if (config.isList(node)) {
			List<String>		l	= config.getStringList(node);
			ArrayList<String>	r	= new ArrayList<>(l.size());
			l.forEach(x -> r.add(t(x)));
			return r;
		} else if (config.isString(node)) {
			String				message	= config.getString(node);
			List<String>		l		= Arrays.asList(message.split("\n"));
			ArrayList<String>	r		= new ArrayList<>(l.size());
			l.forEach(x -> r.add(t(x)));
			return r;
		} else {
			getLogger().warning("§d[LMES] §c§lcan not find list in config: " + node);
			return new ArrayList<>(Arrays.asList(langLost.replace("%node%", node)));
		}
	}

	/**
	 * 加载配置
	 * 
	 * @param fileName 配置文件名，例如{@code "config.yml"}
	 * @return 配置文件
	 */
	public YamlConfiguration loadFile(String fileName) {
		File file = new File(getDataFolder(), fileName);
		if (FORECE_OUT_FILE || !file.exists()) try {
			saveResource(fileName, FORECE_OUT_FILE);
		} catch (IllegalArgumentException e) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			return YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param node 节点
	 * @return 获取到的字符串
	 * @see #mes(String, int)
	 */
	public Msg mes(String node) {
		return mes(node, 0);
	}

	/**
	 * 从配置文件获取信息
	 * <p>
	 * type:<br>
	 * <table border="1">
	 * <tr>
	 * <td>类型</td>
	 * <td>十进制</td>
	 * <td>二进制</td>
	 * </tr>
	 * <tr>
	 * <td>没有前缀</td>
	 * <td>1</td>
	 * <td>1</td>
	 * </tr>
	 * <tr>
	 * <td>检查空字符串</td>
	 * <td>2</td>
	 * <td>10</td>
	 * </tr>
	 * <tr>
	 * <td><b>禁用</b>json模式</td>
	 * <td>4</td>
	 * <td>100</td>
	 * </tr>
	 * </table>
	 *
	 * @param node 节点
	 * @param type 类型
	 * @return 获取到的字符串
	 */
	public Msg mes(String node, int type) {
		val real = node;
		node = "message." + node;
		boolean	nop			= (type & 1) > 0;
		boolean	checkEmpty	= (type & 2) > 0;
		boolean	notSenior	= (type & 4) > 0;
		if (config.isConfigurationSection(node)) {
			val msg = mes(real + ".msg", type | 4);
			if (notSenior) return msg;
			else {
				String json = config.getString(node + ".json", null);
				if (json != null) return new MESSAGE.JsonMsg(t(json), msg.getMsg());
			}
		} else if (config.isList(node)) {
			List<String>		l	= config.getStringList(node);
			final StringBuilder	sb	= new StringBuilder(32);
			l.forEach((x) -> {
				if (!nop) sb.append(prefix);
				sb.append(x).append('\n');
			});
			if (sb.length() > 0) sb.setLength(sb.length() - 1);

			return sb.length() < 1 ? (checkEmpty ? null : MESSAGE.EmptyMsg.INSTANCE) : new MESSAGE.StrMsg(t(sb.toString()));
		} else if (config.isString(node)) {
			String message = config.getString(node);
			return message.isEmpty() ? (checkEmpty ? null : MESSAGE.EmptyMsg.INSTANCE) : new MESSAGE.StrMsg(t(nop ? message : (prefix + message)));
		}
		MESSAGE_LOST_NODE.set(node, node);
		getLogger().warning("§d[LMES] §c§lcan not find message in config: " + node);
		if (nop) return new MESSAGE.StrMsg(t(langLost.replace("%node%", node)));
		return new MESSAGE.StrMsg(t(prefix + langLost.replace("%node%", node)));
	}

	@Override
	public void onDisable() {
		// 关闭插件时自动发出
		getLogger().info("§a" + ShareData.SHOW_NAME + "-关闭");
		if (ShareData.isDEBUG()) saveFile(MESSAGE_LOST_NODE, "lang-lost.yml");
	}

	@Override
	public void onEnable() {
		ShareData.setLogger(getLogger());
		checkYuanluConfig();
		ShareData.setDEBUG(DEBUG);
		bstats();

		if (ShareData.isDEBUG()) {
			MESSAGE_LOST_NODE = loadFile("lang-lost.yml");
			MESSAGE_LOST_NODE.set("message", loadFile("config.yml").getConfigurationSection("message"));
		}

		// 启用插件时自动发出
		main = this;
		getLogger().info("§a" + ShareData.SHOW_NAME + "-启动");
		config		= loadFile("config.yml");
		prefix		= config.getString("Prefix", "");
		langLost	= config.getString("message.LanguageFileIsLost", LANG_LOST);
		getServer().getPluginManager().registerEvents(Core.INSTANCE, this); // 注册监听器
		CommandManager.init(config.getConfigurationSection("cmd"));
		getServer().getMessenger().registerOutgoingPluginChannel(this, ShareData.BC_CHANNEL);
		getServer().getMessenger().registerIncomingPluginChannel(this, ShareData.BC_CHANNEL, Core.INSTANCE);

	}

	/**
	 * 保存配置
	 * 
	 * @param c        配置文件
	 * @param fileName 保存名称
	 * @author yuanlu
	 */
	public void saveFile(FileConfiguration c, String fileName) {
		val f = new File(getDataFolder(), fileName);
		try (val o = new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8"))) {
			o.write(c.saveToString());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
