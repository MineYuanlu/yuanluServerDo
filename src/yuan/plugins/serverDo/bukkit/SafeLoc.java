/**
 *
 */
package yuan.plugins.serverDo.bukkit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import yuan.plugins.serverDo.ShareData;

/**
 * 寻找安全坐标
 *
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class SafeLoc {
	/**
	 * 实现(基于{@code org.spigotmc.spigot-api 1.15.1-R0.1-SNAPSHOT}设计)<br>
	 * 基础实现
	 * 
	 * @author yuanlu
	 */
	@LimitImpl(tar = "1.7.10", type = VersionCmp.GT)
	@NoArgsConstructor
	private static class Impl extends SafeLoc {
		@Override
		public Location getSafeLocation0(@NonNull Player player, @NonNull Location location) {
			location = location.clone();
			if (isSafe(player, location, true)//
					|| searchVertical(player, location)//
					|| searchNear(player, location)//
			) return centerLoc(location);
			return location;
		}

		@Override
		boolean isSafe(Player player, Location loc, boolean checkGameMode) {
			if (checkGameMode && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) return true;
			// 脚下方块检测
			if ((!player.isFlying() && !isSafeBlock(loc, -1)) || !isSafeBlock(loc, 0) || !isSafeBlock(loc, +1)) return false;// 所在方块检测
			return true;
		}

		@Override
		boolean isSafeBlock(Location loc, int offsetY) {
			int			x		= loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
			Material	type	= loc.getWorld().getBlockAt(x, y + offsetY, z).getType();
			if (DANGERS.contains(type)) return false;// 是否是危险方块, 例如岩浆
			if (offsetY < 0) return CAN_STAND.contains(type);// 脚下的方块是否可以站立
			else if (offsetY > 0) return CAN_BREATH.contains(type);// 头上的方块是否可以呼吸(不会窒息)
			else return true;
		}

		@Override
		boolean searchNear(Player player, Location l) {
			Location tmp = l.clone();
			for (Vec3i pos : SEARCH_POS) {
				pos.set(l, tmp);
				if (isSafe(player, tmp, false)) {
					l.add(pos.x, pos.y, pos.z);
					return true;
				}
			}
			return false;
		}

		@Override
		boolean searchVertical(Player player, Location l) {
			val	Y		= l.getBlockY();
			val	world	= l.getWorld();
			for (int i = Y + 1, max = world.getHighestBlockYAt(l.getBlockX(), l.getBlockZ()) + 1; i <= max; i++) {
				l.setY(i);
				if (isSafe(player, l, false)) return true;
			}
			for (int i = Y - 1, min = 0; i > min; i++) {
				l.setY(i);
				if (isSafe(player, l, false)) return true;
			}
			l.setY(Y);
			return false;
		}
	}

	/**
	 * 1.7.10以下的特殊配置
	 * 
	 * @author yuanlu
	 *
	 */
	@LimitImpl(tar = "1.7.10", type = VersionCmp.LE)
	@NoArgsConstructor
	private static class Impl_ge1_7_10 extends Impl {
		@Override
		boolean isSafe(Player player, Location loc, boolean checkGameMode) {
			if (checkGameMode && (player.getGameMode() == GameMode.CREATIVE)) return true;
			// 脚下方块检测
			if ((!player.isFlying() && !isSafeBlock(loc, -1)) || !isSafeBlock(loc, 0) || !isSafeBlock(loc, +1)) return false;// 所在方块检测
			return true;
		}
	}

	/**
	 * 实例限制
	 * 
	 * @author yuanlu
	 *
	 */
	@Retention(RUNTIME)
	@Target(ElementType.TYPE)
	private static @interface LimitImpl {
		/** 比较版本 */
		String tar();

		/** 比较类型 */
		VersionCmp type() default VersionCmp.LE;
	}

	/**
	 * 三维坐标
	 *
	 * @author yuanlu
	 *
	 */
	@EqualsAndHashCode
	@AllArgsConstructor
	@SuppressWarnings("javadoc")
	private static final class Vec3i {
		int x, y, z;

		public int dis() {
			return x * x + y * y + z * z;
		}

		public void set(Location base, Location tar) {
			tar.setX(base.getX() + x);
			tar.setY(base.getY() + y);
			tar.setZ(base.getZ() + z);
		}

		@Override
		public String toString() {
			return String.format("(%d,%d,%d)", x, y, z);
		}
	}

	/**
	 * 版本比较
	 * 
	 * @author yuanlu
	 */
	@AllArgsConstructor
	private static enum VersionCmp {
		/** 小于等于 */
		LE("<=") {
			@Override
			protected boolean test(Matcher m1, Matcher m2) {
				boolean f1 = false, f2 = false;
				while ((f1 = m1.find()) && (f2 = m2.find())) {

					int i1 = Integer.parseInt(m1.group()), i2 = Integer.parseInt(m2.group());

					if (i1 < i2) return true;
					if (i1 > i2) return false;

				}
				return f2 || !f1;
			}
		},
		/** 小于 */
		LT("<") {
			@Override
			protected boolean test(Matcher m1, Matcher m2) {
				boolean f2 = false;
				while (m1.find() && (f2 = m2.find())) {

					int i1 = Integer.parseInt(m1.group()), i2 = Integer.parseInt(m2.group());

					if (i1 < i2) return true;
					if (i1 > i2) return false;

				}
				return f2;
			}
		},
		/** 大于等于 */
		GE(">=") {
			@Override
			protected boolean test(Matcher m1, Matcher m2) {
				boolean f1 = false, f2 = false;
				while ((f1 = m1.find()) && (f2 = m2.find())) {

					int i1 = Integer.parseInt(m1.group()), i2 = Integer.parseInt(m2.group());

					if (i1 > i2) return true;
					if (i1 < i2) return false;

				}
				return f1 || !f2;
			}
		},
		/** 大于 */
		GT(">") {
			@Override
			protected boolean test(Matcher m1, Matcher m2) {
				boolean f1 = false;
				while ((f1 = m1.find()) && m2.find()) {

					int i1 = Integer.parseInt(m1.group()), i2 = Integer.parseInt(m2.group());

					if (i1 > i2) return true;
					if (i1 < i2) return false;

				}
				return f1;
			}
		},
		/** 等于 */
		EQ("=") {
			@Override
			protected boolean test(Matcher m1, Matcher m2) {
				boolean f1 = false, f2 = false;
				while ((f1 = m1.find()) && (f2 = m2.find())) {

					int i1 = Integer.parseInt(m1.group()), i2 = Integer.parseInt(m2.group());

					if (i1 != i2) return false;

				}
				return f1 == f2;
			}
		},
		/** 不等于 */
		NE("!=") {
			@Override
			protected boolean test(Matcher m1, Matcher m2) {
				boolean f1 = false, f2 = false;
				while ((f1 = m1.find()) && (f2 = m2.find())) {

					int i1 = Integer.parseInt(m1.group()), i2 = Integer.parseInt(m2.group());

					if (i1 != i2) return true;

				}
				return f1 != f2;
			}
		};

		/** 数字正则 */
		static final Pattern	NUMBER_PATTERN	= Pattern.compile("\\d+");
		/** 当前版本 */
		static final String		VERSION			= Bukkit.getVersion();

		/**
		 * 测试某个实例是否可以使用
		 * 
		 * @param c 实现类
		 * @return 是否可以使用
		 */
		public static boolean canUse(@NonNull Class<? extends SafeLoc> c) {
			val limit = c.getAnnotation(LimitImpl.class);
			return limit != null && limit.type().test(limit.tar());
		}

		/**
		 * 返回版本名称
		 * 
		 * @param c 实现类
		 * @return 版本名称
		 */
		public static String verName(@NonNull Class<? extends SafeLoc> c) {
			val		limit	= c.getAnnotation(LimitImpl.class);
			String	name	= c.getSimpleName();
			if (limit != null) {
				name = String.format("%s ( 服务器版本 %s %s )", name, limit.type(), limit.tar());
			}
			return name;
		}

		/** 表示符号 */
		final String symbol;

		/**
		 * 实际测试
		 * 
		 * @param m1 服务器版本匹配器
		 * @param m2 目标版本匹配器
		 * @return 是否通过
		 */
		protected boolean test(Matcher m1, Matcher m2) {
			throw new InternalError();
		}

		/**
		 * 测试
		 * 
		 * @param targetVersion 目标版本
		 * @return 是否通过
		 */
		public final boolean test(@NonNull String targetVersion) {
			Matcher m1 = NUMBER_PATTERN.matcher(VERSION), m2 = NUMBER_PATTERN.matcher(targetVersion);
			return test(m1, m2);
		}

		@Override
		public String toString() {
			return symbol;
		}
	}

	/** 实现 */
	private static final SafeLoc impl;

	static {
		List<Class<? extends SafeLoc>> list = Arrays.asList(//
				Impl_ge1_7_10.class, //
				Impl.class//
		//
		);
		impl = list.stream()//
				.filter(VersionCmp::canUse)//
				.map(c -> {
					try {
						val constructor = c.getConstructor();
						constructor.setAccessible(true);
						return constructor.newInstance();
					} catch (Throwable e) {
						ShareData.getLogger().warning("无法创建SafeLoc的实例: " + c.getName());
						e.printStackTrace();
						return null;
					}
				})//
				.filter(Objects::nonNull)//
				.findFirst()//
				.orElse(null)//
		;
		if (impl == null) {
			ShareData.getLogger().warning("[SafeLoc] 未找到任何适用于当前版本的实现, 已自动关闭安全位置 (pv=" + Main.getMain().getDescription().getVersion() + ")");
		} else {
			ShareData.getLogger().warning("[SafeLoc] 已使用实现: " + VersionCmp.verName(impl.getClass()));
		}
	}

	/** 搜索位置 */
	private static Vec3i[]		SEARCH_POS;
	/** 搜索半径 */
	private static final int	SEARCH_RANGE	= 5;
	static {
		long	t1	= System.currentTimeMillis();
		val		pos	= new ArrayList<Vec3i>((int) Math.pow(SEARCH_RANGE * 2 + 1, 3));
		for (int i = -SEARCH_RANGE; i <= SEARCH_RANGE; i++)//
			for (int j = -SEARCH_RANGE; j <= SEARCH_RANGE; j++) //
				for (int k = -SEARCH_RANGE; k <= SEARCH_RANGE; k++) //
					pos.add(new Vec3i(i, j, k));
		pos.removeIf(p -> p.x == 0 && p.z == 0);
		pos.sort(Comparator.comparingInt(Vec3i::dis));
		SEARCH_POS = pos.toArray(new Vec3i[pos.size()]);
		long t2 = System.currentTimeMillis();
		if (ShareData.isDEBUG()) ShareData.getLogger()
				.info(String.format("[DEBUG] SafeLoc初始化: 安全扫描半径: %d, 构建耗时: %dms, 共计 %d 个方块被列入扫描范围 ", SEARCH_RANGE, t2 - t1, SEARCH_POS.length));
	}

	/** 所有危险的方块 */
	private static final EnumSet<Material>	DANGERS		= EnumSet.noneOf(Material.class);

	/** 可以站立的方块 */
	private static final EnumSet<Material>	CAN_STAND	= EnumSet.noneOf(Material.class);

	/** 不会窒息的方块 */
	private static final EnumSet<Material>	CAN_BREATH	= EnumSet.noneOf(Material.class);

	/**
	 * 转换为中心位置
	 *
	 * @param loc 位置
	 * @return 转换后位置
	 */
	public static Location centerLoc(Location loc) {
		loc.setX(loc.getBlockX() + 0.5);
//		loc.setY(loc.getBlockY());
		loc.setZ(loc.getBlockZ() + 0.5);
		return loc;
	}

	/**
	 * 获取安全方块
	 *
	 * @param player   玩家
	 * @param location 坐标
	 * @return 安全坐标
	 */
	public static Location getSafeLocation(@NonNull Player player, @NonNull Location location) {
		return impl == null ? location : impl.getSafeLocation0(player, location);
	}

	/**
	 * 初始化
	 *
	 * @param conf 配置文件
	 */
	public static void init(ConfigurationSection conf) {
		init(DANGERS, conf.getStringList("dangerous"));
		init(CAN_STAND, conf.getStringList("can_stand"));
		init(CAN_BREATH, conf.getStringList("can_breath"));
	}

	/**
	 * 从物品列表加载数据
	 *
	 * @param set  集合
	 * @param list 列表
	 */
	private static void init(Set<Material> set, List<String> list) {
		list.stream()//
				.filter(s -> !s.startsWith("!!"))//
				.map(Material::matchMaterial)//
				.filter(x -> x != null)//
				.forEach(set::add);
		list.stream()//
				.filter(s -> s.startsWith("!!"))//
				.flatMap(SafeLoc::reflect)//
				.filter(x -> x != null)//
				.forEach(set::add);
	}

	/**
	 * 反射获取Material中的判断方法
	 *
	 * @param type 方法
	 * @return 流
	 */
	private static Stream<Material> reflect(String type) {
		if (type.startsWith("!!is")) type = type.substring(4);
		else if (type.startsWith("!!")) type = type.substring(2);
		else if (type.startsWith("is")) type = type.substring(2);
		val typeFinal = type;
		try {
			val method = Material.class.getDeclaredMethod("is" + typeFinal);
			method.setAccessible(true);
			if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) {
				ShareData.getLogger().warning(String.format("[SAFE] 无法获取 %s 方块集合: 内部判断方法返回有误", typeFinal));
				return Stream.empty();
			}
			AtomicBoolean hasThrow = new AtomicBoolean(false);
			return Arrays.stream(Material.values())//
					.filter(material -> {
						try {
							return (boolean) method.invoke(material);
						} catch (Throwable e) {
							if (!hasThrow.get()) {
								ShareData.getLogger().warning(String.format("[SAFE] 无法获取 %s 方块集合: 判断时出错", typeFinal));
								e.printStackTrace();
							}
							return false;
						}
					});
		} catch (NoSuchMethodException e) {
			ShareData.getLogger().warning(String.format("[SAFE] 无法获取 %s 方块集合: 找不到判断方法(低版本/不存在)", typeFinal));
		} catch (Throwable e) {
			ShareData.getLogger().warning(String.format("[SAFE] 无法获取 %s 方块集合: 未知错误", typeFinal));
			e.printStackTrace();
		}
		return Stream.empty();
	}

	/**
	 * 获取安全方块
	 *
	 * @param player   玩家
	 * @param location 坐标
	 * @return 安全坐标
	 */
	public abstract Location getSafeLocation0(@NonNull Player player, @NonNull Location location);

	/**
	 * 检测玩家目前位置是否安全
	 *
	 * @param player        玩家
	 * @param loc           检测坐标
	 * @param checkGameMode 是否检查游戏模式
	 * @return 是否安全
	 */
	abstract boolean isSafe(Player player, Location loc, boolean checkGameMode);

	/**
	 * 检查方块是否安全
	 *
	 * @param loc     位置
	 * @param offsetY 偏移量
	 * @return 是否安全
	 */
	abstract boolean isSafeBlock(Location loc, int offsetY);

	/**
	 * 搜索方法
	 *
	 * @param player 玩家
	 * @param l      坐标
	 * @return 是否是安全的坐标
	 */
	abstract boolean searchNear(Player player, Location l);

	/**
	 * 搜索方法
	 *
	 * @param player 玩家
	 * @param l      坐标
	 * @return 是否是安全的坐标
	 */
	abstract boolean searchVertical(Player player, Location l);
}
