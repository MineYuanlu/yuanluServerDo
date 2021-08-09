/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.util.command;

import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.val;

/**
 * 检查命令来源类型的嵌套命令执行器<br>
 * 一般用于限定控制台或玩家
 * 
 * @author yuanlu
 *
 */
public final class CheckSenderCommand extends FilterCommand {

	/**
	 * 检查类型
	 * 
	 * @author yuanlu
	 *
	 */
	public static enum CheckType {
		/**
		 * 传入的{@link CommandSender}是{@link CheckSenderCommand#clazz}的子类
		 */
		EXTENDS,
		/**
		 * 传入的{@link CommandSender}等于{@link CheckSenderCommand#clazz}
		 */
		EQUAL,
		/**
		 * 传入的{@link CommandSender}不是是{@link CheckSenderCommand#clazz}的子类
		 */
		NOTEXTENDS,
		/**
		 * 传入的{@link CommandSender}不等于{@link CheckSenderCommand#clazz}
		 */
		NOTEQUAL;
	}

	/**
	 * 类的类型名Hash图<br>
	 * 缓存常量
	 */
	public static final HashMap<Class<? extends CommandSender>, String> TYPE_NAME;

	/**
	 * 类的名称Hash图<br>
	 * 缓存常量
	 */
	public static final HashMap<Class<? extends CommandSender>, String> SIMPLE_NAME;

	/**
	 * 是否启用名称缓存图
	 */
	public static final boolean                                         USE_NAME_CACHE = true;

	/**
	 * 初始化是否使用缓存
	 */
	static{
		if (USE_NAME_CACHE){
			TYPE_NAME = new HashMap<>();
			SIMPLE_NAME = new HashMap<>();
		} else{
			TYPE_NAME = null;
			SIMPLE_NAME = null;
		}
	}

	/**
	 * 获取类的SimpleName
	 * 
	 * @param c 类
	 * @return SimpleName
	 */
	protected static final String getCSname(Class<? extends CommandSender> c) {
		if (!USE_NAME_CACHE) return c.getSimpleName();
		String name = SIMPLE_NAME.get(c);
		if (name == null){
			name = c.getSimpleName();
			SIMPLE_NAME.put(c, name);
		}
		return name;
	}

	/**
	 * 获取类的TypeName
	 * 
	 * @param c 类
	 * @return TypeName
	 */
	protected static final String getCTname(Class<? extends CommandSender> c) {
		if (!USE_NAME_CACHE) return c.getTypeName();
		String name = TYPE_NAME.get(c);
		if (name == null){
			name = c.getTypeName();
			TYPE_NAME.put(c, name);
		}
		return name;
	}

	/**
	 * 检测的类
	 */
	public final Class<? extends CommandSender> clazz;

	/**
	 * 检测类型
	 */
	public final CheckType                      type;

	/**
	 * 检测没有通过时发出的信息(默认)
	 */
	public final Msg                            MES_NOT_PASS;

	/**
	 * 构造一个全参数的命令来源类型检查器
	 * 
	 * @param next        在命令通过过滤时执行命令执行器
	 * @param clazz       检测的类
	 * @param type        检测类型
	 * @param mes_notPass 检测未通过时的信息
	 */
	public CheckSenderCommand(AbstractCommand next, Class<? extends CommandSender> clazz, CheckType type,
			Msg mes_notPass) {
		super(next);
		if (clazz == null) throw new NullPointerException("clazz can not be null.");
		if (type == null) throw new NullPointerException("type can not be null.");
		if (mes_notPass == null) throw new NullPointerException("mes_notPass can not be null.");
		this.clazz = clazz;
		this.type = type;
		this.MES_NOT_PASS = mes_notPass;
	}

	/**
	 * 构造一个命令来源是否是玩家的检查器
	 * 
	 * @param next        在命令通过过滤时执行命令执行器
	 * @param mes_notPass 检测未通过时的信息
	 */
	public CheckSenderCommand(AbstractCommand next, Msg mes_notPass) {
		this(next, Player.class, CheckType.EXTENDS, mes_notPass);
	}

	@Override
	public final boolean check(CommandSender sender, Command cmd, String label, String[] args, int argsIndex) {
		val c = sender.getClass();
		final boolean ok;
		switch (type) {
			case EQUAL:
				ok = c == clazz;
				break;
			case EXTENDS:
				ok = clazz.isAssignableFrom(c);
				break;
			case NOTEQUAL:
				ok = c != clazz;
				break;
			case NOTEXTENDS:
				ok = !clazz.isAssignableFrom(c);
				break;
			default:
				ok = false;
		}
		return ok;
	}

	/**
	 * 当命令执行的来源类型不符合预设的类型时调用此方法<br>
	 * 若没有继承本类并重写本方法, 则将会把预设的{@link #MES_NOT_PASS}发给命令执行的来源<br>
	 * 发送的信息将会进行如下的替换:<br>
	 * <br>
	 * %sender% {@code ->} 命令执行来源的类型名称(getSimpleName())<br>
	 * %class% {@code ->} 命令执行来源的类型名称(getTypeName())<br>
	 * <br>
	 * 以上两个名称可能将会进行缓存
	 * 
	 * @param sender    命令执行的来源
	 * @param cmd       被执行的命令
	 * @param label     使用的命令别名
	 * @param args      传递的命令参数
	 * @param argsIndex 命令参数读取起点位置<br>
	 *                  根命令从0开始
	 */
	@Override
	protected void onNoPermissionToRunCommand(CommandSender sender, Command cmd, String label, String[] args,
			int argsIndex) {
		val senderClass = sender.getClass();
		MES_NOT_PASS.send(sender, getCSname(senderClass), getCTname(senderClass));
	}
}
