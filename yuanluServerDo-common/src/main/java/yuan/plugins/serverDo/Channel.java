/**
 * yuanlu
 * date: 2021年8月9日
 * file: Channel.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;

/**
 * 通道数据<br>
 * 包含了服务器之间数据交互所用到的所有数据包<br>
 * 枚举定义了数据包类型, 其指向的实现类有此数据包下所有方法.
 *
 * @author yuanlu
 */
public enum Channel {
	/** 版本检查 */
	VERSION_CHECK(VersionCheck.class),
	/** 权限检查 */
	PERMISSION(Permission.class),
	/** 传送 */
	TP(Tp.class),
	/** 冷却广播 */
	COOLDOWN(Cooldown.class),
	/** 时间修正 */
	TIME_AMEND(TimeAmend.class),
	/** 返回服务器信息 */
	SERVER_INFO(ServerInfo.class),
	/** 隐身模式 */
	VANISH(Vanish.class),
	/** 传送地标 */
	WARP(Warp.class),
	/** 传送家 */
	HOME(Home.class),
	/** 传送坐标 */
	TP_LOC(TpLoc.class),
	/** 转换家 */
	TRANS_HOME(TransHome.class),
	/** 转换地标 */
	TRANS_WARP(TransWarp.class),
	/** 返回 */
	BACK(Back.class),
	/** 播放音效 */
	PLAY_SOUND(PlaySound.class);

	/** 数据包计数 */
	public static final  EnumMap<Channel, AtomicInteger> PACK_COUNT = new EnumMap<>(Channel.class);
	/** 版本数据 */
	private static final byte[]                          VERSION;
	/** 所有数据包 */
	private static final Channel[]                       CHANNELS   = values();

	static {
		try {
			val sb = new StringBuilder();
			for (val x : values()) sb.append(x.name()).append(';');
			val md5 = MessageDigest.getInstance("MD5");
			md5.update(sb.toString().getBytes(ShareData.CHARSET));
			VERSION = md5.digest();
		} catch (Exception e) {
			throw new InternalError(e);
		}
		for (val x : values()) PACK_COUNT.put(x, new AtomicInteger());
	}

	/** 目标类 */
	public final Class<? extends Package> target;

	/**
	 * 构造
	 *
	 * @param target 目标类
	 */
	Channel(Class<? extends Package> target) {
		this.target = target;
		try {
			target.getDeclaredField("ID").setInt(target, ordinal());
		} catch (Exception e) {
			throw new InternalError(e);
		}
	}

	/**
	 * 解析数据包
	 *
	 * @param id 数据包ID
	 *
	 * @return 数据包类型
	 */
	public static Channel byId(int id) {
		return id >= 0 && id < CHANNELS.length ? CHANNELS[id] : null;
	}

	/** @return 数据包计数 */
	public static @NonNull HashMap<String, Integer> getPackCount() {
		HashMap<String, Integer> m = new HashMap<>();
		PACK_COUNT.forEach((k, v) -> m.put(k.name(), v.getAndSet(0)));
		return m;
	}

	/**
	 * 解析数据包子ID
	 *
	 * @param message 数据包
	 *
	 * @return 数据包子ID
	 */
	public static byte getSubId(byte[] message) {
		return message[4];
	}

	/**
	 * 返回数据包
	 *
	 * @author yuanlu
	 */
	@Value
	@EqualsAndHashCode(callSuper = false)
	public static final class Back extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;
		/** 玩家 */
		String        player;
		/** back地址 */
		ShareLocation loc;
		/** 目标是否是服务器(false时to为玩家, 需要解析为server) */
		boolean       isServer;
		/** 目标服务器/玩家 */
		String        to;

