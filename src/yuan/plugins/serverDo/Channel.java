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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

/**
 * @author yuanlu
 *
 */
@SuppressWarnings("javadoc")
public enum Channel {
	/** 版本检查 */
	VERSION_CHECK(VersionCheck.class),
	/** 权限检查 */
	PERMISSION(Permission.class), TP(Tp.class);

	/** 数据输入 */
	private static final class DataIn extends DataInputStream {
		private static final class ByteIn extends ByteArrayInputStream {
			private static final int offsetByPackageID = 4;

			public ByteIn(byte[] buf) {
				super(buf, offsetByPackageID, buf.length);
			}

			public void clear() {
				super.buf = null;
			}

			public void reset(byte[] buf) {
				super.buf	= buf;
				this.pos	= this.mark = offsetByPackageID;
				this.count	= buf.length;
			}

		}

		private static final DataIn	POOL[]	= new DataIn[16];

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

		private boolean release = true;

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

		private DataIn reset(byte[] buf) {
			((ByteIn) super.in).reset(buf);
			release = false;
			return this;
		}
	}

	/** 数据输出 */
	private static final class DataOut extends DataOutputStream {

		private static final DataOut	POOL[]	= new DataOut[128];

		private static int				pool	= 0;

		/**
		 * 从对象池中取出一个数据输出器
		 * 
		 * @return 输出器
		 */
		public static DataOut pool(int ID) throws IOException {
			synchronized (POOL) {
				if (pool > 0) return POOL[--pool].reset(ID);
			}
			return new DataOut(ID);
		}

		private boolean release = true;

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

		private DataOut reset(int ID) throws IOException {
			release = false;
			((ByteArrayOutputStream) super.out).reset();
			writeInt(ID);
			return this;
		}

	}

	/**
	 * 数据包
	 * 
	 * @author yuanlu
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static abstract class Package {
		/** 此数据包ID */
		protected static @Getter int ID;
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Permission extends Package {
		/** 解析Client */
		@NonNull
		public static String parseC(@NonNull byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readUTF();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/** 解析Server */
		public static boolean parseS(@NonNull byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readBoolean();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/** 发送至Client */
		@NonNull
		public static byte[] sendC(boolean permission) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(permission);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/** 发送至Server */
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

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Tp extends Package {
		/** 解析: C1搜索用户 */
		public static void p0C_search(byte[] buf) {

		}

		/** 解析: S响应用户 to C1 */
		public static void p1S_searchResult(byte[] buf) {

		}

		/** 解析: C1传送请求 */
		public static void p2C_tpReq(byte[] buf) {

		}

		/** 解析: S接收到请求 to C1 */
		public static void p3S_tpReqReceive(byte[] buf) {

		}

		/** 解析: S传送请求(转发) to C2 */
		public static void p4S_tpReq(byte[] buf) {

		}

		/** 解析: C2请求响应 */
		public static void p5C_tpResp(byte[] buf) {

		}

		/** 解析: S接收到响应 to C2 */
		public static void p6S_tpRespReceive(byte[] buf) {

		}

		/** 解析: S请求响应(转发) to C1 */
		public static void p7S_tpResp(byte[] buf) {

		}

		/** 解析: C实际传送(二人情况)(谁移动谁发起) */
		public static void p8C_tpReal(byte[] buf) {

		}

		/** 解析: C实际传送(三人情况) */
		public static void p9C_tpThird(byte[] buf) {

		}

		//

		/** C1搜索用户 */
		public static byte[] s0C_search(String target) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(0);
				out.writeUTF(target);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** S响应用户 to C1 */
		public static byte[] s1S_searchResult(String name, String display) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(1);
				out.writeUTF(name);
				out.writeUTF(display);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** C1传送请求 */
		public static byte[] s2C_tpReq(String name, int type) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(2);
				out.writeUTF(name);
				out.writeInt(type);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** S接收到请求 to C1 */
		public static byte[] s3S_tpReqReceive() {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(3);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** S传送请求(转发) to C2 */
		public static byte[] s4S_tpReq(String name, String display) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(4);
				out.writeUTF(name);
				out.writeUTF(display);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** C2请求响应 */
		public static byte[] s5C_tpResp(boolean allow) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(5);
				out.writeBoolean(allow);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** S接收到响应 to C2 */
		public static byte[] s6S_tpRespReceive() {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(6);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** S请求响应(转发) to C1 */
		public static byte[] s7S_tpResp(boolean allow) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(7);
				out.writeBoolean(allow);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** C实际传送(二人情况)(谁移动谁发起) */
		public static byte[] s8C_tpReal(String target) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(8);
				out.writeUTF(target);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

		/** C实际传送(三人情况) */
		public static byte[] s9C_tpThird(String mover, String target) {
			try (val out = DataOut.pool(ID)) {
				out.writeByte(9);
				out.writeUTF(mover);
				out.writeUTF(target);
				return out.getByte();
			} catch (Exception e) {
				throw new InternalError(e);
			}
		}

	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class VersionCheck extends Package {
		/** 解析Client */
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

		/** 解析Server */
		public static boolean parseS(@NonNull byte[] buf) {
			try (val in = DataIn.pool(buf)) {
				return in.readBoolean();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/** 发送至Client */
		@NonNull
		public static byte[] sendC(boolean equal) {
			try (val out = DataOut.pool(ID)) {
				out.writeBoolean(equal);
				return out.getByte();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/** 发送至Server */
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
	 * @param id
	 * @return 数据包类型
	 */
	public static final Channel byId(int id) {
		return id >= 0 && id < CHANNELS.length ? CHANNELS[id] : null;
	}

	public final Class<? extends Package> target;

	private Channel(Class<? extends Package> target) {
		this.target = target;
		try {
			target.getDeclaredField("ID").setInt(target, ordinal());
		} catch (Exception e) {
			throw new InternalError(e);
		}
	}
}
