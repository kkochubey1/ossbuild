
package simple.swt;

import ossbuild.StringUtil;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum Scheme {
	  Unknown   ("Unknown",                            ""     )

	, File      ("Local File",                         "file" )
	, Jar       ("Java Jar File",                      "jar"  )
	, HTTP      ("HyperText Transfer Protocol",        "http" )
	, HTTPS     ("Secure HyperText Transfer Protocol", "https")
	, RTP       ("Real Time Protocol",                 "rtp"  )
	, RTSP      ("Real Time Streaming Protocol",       "rtsp" )
	, UDP       ("UDP",                                "udp"  )
	, TCP       ("Transport Control Protocol",         "tcp"  )
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private String title;
	private String[] validPrefixes;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	Scheme(final String title, final String... validPrefixes) {
		this.title = title;
		this.validPrefixes = validPrefixes;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public String getTitle() {
		return title;
	}

	public String[] getValidPrefixes() {
		return validPrefixes;
	}

	public boolean isUnknown() {
		return this == Unknown;
	}

	public static boolean isUnknown(final Scheme scheme) {
		return scheme == Scheme.Unknown;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public static Scheme fromPrefix(final String prefix) {
		if (StringUtil.isNullOrEmpty(prefix))
			return Scheme.Unknown;
		for(Scheme s : Scheme.values())
			for(String p : s.validPrefixes)
				if (prefix.startsWith(p))
					return s;
		return Scheme.Unknown;
	}
	//</editor-fold>
}
