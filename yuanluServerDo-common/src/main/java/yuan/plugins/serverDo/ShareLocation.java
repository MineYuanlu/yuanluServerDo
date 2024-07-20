/**
 *
 */
package yuan.plugins.serverDo;

import lombok.*;

/**
 * 共享用的坐标
 *
 * @author yuanlu
 */
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
@SuppressWarnings("javadoc")
@EqualsAndHashCode
public final class ShareLocation {
	private final @Getter double x, y, z;
	private final @Getter float yaw, pitch;
	private final @Getter String world;

	private @Setter
	@Getter String server;

	@Override
	public ShareLocation clone() {
		return new ShareLocation(x, y, z, yaw, pitch, world, server);
	}
}
