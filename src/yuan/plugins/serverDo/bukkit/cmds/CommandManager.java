/**
 * @author yuanlu
 */
package yuan.plugins.serverDo.bukkit.cmds;

import java.util.HashMap;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import yuan.plugins.serverDo.bukkit.MESSAGE;

/**
 * 命令管理器
 * 
 * @author yuanlu
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandManager implements MESSAGE {
	/** 命令信息 */
	@Value
	static final class CmdInfo {
		/** 描述 */
		String			description;
		/** 使用方法 */
		String			usageMessage;
		/** 所有名称 */
		List<String>	names;
	}

	/**
	 * 所有的命令<br>
	 * {@code 命令名->命令信息}
	 */
	static final HashMap<String, CmdInfo> INFOS = new HashMap<>();
}
