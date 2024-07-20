/**
 *
 */
package yuan.plugins.serverDo.bukkit.third;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import yuan.plugins.serverDo.Channel;
import yuan.plugins.serverDo.Channel.Package.BiIntConsumer;
import yuan.plugins.serverDo.ShareData;
import yuan.plugins.serverDo.bukkit.Core;
import yuan.plugins.serverDo.bukkit.Main;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.IntConsumer;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 第三方数据
 *
 * @author yuanlu
 */
@Getter
public abstract class Third {
	/** 第三方数据 */
	private static final         HashMap<String, Third>        THIRDS           = new HashMap<>();
	/** 第三方数据的视图 */
	public static final @NonNull Set<String>                   THIRDS_VIEW      = Collections.unmodifiableSet(THIRDS.keySet());
	/** 所有的转换方法(处理函数) */
	private static final         EnumMap<TransMethods, Method> TRANS_FUNCS_CALL = new EnumMap<>(TransMethods.class);
	/** 所有的转换方法(实现函数) */
	private static final         ArrayList<Method>             TRANS_FUNCS      = new ArrayList<>();

	static {
		try {
			val ms = Third.class.getDeclaredMethods();
			for (val m : ms) {
				val a = m.getDeclaredAnnotation(TransFunc.class);
				if (a != null) {
					TRANS_FUNCS.add(m);
					TRANS_FUNCS_CALL.put(a.value(), Third.class.getDeclaredMethod(m.getName(), Player.class, BiIntConsumer.class));
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		//		registerThird(CMI.INSTANCE);TODO
	}

	/** 目标插件名 */
	public final @NonNull String            targetPluginName;
	/** 别名 */
	public final @NonNull String            name;
	/** 所有可以完成的功能 */
	private final         Set<TransMethods> canDos;

	/**
	 * 构造
	 *
	 * @param targetPluginName 目标插件名
	 * @param name             别名
	 */
	public Third(@NonNull String targetPluginName, @NonNull String name) {
		this.targetPluginName = targetPluginName;
		this.name = name;
		this.canDos = Collections.unmodifiableSet(canDo(this.getClass()));
	}

	/**
	 * 获取某个转换器可以完成的功能
	 *
	 * @param c 转换器的类
	 *
	 * @return 可以完成的功能
	 */
	private static EnumSet<TransMethods> canDo(Class<? extends Third> c) {
		EnumSet<TransMethods> methods = EnumSet.noneOf(TransMethods.class);
		for (val m : TRANS_FUNCS) {
			try {
				val cm = c.getMethod(m.getName(), m.getParameterTypes());
				if (cm.getDeclaringClass() != Third.class) methods.add(m.getAnnotation(TransFunc.class).value());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return methods;
	}

	/**
	 * 获取处理器
	 *
	 * @param third 名称
	 *
	 * @return 处理器/null
	 */
	public static Third get(@NonNull String third) {
		return THIRDS.get(third.toLowerCase());
	}

	/** @param third 第三方插件的处理器 */
	public static final void registerThird(Third third) {
		THIRDS.put(third.getName().toLowerCase(), third);
	}

	/**
	 * 检测此转换器是否能够完成某个功能
	 *
	 * @param m 功能
	 *
	 * @return 是否能完成
	 */
	public boolean canDo(TransMethods m) {
		return canDos.contains(m);
	}

	/**
	 * 获取所有玩家的Home
	 *
	 * @return 玩家UUID-家名-家坐标
	 */
	@TransFunc(TransMethods.ALL_HOME)
	@NonNull
	public HashMap<UUID, LinkedHashMap<String, Location>> getAllHomes() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param p       玩家
	 * @param process 过程
	 *
	 * @see #getAllHomes()
	 */
	public final void getAllHomes(Player p, BiIntConsumer process) {
		@NonNull val data = getAllHomes();
		int all = 0, now = 0;
		for (val homes : data.values()) all += homes.size();
		if (all > 0) for (val e : data.entrySet()) {
			val uuid = e.getKey();
			val homes = e.getValue();
			if (!homes.isEmpty()) for (val he : homes.entrySet()) {
				val name = he.getKey();
				val home = he.getValue();
				Main.send(p, Channel.TransHome.sendS(uuid, name, Core.toSLoc(home)));
				process.accept(now++, all);
				if (ShareData.isDEBUG()) ShareData.getLogger().info("[TRANS] " + uuid + " " + name + " " + home);
			}
		}

		val ALL = -all;
		Core.listenCallBack(p, Channel.TRANS_HOME, null, (IntConsumer) amount -> {
			process.accept(amount, ALL);
		});
		Main.send(p, Channel.TransHome.sendS());
	}

	/**
	 * 获取所有的Warp
	 *
	 * @return 地标名-地标坐标
	 */
	@TransFunc(TransMethods.ALL_WARP)
	@NonNull
	public LinkedHashMap<String, Location> getAllWarps() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param p       玩家
	 * @param process 过程
	 *
	 * @see #getAllWarps()
	 */
	public final void getAllWarps(Player p, BiIntConsumer process) {
		@NonNull val data = getAllWarps();
		val all = data.size();
		int now = 0;
		for (val warp : data.entrySet()) {
			val name = warp.getKey();
			val loc = warp.getValue();
			Main.send(p, Channel.TransWarp.sendS(name, Core.toSLoc(loc)));
			process.accept(now++, all);
			if (ShareData.isDEBUG()) ShareData.getLogger().info("[TRANS] " + name + " " + loc);
		}

		Core.listenCallBack(p, Channel.TRANS_WARP, null, (IntConsumer) amount -> {
			process.accept(amount, -all);
		});
		Main.send(p, Channel.TransWarp.sendS());
	}

	/** @return 此第三方数据是否有效 */
	public boolean isValid() {
		return Bukkit.getPluginManager().getPlugin(targetPluginName) != null;
	}

	/**
	 * 所有的转换方法
	 *
	 * @author yuanlu
	 */
	@AllArgsConstructor
	@Getter
	public enum TransMethods {
		/** @see Third#getAllHomes() */
		ALL_HOME("all-home"),
		/** @see Third#getAllWarps() */
		ALL_WARP("warps");

		/** 集合 */
		private static final HashMap<String, TransMethods> VS = new HashMap<>();

		static {
			for (val v : values()) VS.put(v.getName().toLowerCase(), v);
		}

		/** 名称 */
		private final String name;

		/**
		 * 通过名字获取
		 *
		 * @param name 名字
		 *
		 * @return 方法
		 */
		public static final TransMethods getByName(String name) {
			if (name == null) return null;
			return VS.get(name.toLowerCase());
		}

		/**
		 * 执行此项操作
		 *
		 * @param t       处理器
		 * @param p       玩家
		 * @param process 过程回调
		 */
		public void handle(Third t, Player p, BiIntConsumer process) {
			val m = TRANS_FUNCS_CALL.get(this);
			try {
				m.invoke(t, p, process);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 转换方法
	 *
	 * @author yuanlu
	 */
	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface TransFunc {
		/** @return 对应的转换方法 */
		TransMethods value();
	}
}
