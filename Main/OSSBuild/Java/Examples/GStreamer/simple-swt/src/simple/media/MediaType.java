
package simple.media;

/**
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum MediaType {
	  Unknown   ("MediaType.Unknown", "Unknown", "It is unknown if the media contains video, audio, or an image.")
	  
	, Video     ("MediaType.Video",   "Video",   "The media contains video.")
	, Audio     ("MediaType.Audio",   "Audio",   "The media contains audio.")
	, Image     ("MediaType.Image",   "Image",   "The media contains a single image.")
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private String i18nID;
	private String title;
	private String description;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	private MediaType(String I18NID, String Title, String Description) {
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
}
