/**
 * 
 */
package yuan.plugins.serverDo.bukkit.third;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import lombok.val;
import yuan.plugins.serverDo.Tool;

/**
 * @author yuanlu
 *
 */
public class CMI extends Third {
	/** INSTANCE */
	public static final CMI INSTANCE = new CMI();

	/***/
	public CMI() {
		super("CMI", "CMI");
	}

	/**
	 * 通过反射获取所有玩家的Home
	 * 
	 * @return 玩家UUID-家名-家坐标
	 */
	@Override
	public HashMap<UUID, LinkedHashMap<String, Location>> getAllHomes() {
		try {
			val	plugin	= (com.Zrips.CMI.CMI) Bukkit.getPluginManager().getPlugin("CMI");
			val	users	= plugin.getPlayerManager().getAllUsers();
			//

			HashMap<UUID, LinkedHashMap<String, Location>> datas = new HashMap<>();
			for (val e : users.entrySet()) {
				val	uuid	= e.getKey();
				val	user	= e.getValue();
				val	homes	= user.getHomes();
				//

				LinkedHashMap<String, Location> newHomes = new LinkedHashMap<>();
				for (val home : homes.values()) {
					newHomes.put(home.getName(), home.getLoc().getBukkitLoc());
				}
				datas.put(uuid, newHomes);
			}
			return datas;
		} catch (Throwable e) {
			throw new RuntimeException("无法获取CMI数据, 可能由于版本更新引起数据变动, 请向作者反馈, 谢谢!", e);
		}
	}

	@Override
	public LinkedHashMap<String, Location> getAllWarps() {
		try {
			val								plugin	= (com.Zrips.CMI.CMI) Bukkit.getPluginManager().getPlugin("CMI");
			val								datas	= plugin.getWarpManager().getWarps();
			LinkedHashMap<String, Location>	warps	= new LinkedHashMap<>();
			Tool.translate(datas, (o, n) -> {
				n.setKey(o.getKey());
				n.setValue(o.getValue().getLoc());
			}, warps);
			return warps;
		} catch (Throwable e) {
			throw new RuntimeException("无法获取CMI数据, 可能由于版本更新引起数据变动, 请向作者反馈, 谢谢!", e);
		}
	}
}
