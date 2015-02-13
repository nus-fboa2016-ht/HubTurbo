package util;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import model.TurboLabel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

public class Utility {

	private static final Logger logger = LogManager.getLogger(Utility.class.getName());
	
	public static String stringify(Collection<TurboLabel> labels) {
		return "[" + Joiner.on(", ").join(labels.stream().map(l -> l.logString()).collect(Collectors.toList())) + "]";
	}
	
	public static int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}
	
	public static long localDateTimeToLong(LocalDateTime t) {
		return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	public static LocalDateTime longToLocalDateTime(long epochMilli) {
		Instant instant = new Date(epochMilli).toInstant();
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}
	
	public static Optional<int[]> parseVersionNumberString(String version) {
		// Strip non-digits
		version = version.replaceAll("[^0-9.]+", "");
		
		String[] temp = version.split("\\.");
		try {
            int major = temp.length > 0 ? Integer.parseInt(temp[0]) : 0;
            int minor = temp.length > 1 ? Integer.parseInt(temp[1]) : 0;
            int patch = temp.length > 2 ? Integer.parseInt(temp[2]) : 0;
            return Optional.of(new int[] {major, minor, patch});
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public static String version(int major, int minor, int patch) {
		return String.format("V%d.%d.%d", major, minor, patch);
	}
	
	public static String snakeCaseToCamelCase(String str) {
        Pattern p = Pattern.compile("(^|_)([a-z])" );
        Matcher m = p.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(2).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
	}
	
	public static Rectangle getScreenDimensions() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		return new Rectangle((int) screenSize.getWidth(), (int) screenSize.getHeight());
	}
	
	public static Optional<Rectangle> getUsableScreenDimensions() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			return Optional.of(ge.getMaximumWindowBounds());
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		return Optional.empty();
	}

}
