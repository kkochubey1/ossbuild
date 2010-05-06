
package simple.media;

/**
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum MediaRequestType {
	  Live          ("MediaRequestType.Live",       "Live",            "The requested media is from a live source.")
	, Video         ("MediaRequestType.Video",      "Recording",       "The requested media contains video.")
	, Still         ("MediaRequestType.Still",      "Still",           "The requested media contains a single still image.")
	, AlarmVideo    ("MediaRequestType.AlarmVideo", "Alarm Recording", "The requested media contains video associated with an alarm.")
	, AlarmStill    ("MediaRequestType.AlarmStill", "Alarm Still",     "The requested media contains a single still image associated with an alarm.")
	, TestVideo     ("MediaRequestType.TestVideo",  "Test Video",      "The requested media contains video meant for testing.")
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private String i18nID;
	private String title;
	private String description;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	private MediaRequestType(String I18NID, String Title, String Description) {
		this.i18nID = I18NID;
		this.title = Title;
		this.description = Description;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public String getDescription() {
		return description;
	}

	public String getI18NID() {
		return i18nID;
	}

	public String getTitle() {
		return title;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public boolean isAlarm() {
		switch(this) {
			case AlarmVideo:
			case AlarmStill:
				return true;
			default:
				return false;
		}
	}

	public boolean isVideo() {
		switch(this) {
			case Video:
			case AlarmVideo:
			case TestVideo:
				return true;
			default:
				return false;
		}
	}

	public boolean isStill() {
		switch(this) {
			case Still:
			case AlarmStill:
				return true;
			default:
				return false;
		}
	}

	public boolean isTest() {
		switch(this) {
			case TestVideo:
				return true;
			default:
				return false;
		}
	}

	public boolean isLive() {
		switch(this) {
			case Live:
				return true;
			default:
				return false;
		}
	}
	//</editor-fold>
}
