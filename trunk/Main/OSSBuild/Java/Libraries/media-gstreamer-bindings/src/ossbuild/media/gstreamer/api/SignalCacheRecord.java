
package ossbuild.media.gstreamer.api;

import ossbuild.StringUtil;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SignalCacheRecord implements ISignalCacheRecord {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private Class<? extends ISignal> cls;
	private String signalName;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public SignalCacheRecord(Class<? extends ISignal> cls, String signalName) {
		this.cls = cls;
		this.signalName = signalName;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public Class<? extends ISignal> getSignalClass() {
		return cls;
	}

	@Override
	public String getSignalName() {
		return signalName;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static SignalCacheRecord from(Class<? extends ISignal> cls) {
		if (cls == null)
			throw new NullPointerException("cls cannot be empty");

		Signal s = Utils.findAnnotation(cls, Signal.class);
		if (s == null)
			throw new RuntimeException("Missing signal annotation for " + cls.getName());

		return new SignalCacheRecord(cls, StringUtil.isNullOrEmpty(s.detail()) ? s.name() : s.name() + "::" + s.detail());
	}
	//</editor-fold>
}