		/**
		 * 解析:传送坐标
		 *
		 * @param buf  数据
		 * @param back 数据
		 *
		 * @see #s0C_tellTp(String, ShareLocation, boolean, String)
		 */
		public static void p0C_tellTp(byte[] buf, Consumer<Back> back) {
			try (val in = DataIn.pool(buf, 0)) {
				back.accept(new Back(in.readUTF(), in.readLocation(), in.readBoolean(), in.readUTF()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析:传送坐标
		 *
		 * @param buf              数据
		 * @param playerAndBackLoc 玩家, 传送前坐标
		 *
		 * @see #s1S_tellTp(String, ShareLocation, String)
		 */
		public static void p1S_tellTp(byte[] buf, BiConsumer<String, ShareLocation> playerAndBackLoc) {
			try (val in = DataIn.pool(buf, 1)) {
				val player = in.readUTF();
				val loc = in.readLocation();
				loc.setServer(in.readUTF());
				playerAndBackLoc.accept(player, loc);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送坐标
		 *
		 * @param player   被传送的玩家
		 * @param backLoc  要传送的位置
		 * @param isServer 目标是否是服务器(如果为false将被从玩家解析为服务器再继续传递)
		 * @param to       目标服务器/玩家
		 *
		 * @return 数据包
		 */
		public static byte[] s0C_tellTp(String player, ShareLocation backLoc, boolean isServer, String to) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeUTF(player);
				out.writeLocation(backLoc);
				out.writeBoolean(isServer);
				out.writeUTF(to);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送坐标
		 *
		 * @param player     被传送的玩家
		 * @param backLoc    要传送的位置
		 * @param fromServer 来源服务器
		 *
		 * @return 数据包
		 */
		public static byte[] s1S_tellTp(String player, ShareLocation backLoc, String fromServer) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeUTF(player);
				out.writeLocation(backLoc);
				out.writeUTF(fromServer);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 传送冷却数据包
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Cooldown extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 发送至Client
		 *
		 * @param player 玩家
		 * @param end    冷却结束时间
		 *
		 * @return 数据包
		 */
		public static byte[] broadcast(UUID player, long end) {
			try (val out = DataOut.pool(ID)) {
				out.writeUUID(player);
				out.writeLong(end);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析Client
		 *
		 * @param buf 数据
		 *
		 * @return 冷却结束时间
		 */
		public static long parseC(byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readLong();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析Client
		 *
		 * @param buf 数据
		 * @param map 冷却数据映射
		 */
		public static void parseS(byte[] buf, Map<UUID, Long> map) {
			try (val in = DataIn.pool(buf)) {
				val player = in.readUUID();
				val end = in.readLong();
				WaitMaintain.put(map, player, end, end - System.currentTimeMillis());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @param end 冷却结束时间
		 *
		 * @return 数据包
		 */
		public static byte[] sendS(long end) {
			try (val out = DataOut.pool(ID)) {
				out.writeLong(end);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/** 数据输入 */
	private static final class DataIn extends DataInputStream {
		/** 缓存池 */
		private static final DataIn[] POOL = new DataIn[16];
		/** 缓存池有效容量 */
		private static       int      pool = 0;
		/** 此输入器是否已经释放 */
		private              boolean  release;

		/**
		 * 构造并初始化
		 *
		 * @param buf 初始化数据流
		 */
		private DataIn(byte[] buf) {
			super(new ByteIn(buf));
			release = false;
		}

		/**
		 * 从对象池中取出一个数据输入器并初始化
		 *
		 * @param buf 数据
		 *
		 * @return 输入器
		 */
		public static DataIn pool(byte @NonNull [] buf) {
			synchronized (POOL) {
				if (pool > 0) return POOL[--pool].reset(buf);
			}
			return new DataIn(buf);
		}

		/**
		 * 从对象池中取出一个数据输入器并初始化, 同时检查接收到的数据包子ID
		 *
		 * @param buf 数据
		 * @param id  子包ID
		 *
		 * @return 数据流
		 *
		 * @throws IOException IOE
		 */
		private static DataIn pool(byte @NonNull [] buf, int id) throws IOException {
			val in = pool(buf);
			val packageId = in.readByte();
			if (packageId != id) throw new IllegalArgumentException("Bad Package Id: " + packageId + ", expect: " + id);
			return in;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (release) return;
			synchronized (POOL) {
				if (release) return;
				((ByteIn) super.in).clear();
				if (pool < POOL.length) POOL[pool++] = this;
				release = true;
			}
		}

		/**
		 * 读取一个Location
		 *
		 * @return Location
		 *
		 * @throws IOException IOE
		 */
		public ShareLocation readLocation() throws IOException {
			return new ShareLocation(readDouble(), readDouble(), readDouble(), readFloat(), readFloat(), readUTF());
		}

		/**
		 * 读取一个UUID
		 *
		 * @return UUID
		 *
		 * @throws IOException IOE
		 */
		public UUID readUUID() throws IOException {
			return new UUID(readLong(), readLong());
		}

		/**
		 * 初始化
		 *
		 * @param buf 初始化数据流
		 *
		 * @return this
		 */
		private DataIn reset(byte[] buf) {
			((ByteIn) super.in).reset(buf);
			release = false;
			return this;
		}

		/** 字节输入 */
		private static final class ByteIn extends ByteArrayInputStream {
			/** 数据包偏移量 */
			private static final int offsetByPackageID = 4;

			/**
			 * 构造并初始化
			 *
			 * @param buf 初始化数据流
			 */
			public ByteIn(byte[] buf) {
				super(buf, offsetByPackageID, buf.length);
			}

			/** 清除 */
			public void clear() {
				super.buf = null;
			}

			/**
			 * 初始化
			 *
			 * @param buf 初始化数据流
			 */
			public void reset(byte[] buf) {
				super.buf = buf;
				this.pos = this.mark = offsetByPackageID;
				this.count = buf.length;
			}

		}
	}

	/** 数据输出 */
	private static final class DataOut extends DataOutputStream {

		/** 缓存池 */
		private static final DataOut[] POOL = new DataOut[128];
		/** 缓存池有效容量 */
		private static       int       pool = 0;
		/** 此输入器是否已经释放 */
		private              boolean   release;

		/**
		 * 构造并初始化
		 *
		 * @param ID 包ID
		 *
		 * @throws IOException IO错误
		 */
		public DataOut(int ID) throws IOException {
			super(new ByteArrayOutputStream());
			release = false;
			writeInt(ID);
		}

		/**
		 * 从对象池中取出一个数据输出器并初始化
		 *
		 * @param ID 包ID
		 *
		 * @return 输出器
		 *
		 * @throws IOException IO错误
		 */
		public static DataOut pool(int ID) throws IOException {
			synchronized (POOL) {
				if (pool > 0) return POOL[--pool].reset(ID);
			}
			return new DataOut(ID);
		}

		/**
		 * 从对象池中取出一个数据输出器并初始化
		 *
		 * @param ID 包ID
		 * @param id 子包ID
		 *
		 * @return 输出器
		 *
		 * @throws IOException IO错误
		 */
		private static DataOut pool(int ID, int id) throws IOException {
			val out = DataOut.pool(ID);
			out.writeByte(id);
			return out;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (release) return;
			synchronized (POOL) {
				if (release) return;
				if (pool < POOL.length) POOL[pool++] = this;
			}
		}

		/**
		 * 释放输出器
		 *
		 * @return 数据
		 */
		public byte[] getByte() {
			return ((ByteArrayOutputStream) super.out).toByteArray();
		}

		/**
		 * 初始化
		 *
		 * @param ID 包ID
		 *
		 * @return this
		 *
		 * @throws IOException IO错误
		 */
		private DataOut reset(int ID) throws IOException {
			release = false;
			((ByteArrayOutputStream) super.out).reset();
			writeInt(ID);
			return this;
		}

		/**
		 * 写入Location
		 *
		 * @param l location
		 *
		 * @throws IOException IOE
		 */
		public void writeLocation(ShareLocation l) throws IOException {
			writeDouble(l.getX());
			writeDouble(l.getY());
			writeDouble(l.getZ());
			writeFloat(l.getYaw());
			writeFloat(l.getPitch());
			writeUTF(l.getWorld());
		}

		/**
		 * 写入UUID
		 *
		 * @param u uuid
		 *
		 * @throws IOException IOE
		 */
		public void writeUUID(UUID u) throws IOException {
			writeLong(u.getMostSignificantBits());
			writeLong(u.getLeastSignificantBits());
		}

	}

	/**
	 * 家数据包
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Home extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析: 设置家
		 *
		 * @param buf                 数据
		 * @param nameAndLocAndAmount 家名称,家,最大数量
		 *
		 * @see #s0C_setHome(String, ShareLocation, int)
		 */
		public static void p0C_setHome(byte @NonNull [] buf, BiObjIntConsumer<String, ShareLocation> nameAndLocAndAmount) {
			try (val in = DataIn.pool(buf, 0)) {
				nameAndLocAndAmount.accept(in.readUTF(), in.readLocation(), in.readInt());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 设置家响应
		 *
		 * @param buf     数据
		 * @param success 成功
		 *
		 * @see #s0S_setHomeResp(boolean)
		 */
		public static void p0S_setHomeResp(byte @NonNull [] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 0)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 删除家
		 *
		 * @param buf  数据
		 * @param name 家名称
		 *
		 * @see #s1C_delHome(String)
		 */
		public static void p1C_delHome(byte @NonNull [] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 1)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 删除家响应
		 *
		 * @param buf     数据
		 * @param success 删除成功
		 *
		 * @see #s1S_delHomeResp(boolean)
		 */
		public static void p1S_delHomeResp(byte @NonNull [] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 1)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 搜索家
		 *
		 * @param buf  数据
		 * @param name 家名称
		 *
		 * @see #s2C_searchHome(String)
		 */
		public static void p2C_searchHome(byte @NonNull [] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 2)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 搜索家响应
		 *
		 * @param buf           数据
		 * @param nameAndServer 家名称,服务器名称
		 *
		 * @see #s2S_searchHomeResp(String, String)
		 */
		public static void p2S_searchHomeResp(byte @NonNull [] buf, BiConsumer<String, String> nameAndServer) {
			try (val in = DataIn.pool(buf, 2)) {
				nameAndServer.accept(in.readUTF(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送家
		 *
		 * @param buf  数据
		 * @param name 家名称
		 *
		 * @see #s3C_tpHome(String)
		 */
		public static void p3C_tpHome(byte @NonNull [] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 3)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送家响应
		 *
		 * @param buf     数据
		 * @param success 是否成功
		 *
		 * @see #s3S_tpHomeResp(boolean)
		 */
		public static void p3S_tpHomeResp(byte @NonNull [] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 3)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送家列表
		 *
		 * @param buf 数据
		 * @param r   next
		 *
		 * @see #s4C_listHome()
		 */
		public static void p4C_listHome(byte @NonNull [] buf, Runnable r) {
			try (val ignored = DataIn.pool(buf, 4)) {
				r.run();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送家列表响应
		 *
		 * @param buf   数据
		 * @param homes 两组家列表
		 *
		 * @see #s4S_listHomeResp(Collection, Collection)
		 */
		public static void p4S_listHomeResp(byte[] buf, BiConsumer<Collection<String>, Collection<String>> homes) {
			try (val in = DataIn.pool(buf, 4)) {
				Collection<String> tar1 = new LinkedHashSet<>();
				for (int i = 0, j = in.readInt(); i < j; i++) tar1.add(in.readUTF());
				Collection<String> tar2 = new LinkedHashSet<>();
				for (int i = 0, j = in.readInt(); i < j; i++) tar2.add(in.readUTF());
				homes.accept(tar1, tar2);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		//

		/**
		 * 设置家
		 *
		 * @param name   家名称
		 * @param loc    家
		 * @param amount 最大数量
		 *
		 * @return 数据包
		 */
		public static byte[] s0C_setHome(String name, ShareLocation loc, int amount) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeUTF(name);
				out.writeLocation(loc);
				out.writeInt(amount);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 设置家响应
		 *
		 * @param success 成功
		 *
		 * @return 数据包
		 */
		public static byte[] s0S_setHomeResp(boolean success) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 删除家
		 *
		 * @param name 家名称
		 *
		 * @return 数据包
		 */
		public static byte[] s1C_delHome(String name) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 删除家响应
		 *
		 * @param success 删除成功
		 *
		 * @return 数据包
		 */
		public static byte[] s1S_delHomeResp(boolean success) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 搜索家
		 *
		 * @param name 家名称
		 *
		 * @return 数据包
		 */
		public static byte[] s2C_searchHome(String name) {
			try (val out = DataOut.pool(ID, 2)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 搜索家响应
		 *
		 * @param name   家名称
		 * @param server 所在服务器
		 *
		 * @return 数据包
		 */
		public static byte[] s2S_searchHomeResp(String name, String server) {
			try (val out = DataOut.pool(ID, 2)) {
				out.writeUTF(name);
				out.writeUTF(server);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送家
		 *
		 * @param name 家名称
		 *
		 * @return 数据包
		 */
		public static byte[] s3C_tpHome(String name) {
			try (val out = DataOut.pool(ID, 3)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送家响应
		 *
		 * @param success 是否成功
		 *
		 * @return 数据包
		 */
		public static byte[] s3S_tpHomeResp(boolean success) {
			try (val out = DataOut.pool(ID, 3)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送家列表
		 *
		 * @return 数据包
		 */
		public static byte[] s4C_listHome() {
			try (val out = DataOut.pool(ID, 4)) {
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送家列表响应
		 *
		 * @param homes           同一服务器组的家列表
		 * @param otherGroupHomes 其他组的家列表
		 *
		 * @return 数据包
		 */
		public static byte[] s4S_listHomeResp(Collection<String> homes, Collection<String> otherGroupHomes) {
			try (val out = DataOut.pool(ID, 4)) {
				out.writeInt(homes.size());
				for (val x : homes) out.writeUTF(x);
				out.writeInt(otherGroupHomes.size());
				for (val x : otherGroupHomes) out.writeUTF(x);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 数据包<br>
	 * 复杂数据包函数命名规则:
	 *
	 * <pre>
	 *     函数名: tis_n
	 *     t: type	函数类型, 当其为s时表示发送用的函数，为p时表示解析用的函数
	 *     i: index	数据包子ID, 用于识别分类
	 *     s: side	发送边, C代表客户端(bukkit), S代表服务器端(bungee)
	 *     n: name	函数名称, 代表功能
	 *
	 *     其中
	 *     <table border="1">
	 *     <tr><td>功能表示</td><td>t=s</td><td>t=p</td></tr>
	 *     <tr><td>s=C</td><td>客户端发送数据</td><td>解析客户端数据</td></tr>
	 *     <tr><td>s=S</td><td>服务器发送数据</td><td>解析服务器数据</td></tr>
	 *     </table>
	 * </pre>
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static abstract class Package {
		@SuppressWarnings("javadoc")
		@FunctionalInterface
		public interface BiBoolConsumer {
			void accept(boolean t, boolean u);
		}

		@SuppressWarnings("javadoc")
		@FunctionalInterface
		public interface BiIntConsumer {
			void accept(int t, int u);
		}

		@SuppressWarnings("javadoc")
		@FunctionalInterface
		public interface BiObjIntConsumer<T, U> {
			void accept(T t, U u, int x);
		}

		@SuppressWarnings("javadoc")
		@FunctionalInterface
		public interface BiPlayerConsumer {
			void accept(String aName, String aDisplay, String bName, String bDisplay);
		}

		@SuppressWarnings("javadoc")
		@FunctionalInterface
		public interface BoolConsumer {
			void accept(boolean t);
		}

		@SuppressWarnings("javadoc")
		@FunctionalInterface
		public interface ObjBoolConsumer<T> {
			void accept(T t, boolean u);
		}

	}

	/**
	 * 权限检查数据包
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Permission extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Client
		 *
		 * @param buf 数据
		 *
		 * @return 权限节点
		 */
		@NonNull
		public static String parseC(byte @NonNull [] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readUTF();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析Server
		 *
		 * @param buf                数据
		 * @param permissionAndAllow 权限,是否允许
		 */
		public static void parseS(byte @NonNull [] buf, ObjBoolConsumer<String> permissionAndAllow) {
			try (val in = DataIn.pool(buf)) {
				permissionAndAllow.accept(in.readUTF(), in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @param permission 权限节点
		 * @param allow      是否拥有权限
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendC(String permission, boolean allow) {
			try (val out = DataOut.pool(ID)) {
				out.writeUTF(permission);
				out.writeBoolean(allow);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @param permission 权限节点
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendS(@NonNull String permission) {
			try (val out = DataOut.pool(ID)) {
				out.writeUTF(permission);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 播放音效数据包
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class PlaySound extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 获取音效
		 *
		 * @param buf 数据
		 *
		 * @return 音效
		 */
		public static Sounds getSound(byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return Sounds.SOUNDS[in.readInt()];
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送到Client
		 *
		 * @param sounds 音效
		 *
		 * @return 数据包
		 */
		public static byte[] play(Sounds sounds) {
			try (val out = DataOut.pool(ID)) {
				out.writeInt(sounds.ordinal());
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 音效类型
		 *
		 * @author yuanlu
		 */
		public enum Sounds {
			/** At玩家时的音效 */
			AT;

			/** SOUNDS */
			private static final Sounds[] SOUNDS = values();
		}

	}

	/**
	 * 权限检查数据包
	 *
	 * @author yuanlu
	 */
	@Value
	@EqualsAndHashCode(callSuper = false)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class ServerInfo extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;
		/** tab名称(全部) */
		String    tab;
		/** BC版本 */
		String    version;
		ProxyType proxyType;

		/**
		 * 解析Server
		 *
		 * @param buf 数据
		 *
		 * @return 权限节点
		 */
		public static ServerInfo parseS(byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return new ServerInfo(in.readUTF(), in.readUTF(), ProxyType.valueOf(in.readUTF()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @param tab     tab名称
		 * @param version BC版本
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendS(@NonNull String tab, @NonNull String version, @NonNull ProxyType proxyType) {
			try (val out = DataOut.pool(ID)) {
				out.writeUTF(tab);
				out.writeUTF(version);
				out.writeUTF(proxyType.name());
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public enum ProxyType {
			BungeeCord, Velocity
		}
	}

	/**
	 * 时间修正数据包
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class TimeAmend extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Client
		 *
		 * @param buf 数据
		 *
		 * @return 客户端时间戳
		 */
		public static long parseC(byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readLong();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @return 数据包(可复用)
		 */
		public static byte @NonNull [] sendC() {
			try (val out = DataOut.pool(ID)) {
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendS() {
			try (val out = DataOut.pool(ID)) {
				out.writeLong(System.currentTimeMillis());
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 传送 <br>
	 * /tp target:
	 *
	 * <pre>
	 * A(移动者) B(服务器) C(目标者)
	 * 1. A 0 B (C名 0)
	 * 2. B 2 C (A全名 A展名 0)
	 * C msg
	 * 3. B 1 A (C全名 C展名)
	 * A msg
	 * 4. A 6 B (A全名 C全名)
	 * 5. B 8 C (A全名)
	 * 6. B 7 A ()
	 * </pre>
	 * <p>
	 * /tp mover target:
	 *
	 * <pre>
	 * A(发起者) B(服务器) C(移动者) D(目标者)
	 * 1. A 9 B (C名 D全名)
	 * 2. B 2 C (D全名 D展名 4)
	 * C msg
	 * 3. B 2 D (C全名 C展名 5)
	 * D msg
	 * 4. B a A (C全名 C展名 D全名 D展名)
	 * A msg
	 * 5. A 6 B (C全名 D全名)
	 * 6. B 8 D (C全名)
	 * 7. B 7 A ()
	 * </pre>
	 * <p>
	 * /tpa target:
	 *
	 * <pre>
	 * A(移动者) B(服务器) C(目标者)
	 * 1. A 0 B (C全名 1)
	 * 2. B 2 C (A全名 A展名 1)
	 * C msg
	 * 3. B 1 A (C全名 C展名)
	 * A msg
	 *
	 * C accept
	 * C msg
	 * 4. C 3 B (true)
	 * 5. B 4 C ()
	 * 6. B 5 A (true)
	 * A msg accept
	 * 7. A 6 B (A全名 C全名)
	 * 8. B 8 C (A全名)
	 * 9. B 7 A ()
	 *
	 * C deny
	 * C msg
	 * 4. C 3 B (false)
	 * 5. B 4 C ()
	 * 6. B 5 A (false)
	 * A msg deny
	 *
	 * </pre>
	 * <p>
	 * /tphere target:(/tp target self)
	 *
	 * <pre>
	 * A(移动者) B(服务器) C(目标者)
	 * 1. A 0 B (C名 2)
	 * 2. B 2 C (A全名 A展名 2)
	 * C msg
	 * 3. B 1 A (C全名 C展名)
	 * A msg
	 * 4. A 6 B (C全名 A全名)
	 * 5. B 8 A (C全名)
	 * 6. B 7 A ()
	 * </pre>
	 * <p>
	 * /tpahere target:
	 *
	 * <pre>
	 * A(发起者) B(服务器) C(被移动者)
	 * 1. A 0 B (C全名 3)
	 * 2. B 2 C (A全名 A展名 3)
	 * C msg
	 * 3. B 1 A (C全名 C展名)
	 * A msg
	 *
	 * C accept
	 * C msg
	 * 4. C 3 B (true)
	 * 5. B 4 C ()
	 * 6. B 5 A (true)
	 * A msg accept
	 * 7. C 6 B (C全名 A全名)
	 * 8. B 8 A (C全名)
	 * 9. B 7 C ()
	 *
	 * C deny
	 * 4. C 3 B (false)
	 * 5. B 4 C ()
	 * 6. B 5 A (false)
	 * A msg deny
	 * </pre>
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Tp extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析: C1搜索用户并发送传送请求
		 *
		 * @param buf           数据
		 * @param targetAndType 搜索内容,传送类型
		 *
		 * @see #s0C_tpReq(String, int)
		 */
		public static void p0C_tpReq(byte @NonNull [] buf, ObjIntConsumer<String> targetAndType) {
			try (val in = DataIn.pool(buf, 0)) {
				targetAndType.accept(in.readUTF(), in.readInt());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S响应用户 to C1
		 *
		 * @param buf            数据
		 * @param nameAndDisplay 搜索匹配到的真实名字,其展示名字
		 *
		 * @see #s1S_tpReqReceive(String, String)
		 */
		public static void p1S_searchResult(byte @NonNull [] buf, BiConsumer<String, String> nameAndDisplay) {
			try (val in = DataIn.pool(buf, 1)) {
				nameAndDisplay.accept(in.readUTF(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S传送请求(转发) to C2
		 *
		 * @param buf                   数据
		 * @param nameAndDisplayAndType 传送者真实名字,传送者展示名字,传送类型
		 *
		 * @see #s2S_tpReq(String, String, int)
		 */
		public static void p2S_tpReq(byte @NonNull [] buf, BiObjIntConsumer<String, String> nameAndDisplayAndType) {
			try (val in = DataIn.pool(buf, 2)) {
				nameAndDisplayAndType.accept(in.readUTF(), in.readUTF(), in.readInt());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: C2请求响应
		 *
		 * @param buf         数据
		 * @param whoAndAllow 目标全名,是否允许传送
		 *
		 * @see #s3C_tpResp(String, boolean)
		 */
		public static void p3C_tpResp(byte @NonNull [] buf, ObjBoolConsumer<String> whoAndAllow) {
			try (val in = DataIn.pool(buf, 3)) {
				whoAndAllow.accept(in.readUTF(), in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S接收到响应 to C2
		 *
		 * @param buf     数据
		 * @param success 是否成功处理
		 *
		 * @see #s4S_tpRespReceive(boolean)
		 */
		public static void p4S_tpRespReceive(byte @NonNull [] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 4)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S请求响应(转发) to C1
		 *
		 * @param buf         数据
		 * @param whoAndAllow C2玩家全名,是否允许传送
		 *
		 * @see #s5S_tpResp(String, boolean)
		 */
		public static void p5S_tpResp(byte @NonNull [] buf, ObjBoolConsumer<String> whoAndAllow) {
			try (val in = DataIn.pool(buf, 5)) {
				whoAndAllow.accept(in.readUTF(), in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: C实际传送
		 *
		 * @param buf            数据
		 * @param moverAndTarget 被移动者,移动目标
		 *
		 * @see #s6C_tpThird(String, String)
		 */
		public static void p6C_tpThird(byte @NonNull [] buf, BiConsumer<String, String> moverAndTarget) {
			try (val in = DataIn.pool(buf, 6)) {
				moverAndTarget.accept(in.readUTF(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S实际传送响应
		 *
		 * @param buf             数据
		 * @param successAndError 是否成功传送,是否有错误
		 *
		 * @see #s7S_tpThirdReceive(boolean, boolean)
		 */
		public static void p7S_tpThirdReceive(byte @NonNull [] buf, BiBoolConsumer successAndError) {
			try (val in = DataIn.pool(buf, 7)) {
				successAndError.accept(in.readBoolean(), in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S 被传送(转发)
		 *
		 * @param buf  数据
		 * @param name 被传送者
		 *
		 * @see #s8S_tpThird(String)
		 */
		public static void p8S_tpThird(byte @NonNull [] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 8)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: C1 搜索用户C2,C3并发送传送请求
		 *
		 * @param buf            数据
		 * @param moverAndTarget 双方搜索内容
		 *
		 * @see #s9C_tpReqThird(String, String, int)
		 */
		public static void p9C_tpReqThird(byte @NonNull [] buf, BiObjIntConsumer<String, String> moverAndTarget) {
			try (val in = DataIn.pool(buf, 9)) {
				moverAndTarget.accept(in.readUTF(), in.readUTF(), in.readInt());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: S响应用户 to C1
		 *
		 * @param buf            数据
		 * @param moverAndTarget 双方名称及展示名
		 *
		 * @see #saS_tpReqThirdReceive(String, String, String, String)
		 */
		public static void paS_tpReqThirdReceive(byte @NonNull [] buf, BiPlayerConsumer moverAndTarget) {
			try (val in = DataIn.pool(buf, 0xa)) {
				moverAndTarget.accept(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: C 取消传送
		 *
		 * @param buf    数据
		 * @param target 目标名称
		 *
		 * @see #sbC_cancel(String)
		 */
		public static void pbC_cancel(byte @NonNull [] buf, Consumer<String> target) {
			try (val in = DataIn.pool(buf, 0xb)) {
				target.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: C 取消传送
		 *
		 * @param buf  数据
		 * @param from 目标名称
		 *
		 * @see #scS_cancel(String)
		 */
		public static void pcS_cancel(byte @NonNull [] buf, Consumer<String> from) {
			try (val in = DataIn.pool(buf, 0xc)) {
				from.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		//

		/**
		 * C1搜索用户并发送传送请求<br>
		 * 传送类型:
		 *
		 * <pre>
		 * 0: tp
		 * 1: tpa
		 * 2: tphere
		 * 3: tpahere
		 * 4: 第三方传送mover
		 * 5: 第三方传送target
		 *
		 * 当类型{@code <0}时, 代表其拥有高级权限
		 * </pre>
		 *
		 * @param target 搜索内容
		 * @param type   传送类型
		 *
		 * @return 数据包
		 */
		public static byte[] s0C_tpReq(String target, int type) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeUTF(target);
				out.writeInt(type);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S响应用户 to C1
		 *
		 * @param name    搜索匹配到的真实名字
		 * @param display 其展示名字
		 *
		 * @return 数据包
		 */
		public static byte[] s1S_tpReqReceive(String name, String display) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeUTF(name);
				out.writeUTF(display);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S传送请求(转发) to C2
		 *
		 * @param name    传送者真实名字
		 * @param display 传送者展示名字
		 * @param type    传送类型
		 *
		 * @return 数据包
		 */
		public static byte[] s2S_tpReq(String name, String display, int type) {
			try (val out = DataOut.pool(ID, 2)) {
				out.writeUTF(name);
				out.writeUTF(display);
				out.writeInt(type);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * C2请求响应<br>
		 * 对于传送类型0(tp)和2(tphere), allow必为true
		 *
		 * @param who   C1玩家全名
		 * @param allow 是否允许传送
		 *
		 * @return 数据包
		 */
		public static byte[] s3C_tpResp(String who, boolean allow) {
			try (val out = DataOut.pool(ID, 3)) {
				out.writeUTF(who);
				out.writeBoolean(allow);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S接收到响应 to C2
		 *
		 * @param success 是否成功处理
		 *
		 * @return 数据包
		 */
		public static byte[] s4S_tpRespReceive(boolean success) {
			try (val out = DataOut.pool(ID, 4)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S请求响应(转发) to C1
		 *
		 * @param who   C2玩家全名
		 * @param allow 是否允许传送
		 *
		 * @return 数据包
		 */
		public static byte[] s5S_tpResp(String who, boolean allow) {
			try (val out = DataOut.pool(ID, 5)) {
				out.writeUTF(who);
				out.writeBoolean(allow);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * C实际传送
		 *
		 * @param mover  被移动者
		 * @param target 移动目标
		 *
		 * @return 数据包
		 */
		public static byte[] s6C_tpThird(String mover, String target) {
			try (val out = DataOut.pool(ID, 6)) {
				out.writeUTF(mover);
				out.writeUTF(target);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S实际传送响应
		 *
		 * @param success 是否成功传送
		 * @param error   是否有错误
		 *
		 * @return 数据包
		 */
		public static byte[] s7S_tpThirdReceive(boolean success, boolean error) {
			try (val out = DataOut.pool(ID, 7)) {
				out.writeBoolean(success);
				out.writeBoolean(error);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S 被传送(转发)
		 *
		 * @param name 传送者名称
		 *
		 * @return 数据包
		 */
		public static byte[] s8S_tpThird(String name) {
			try (val out = DataOut.pool(ID, 8)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * C1 搜索用户C2,C3并发送传送请求
		 *
		 * @param mover  搜索内容
		 * @param target 搜索内容
		 * @param code   附属码
		 *
		 * @return 数据包
		 */
		public static byte[] s9C_tpReqThird(String mover, String target, int code) {
			try (val out = DataOut.pool(ID, 9)) {
				out.writeUTF(mover);
				out.writeUTF(target);
				out.writeInt(code);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S响应用户 to C1
		 *
		 * @param mover         被移动者
		 * @param moverDisplay  其展示名字
		 * @param target        移动目标
		 * @param targetDisplay 其展示名字
		 *
		 * @return 数据包
		 */
		public static byte[] saS_tpReqThirdReceive(String mover, String moverDisplay, String target, String targetDisplay) {
			try (val out = DataOut.pool(ID, 0xa)) {
				out.writeUTF(mover);
				out.writeUTF(moverDisplay);
				out.writeUTF(target);
				out.writeUTF(targetDisplay);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * C 取消传送
		 *
		 * @param target 目标名称
		 *
		 * @return 数据包
		 */
		public static byte[] sbC_cancel(String target) {
			try (val out = DataOut.pool(ID, 0xb)) {
				out.writeUTF(target);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * S 取消传送
		 *
		 * @param from 请求名称
		 *
		 * @return 数据包
		 */
		public static byte[] scS_cancel(String from) {
			try (val out = DataOut.pool(ID, 0xc)) {
				out.writeUTF(from);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * 传送位置
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class TpLoc extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析: 传送坐标
		 *
		 * @param buf          数据
		 * @param locAndServer 要传送的位置,要传送的服务器
		 *
		 * @see #s0C_tpLoc(ShareLocation, String)
		 */
		public static void p0C_tpLoc(byte[] buf, BiConsumer<ShareLocation, String> locAndServer) {
			try (val in = DataIn.pool(buf, 0)) {
				locAndServer.accept(in.readLocation(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送地标响应
		 *
		 * @param buf     数据
		 * @param success 是否成功
		 *
		 * @see #s0S_tpLocResp(boolean)
		 */
		public static void p0S_tpLocResp(byte[] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 0)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送坐标
		 *
		 * @param buf          数据
		 * @param locAndPlayer 要传送的位置,被传送的玩家
		 *
		 * @see #s1S_tpLoc(ShareLocation, String)
		 */
		public static void p1S_tpLoc(byte[] buf, BiConsumer<ShareLocation, String> locAndPlayer) {
			try (val in = DataIn.pool(buf, 1)) {
				locAndPlayer.accept(in.readLocation(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送坐标
		 *
		 * @param loc    要传送的位置
		 * @param server 要传送的服务器
		 *
		 * @return 数据包
		 */
		public static byte[] s0C_tpLoc(ShareLocation loc, String server) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeLocation(loc);
				out.writeUTF(server);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送坐标响应
		 *
		 * @param success 是否成功
		 *
		 * @return 数据包
		 */
		public static byte[] s0S_tpLocResp(boolean success) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送坐标
		 *
		 * @param loc    要传送的位置
		 * @param player 被传送的玩家
		 *
		 * @return 数据包
		 */
		public static byte[] s1S_tpLoc(ShareLocation loc, String player) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeLocation(loc);
				out.writeUTF(player);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 转换家数据包
	 *
	 * @author yuanlu
	 */
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
	@ToString
	public static final class TransHome extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;
		/** 玩家UUID */
		UUID          player;
		/** 家名称 */
		String        name;
		/** 家坐标 */
		ShareLocation loc;

		/**
		 * 解析: 响应
		 *
		 * @param buf    数据
		 * @param amount 接收到的数量
		 */
		public static void parseC(byte[] buf, IntConsumer amount) {
			try (val in = DataIn.pool(buf)) {
				amount.accept(in.readInt());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 家数据
		 *
		 * @param buf  数据
		 * @param home 家数据
		 */
		public static void parseS(byte[] buf, Consumer<TransHome> home) {
			try (val in = DataIn.pool(buf)) {
				home.accept(in.readBoolean() ? null : new TransHome(in.readUUID(), in.readUTF(), in.readLocation()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @param amount 接收到的数量
		 *
		 * @return 数据包
		 */
		public static byte[] sendC(int amount) {
			try (val out = DataOut.pool(ID)) {
				out.writeInt(amount);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server<br>
		 * 结束标志
		 *
		 * @return 数据包
		 */
		public static byte[] sendS() {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(true);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @param player 玩家UUID
		 * @param name   家名称
		 * @param loc    家坐标
		 *
		 * @return 数据包
		 */
		public static byte[] sendS(UUID player, String name, ShareLocation loc) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(false);
				out.writeUUID(player);
				out.writeUTF(name);
				out.writeLocation(loc);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 转换地标数据包
	 *
	 * @author yuanlu
	 */
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
	@ToString
	public static final class TransWarp extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;
		/** 地标名称 */
		String        name;
		/** 地标坐标 */
		ShareLocation loc;

		/**
		 * 解析: 响应
		 *
		 * @param buf    数据
		 * @param amount 接收到的数量
		 */
		public static void parseC(byte[] buf, IntConsumer amount) {
			try (val in = DataIn.pool(buf)) {
				amount.accept(in.readInt());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 地标数据
		 *
		 * @param buf  数据
		 * @param warp 地标数据
		 */
		public static void parseS(byte[] buf, Consumer<TransWarp> warp) {
			try (val in = DataIn.pool(buf)) {
				warp.accept(in.readBoolean() ? null : new TransWarp(in.readUTF(), in.readLocation()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @param amount 接收到的数量
		 *
		 * @return 数据包
		 */
		public static byte[] sendC(int amount) {
			try (val out = DataOut.pool(ID)) {
				out.writeInt(amount);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server<br>
		 * 结束标志
		 *
		 * @return 数据包
		 */
		public static byte[] sendS() {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(true);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @param name 地标名称
		 * @param loc  地标坐标
		 *
		 * @return 数据包
		 */
		public static byte[] sendS(String name, ShareLocation loc) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(false);
				out.writeUTF(name);
				out.writeLocation(loc);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 隐身模式
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Vanish extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析
		 *
		 * @param buf 数据
		 *
		 * @return 双向数据
		 */
		public static boolean parse(byte @NonNull [] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readBoolean();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @param inHide 当前是否处于隐身状态
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendC(boolean inHide) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(inHide);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 *
		 * @param always 是否每次上线都拥有隐身
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendS(boolean always) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(always);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 版本检查<br>
	 * 将会比较由 {@link Channel} 枚举项生成的 {@link Channel#VERSION MD5} 值, 确认插件版本是否正确
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class VersionCheck extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Client
		 *
		 * @param buf 数据
		 *
		 * @return 客户端与服务器版本是否一致
		 */
		public static boolean parseC(byte[] buf) {
			try (DataIn in = DataIn.pool(buf)) {
				byte[] ver = new byte[VERSION.length];
				boolean equal = in.read(ver) == VERSION.length;
				for (int i = 0; i < VERSION.length; i++) if (!(equal = (ver[i] == VERSION[i]))) break;
				return equal;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析Server
		 *
		 * @param buf 数据
		 *
		 * @return 客户端与服务器版本是否一致
		 */
		public static boolean parseS(byte @NonNull [] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readBoolean();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @param equal 客户端与服务器版本是否一致
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendC(boolean equal) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(equal);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Client
		 *
		 * @param buf Client数据
		 *
		 * @return 数据包
		 *
		 * @see #parseC(byte[])
		 * @see #sendC(boolean)
		 */
		public static byte @NonNull [] sendC(byte @NonNull [] buf) {
			return sendC(parseC(buf));
		}

		/**
		 * 发送至Server
		 *
		 * @return 数据包
		 */
		public static byte @NonNull [] sendS() {
			try (val out = DataOut.pool(ID)) {
				out.write(VERSION);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 地标数据包
	 *
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Warp extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析: 设置地标
		 *
		 * @param buf        数据
		 * @param nameAndLoc 地标名称,地标
		 *
		 * @see #s0C_setWarp(String, ShareLocation)
		 */
		public static void p0C_setWarp(byte @NonNull [] buf, BiConsumer<String, ShareLocation> nameAndLoc) {
			try (val in = DataIn.pool(buf, 0)) {
				nameAndLoc.accept(in.readUTF(), in.readLocation());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 设置地标响应
		 *
		 * @param buf 数据
		 * @param r   next
		 *
		 * @see #s0S_setWarpResp()
		 */
		public static void p0S_setWarpResp(byte @NonNull [] buf, Runnable r) {
			try (val ignored = DataIn.pool(buf, 0)) {
				r.run();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 删除地标
		 *
		 * @param buf  数据
		 * @param name 地标名称
		 *
		 * @see #s1C_delWarp(String)
		 */
		public static void p1C_delWarp(byte @NonNull [] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 1)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 删除地标响应
		 *
		 * @param buf     数据
		 * @param success 删除删除
		 *
		 * @see #s1S_delWarpResp(boolean)
		 */
		public static void p1S_delWarpResp(byte @NonNull [] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 1)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 搜索地标
		 *
		 * @param buf  数据
		 * @param name 地标名称
		 *
		 * @see #s2C_searchWarp(String)
		 */
		public static void p2C_searchWarp(byte @NonNull [] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 2)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 搜索地标响应
		 *
		 * @param buf           数据
		 * @param nameAndServer 地标名称,服务器名称
		 *
		 * @see #s2S_searchWarpResp(String, String)
		 */
		public static void p2S_searchWarpResp(byte @NonNull [] buf, BiConsumer<String, String> nameAndServer) {
			try (val in = DataIn.pool(buf, 2)) {
				nameAndServer.accept(in.readUTF(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送地标
		 *
		 * @param buf  数据
		 * @param name 地标名称
		 *
		 * @see #s3C_tpWarp(String)
		 */
		public static void p3C_tpWarp(byte[] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf, 3)) {
				name.accept(in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送地标响应
		 *
		 * @param buf     数据
		 * @param success 是否成功
		 *
		 * @see #s3S_tpWarpResp(boolean)
		 */
		public static void p3S_tpWarpResp(byte[] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf, 3)) {
				success.accept(in.readBoolean());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送地标列表
		 *
		 * @param buf 数据
		 * @param r   next
		 *
		 * @see #s4C_listWarp()
		 */
		public static void p4C_listWarp(byte[] buf, Runnable r) {
			try (val ignored = DataIn.pool(buf, 4)) {
				r.run();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 解析: 传送地标列表响应
		 *
		 * @param buf   数据
		 * @param warps 两组地标列表
		 *
		 * @see #s4S_listWarpResp(Collection, Collection)
		 */
		public static void p4S_listWarpResp(byte[] buf, BiConsumer<Collection<String>, Collection<String>> warps) {
			try (val in = DataIn.pool(buf, 4)) {
				Collection<String> tar1 = new LinkedHashSet<>();
				for (int i = 0, j = in.readInt(); i < j; i++) tar1.add(in.readUTF());
				Collection<String> tar2 = new LinkedHashSet<>();
				for (int i = 0, j = in.readInt(); i < j; i++) tar2.add(in.readUTF());
				warps.accept(tar1, tar2);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		//

		/**
		 * 设置地标
		 *
		 * @param name 地标名称
		 * @param loc  地标
		 *
		 * @return 数据包
		 */
		public static byte[] s0C_setWarp(String name, ShareLocation loc) {
			try (val out = DataOut.pool(ID, 0)) {
				out.writeUTF(name);
				out.writeLocation(loc);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 设置地标响应
		 *
		 * @return 数据包
		 */
		public static byte[] s0S_setWarpResp() {
			try (val out = DataOut.pool(ID, 0)) {
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 删除地标
		 *
		 * @param name 地标名称
		 *
		 * @return 数据包
		 */
		public static byte[] s1C_delWarp(String name) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 删除地标响应
		 *
		 * @param success 删除删除
		 *
		 * @return 数据包
		 */
		public static byte[] s1S_delWarpResp(boolean success) {
			try (val out = DataOut.pool(ID, 1)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 搜索地标
		 *
		 * @param name 地标名称
		 *
		 * @return 数据包
		 */
		public static byte[] s2C_searchWarp(String name) {
			try (val out = DataOut.pool(ID, 2)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 搜索地标响应
		 *
		 * @param name   地标名称
		 * @param server 所在服务器
		 *
		 * @return 数据包
		 */
		public static byte[] s2S_searchWarpResp(String name, String server) {
			try (val out = DataOut.pool(ID, 2)) {
				out.writeUTF(name);
				out.writeUTF(server);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送地标
		 *
		 * @param name 地标名称
		 *
		 * @return 数据包
		 */
		public static byte[] s3C_tpWarp(String name) {
			try (val out = DataOut.pool(ID, 3)) {
				out.writeUTF(name);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送地标响应
		 *
		 * @param success 是否成功
		 *
		 * @return 数据包
		 */
		public static byte[] s3S_tpWarpResp(boolean success) {
			try (val out = DataOut.pool(ID, 3)) {
				out.writeBoolean(success);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送地标列表
		 *
		 * @return 数据包
		 */
		public static byte[] s4C_listWarp() {
			try (val out = DataOut.pool(ID, 4)) {
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 传送地标列表响应
		 *
		 * @param warps           同一服务器组的地标列表
		 * @param otherGroupWarps 其他组的地标列表
		 *
		 * @return 数据包
		 */
		public static byte[] s4S_listWarpResp(Collection<String> warps, Collection<String> otherGroupWarps) {
			try (val out = DataOut.pool(ID, 4)) {
				out.writeInt(warps.size());
				for (val x : warps) out.writeUTF(x);
				out.writeInt(otherGroupWarps.size());
				for (val x : otherGroupWarps) out.writeUTF(x);
				return out.getByte();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
