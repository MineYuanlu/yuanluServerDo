package yuan.plugins.serverDo;

import java.util.Map;

import org.apache.commons.lang.Validate;

import com.google.common.collect.Maps;

/**
 * All supported color values for chat
 */
@SuppressWarnings("javadoc")
public enum ChatColor {
	/**
	 * Represents black
	 */
	BLACK('0'),
	/**
	 * Represents dark blue
	 */
	DARK_BLUE('1'),
	/**
	 * Represents dark green
	 */
	DARK_GREEN('2'),
	/**
	 * Represents dark blue (aqua)
	 */
	DARK_AQUA('3'),
	/**
	 * Represents dark red
	 */
	DARK_RED('4'),
	/**
	 * Represents dark purple
	 */
	DARK_PURPLE('5'),
	/**
	 * Represents gold
	 */
	GOLD('6'),
	/**
	 * Represents gray
	 */
	GRAY('7'),
	/**
	 * Represents dark gray
	 */
	DARK_GRAY('8'),
	/**
	 * Represents blue
	 */
	BLUE('9'),
	/**
	 * Represents green
	 */
	GREEN('a'),
	/**
	 * Represents aqua
	 */
	AQUA('b'),
	/**
	 * Represents red
	 */
	RED('c'),
	/**
	 * Represents light purple
	 */
	LIGHT_PURPLE('d'),
	/**
	 * Represents yellow
	 */
	YELLOW('e'),
	/**
	 * Represents white
	 */
	WHITE('f'),
	/**
	 * Represents magical characters that change around randomly
	 */
	MAGIC('k', true),
	/**
	 * Makes the text bold.
	 */
	BOLD('l', true),
	/**
	 * Makes a line appear through the text.
	 */
	STRIKETHROUGH('m', true),
	/**
	 * Makes the text appear underlined.
	 */
	UNDERLINE('n', true),
	/**
	 * Makes the text italic.
	 */
	ITALIC('o', true),
	/**
	 * Resets all previous chat colors or formats.
	 */
	RESET('r');

	/**
	 * The special character which prefixes all chat colour codes. Use this if you
	 * need to dynamically convert colour codes from your custom format.
	 */
	public static final char						COLOR_CHAR	= '\u00A7';
	private static final Map<Character, ChatColor>	BY_CHAR		= Maps.newHashMap();
	static {
		for (ChatColor color : values()) {
			BY_CHAR.put(color.code, color);
		}
	}

	/**
	 * Gets the color represented by the specified color code
	 *
	 * @param code Code to check
	 * @return Associative {@link org.bukkit.ChatColor} with the given code, or null
	 *         if it doesn't exist
	 */
	public static ChatColor getByChar(char code) {
		return BY_CHAR.get(code);
	}

	/**
	 * Gets the ChatColors used at the end of the given input string.
	 *
	 * @param input Input string to retrieve the colors from.
	 * @return Any remaining ChatColors to pass onto the next line.
	 */
	public static String getLastColors(String input) {
		Validate.notNull(input, "Cannot get last colors from null text");

		String	result	= "";
		int		length	= input.length();

		// Search backwards from the end as it is faster
		for (int index = length - 1; index > -1; index--) {
			char section = input.charAt(index);
			if (section == COLOR_CHAR && index < length - 1) {
				char		c		= input.charAt(index + 1);
				ChatColor	color	= getByChar(c);

				if (color != null) {
					result = color.toString() + result;

					// Once we find a color or reset we can stop searching
					if (color.isColor() || color.equals(RESET)) {
						break;
					}
				}
			}
		}

		return result;
	}

	private final char		code;

	private final boolean	isFormat;

	private final String	toString;

	private ChatColor(char code) {
		this(code, false);
	}

	private ChatColor(char code, boolean isFormat) {
		this.code		= code;
		this.isFormat	= isFormat;
		this.toString	= new String(new char[] { COLOR_CHAR, code });
	}

	/**
	 * Checks if this code is a color code as opposed to a format code.
	 *
	 * @return whether this ChatColor is a color code
	 */
	public boolean isColor() {
		return !isFormat && this != RESET;
	}

	@Override
	public String toString() {
		return toString;
	}
}
