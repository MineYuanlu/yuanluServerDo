/**
 * yuanlu
 * date: 2021年8月9日
 * file: Channel.java
 * gitu: yuanlu
 * gite: 2573580691@qq.com
 */
package yuan.plugins.serverDo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

/**
 * 通道数据<br>
 * 包含了服务器之间数据交互所用到的所有数据包<br>
 * 枚举定义了数据包类型, 其指向的实现类有此数据包下所有方法.
 * 
 * @author yuanlu
 *
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
	VANISH(Vanish.class);

	/**
	 * 传送冷却数据包
	 * 
	 * @author yuanlu
	 * @see Channel#PERMISSION
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
				val	player	= in.readUUID();
				val	end		= in.readLong();
				WaitMaintain.put(map, player, end, end - System.currentTimeMillis());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 * 
		 * @param end 冷却结束时间
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
				super.buf	= buf;
				this.pos	= this.mark = offsetByPackageID;
				this.count	= buf.length;
			}

		}

		/** 缓存池 */
		private static final DataIn	POOL[]	= new DataIn[16];
		/** 缓存池有效容量 */
		private static int			pool	= 0;

		/**
		 * 从对象池中取出一个数据输入器并初始化
		 * 
		 * @param buf 数据
		 * @return 输入器
		 */
		public static DataIn pool(byte[] buf) {
			synchronized (POOL) {
				if (pool > 0) return POOL[--pool].reset(buf);
			}
			return new DataIn(buf);
		}

		/** 此输入器是否已经释放 */
		private boolean release;

		/**
		 * 构造并初始化
		 * 
		 * @param buf 初始化数据流
		 */
		private DataIn(byte[] buf) {
			super(new ByteIn(buf));
			release = false;
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
		 * 读取一个UUID
		 * 
		 * @return UUID
		 * @throws IOException IOE
		 */
		public UUID readUUID() throws IOException {
			return new UUID(readLong(), readLong());
		}

		/**
		 * 初始化
		 * 
		 * @param buf 初始化数据流
		 * @return this
		 */
		private DataIn reset(byte[] buf) {
			((ByteIn) super.in).reset(buf);
			release = false;
			return this;
		}
	}

	/** 数据输出 */
	private static final class DataOut extends DataOutputStream {

		/** 缓存池 */
		private static final DataOut	POOL[]	= new DataOut[128];
		/** 缓存池有效容量 */
		private static int				pool	= 0;

		/**
		 * 从对象池中取出一个数据输出器并初始化
		 * 
		 * @param ID 包ID
		 * @return 输出器
		 * @throws IOException IO错误
		 */
		public static DataOut pool(int ID) throws IOException {
			synchronized (POOL) {
				if (pool > 0) return POOL[--pool].reset(ID);
			}
			return new DataOut(ID);
		}

		/** 此输入器是否已经释放 */
		private boolean release;

		/**
		 * 构造并初始化
		 * 
		 * @param ID 包ID
		 * @throws IOException IO错误
		 */
		public DataOut(int ID) throws IOException {
			super(new ByteArrayOutputStream());
			release = false;
			writeInt(ID);
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
		 * @return this
		 * @throws IOException IO错误
		 */
		private DataOut reset(int ID) throws IOException {
			release = false;
			((ByteArrayOutputStream) super.out).reset();
			writeInt(ID);
			return this;
		}

		/**
		 * 写入UUID
		 * 
		 * @param u uuid
		 * @throws IOException IOE
		 */
		public void writeUUID(UUID u) throws IOException {
			writeLong(u.getMostSignificantBits());
			writeLong(u.getLeastSignificantBits());
		}

	}

	/**
	 * 数据包
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
		public interface BiIntConsumer<T, U> {
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
	 * @see Channel#PERMISSION
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Permission extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Client
		 * 
		 * @param buf 数据
		 * @return 权限节点
		 */
		@NonNull
		public static String parseC(@NonNull byte[] buf) {
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
		public static void parseS(@NonNull byte[] buf, ObjBoolConsumer<String> permissionAndAllow) {
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
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendC(String permission, boolean allow) {
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
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendS(@NonNull String permission) {
			try (val out = DataOut.pool(ID)) {
				out.writeUTF(permission);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 权限检查数据包
	 * 
	 * @author yuanlu
	 * @see Channel#PERMISSION
	 */
	@Value
	@EqualsAndHashCode(callSuper = false)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class ServerInfo extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Server
		 * 
		 * @param buf 数据
		 * @return 权限节点
		 */
		public static ServerInfo parseS(byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return new ServerInfo(in.readUTF(), in.readUTF(), in.readUTF());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 发送至Server
		 * 
		 * @param tabAll  tab名称(全部)
		 * @param tabNor  tab名称(检测组)
		 * @param version BC版本
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendS(@NonNull String tabAll, @NonNull String tabNor, @NonNull String version) {
			try (val out = DataOut.pool(ID)) {
				out.writeUTF(tabAll);
				out.writeUTF(tabNor);
				out.writeUTF(version);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/** tab名称(全部) */
		String	tabAll;
		/** tab名称(检测组) */
		String	tabNor;

		/** BC版本 */
		String	version;
	}

	/**
	 * 权限检查数据包
	 * 
	 * @author yuanlu
	 * @see Channel#PERMISSION
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class TimeAmend extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Client
		 * 
		 * @param buf 数据
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
		public static byte[] sendC() {
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
		public static byte[] sendS() {
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
	 *
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Tp extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 检查接收到的数据包ID
		 * 
		 * @param in 数据流
		 * @param id 包ID
		 * @throws IOException IOE
		 */
		private static void checkId(DataIn in, int id) throws IOException {
			val packageId = in.readByte();
			if (packageId != id) throw new IllegalArgumentException("Bad Package Id: " + packageId + ", expect: " + id);
		}

		/**
		 * 解析: C1搜索用户并发送传送请求
		 * 
		 * @param buf           数据
		 * @param targetAndType 搜索内容,传送类型
		 * 
		 * @see #s0C_tpReq(String, int)
		 */
		public static void p0C_tpReq(byte[] buf, ObjIntConsumer<String> targetAndType) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 0);
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
		public static void p1S_searchResult(byte[] buf, BiConsumer<String, String> nameAndDisplay) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 1);
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
		 * @see #s2S_tpReq(String, String, int)
		 */
		public static void p2S_tpReq(byte[] buf, BiIntConsumer<String, String> nameAndDisplayAndType) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 2);
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
		 * @see #s3C_tpResp(String, boolean)
		 */
		public static void p3C_tpResp(byte[] buf, ObjBoolConsumer<String> whoAndAllow) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 3);
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
		 * @see #s4S_tpRespReceive(boolean)
		 */
		public static void p4S_tpRespReceive(byte[] buf, BoolConsumer success) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 4);
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
		 * @see #s5S_tpResp(String, boolean)
		 */
		public static void p5S_tpResp(byte[] buf, ObjBoolConsumer<String> whoAndAllow) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 5);
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
		 * @see #s6C_tpThird(String, String)
		 */
		public static void p6C_tpThird(byte[] buf, BiConsumer<String, String> moverAndTarget) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 6);
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
		 * @see #s7S_tpThirdReceive(boolean, boolean)
		 */
		public static void p7S_tpThirdReceive(byte[] buf, BiBoolConsumer successAndError) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 7);
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
		 * @see #s8S_tpThird(String)
		 */
		public static void p8S_tpThird(byte[] buf, Consumer<String> name) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 8);
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
		 * @see #s9C_tpReqThird(String, String, int)
		 */
		public static void p9C_tpReqThird(byte[] buf, BiIntConsumer<String, String> moverAndTarget) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 9);
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
		 * @see #saS_tpReqThirdReceive(String, String, String, String)
		 */
		public static void paS_tpReqThirdReceive(byte[] buf, BiPlayerConsumer moverAndTarget) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 0xa);
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
		 * @see #sbC_cancel(String)
		 */
		public static void pbC_cancel(byte[] buf, Consumer<String> target) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 0xb);
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
		 * @see #scS_cancel(String)
		 */
		public static void pcS_cancel(byte[] buf, Consumer<String> from) {
			try (val in = DataIn.pool(buf)) {
				checkId(in, 0xc);
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
		 * @return 数据包
		 */
		public static byte[] s0C_tpReq(String target, int type) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(0);
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
			try (val out = DataOut.pool(ID)) {
				out.writeByte(1);
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
			try (val out = DataOut.pool(ID)) {
				out.writeByte(2);
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
			try (val out = DataOut.pool(ID)) {
				out.writeByte(3);
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
		 * @return 数据包
		 */
		public static byte[] s4S_tpRespReceive(boolean success) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(4);
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
		 * @return 数据包
		 */
		public static byte[] s5S_tpResp(String who, boolean allow) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(5);
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
			try (val out = DataOut.pool(ID)) {
				out.writeByte(6);
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
		 * @return 数据包
		 */
		public static byte[] s7S_tpThirdReceive(boolean success, boolean error) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(7);
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
			try (val out = DataOut.pool(ID)) {
				out.writeByte(8);
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
		 * @return 数据包
		 */
		public static byte[] s9C_tpReqThird(String mover, String target, int code) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(9);
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
			try (val out = DataOut.pool(ID)) {
				out.writeByte(0xa);
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
		 * @return 数据包
		 */
		public static final byte[] sbC_cancel(String target) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(0xb);
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
		 * @return 数据包
		 */
		public static final byte[] scS_cancel(String from) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(0xc);
				out.writeUTF(from);
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
	 * @see Channel#PERMISSION
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Vanish extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析
		 * 
		 * @param buf 数据
		 * @return 双向数据
		 */
		@NonNull
		public static boolean parse(@NonNull byte[] buf) {
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
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendC(boolean inHide) {
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
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendS(boolean always) {
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
	 *
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class VersionCheck extends Package {
		/** 此数据包ID */
		protected static @Getter int ID;

		/**
		 * 解析Client
		 * 
		 * @param buf 数据
		 * @return 客户端与服务器版本是否一致
		 */
		public static boolean parseC(byte[] buf) {
			try (DataIn in = DataIn.pool(buf)) {
				byte	ver[]	= new byte[VERSION.length];
				boolean	equal	= in.read(ver) == VERSION.length;
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
		 * @return 客户端与服务器版本是否一致
		 */
		public static boolean parseS(@NonNull byte[] buf) {
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
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendC(boolean equal) {
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
		 * @return 数据包
		 * @see #parseC(byte[])
		 * @see #sendC(boolean)
		 */
		@NonNull
		public static byte[] sendC(byte[] buf) {
			return sendC(parseC(buf));
		}

		/**
		 * 发送至Server
		 * 
		 * @return 数据包
		 */
		@NonNull
		public static byte[] sendS() {
			try (val out = DataOut.pool(ID)) {
				out.write(VERSION);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/** 版本数据 */
	private static final byte[] VERSION;
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
	}

	/** 所有数据包 */
	private static final Channel CHANNELS[] = values();

	/**
	 * 解析数据包
	 * 
	 * @param id 数据包ID
	 * @return 数据包类型
	 */
	public static final Channel byId(int id) {
		return id >= 0 && id < CHANNELS.length ? CHANNELS[id] : null;
	}

	/** 目标类 */
	public final Class<? extends Package> target;

	/**
	 * 构造
	 * 
	 * @param target 目标类
	 */
	private Channel(Class<? extends Package> target) {
		this.target = target;
		try {
			target.getDeclaredField("ID").setInt(target, ordinal());
		} catch (Exception e) {
			throw new InternalError(e);
		}
	}

}
