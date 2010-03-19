
package ossbuild.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import java.util.Enumeration;
import java.util.HashSet;
import ossbuild.extract.ResourceException;

/**
 * Convenience class for working with serial ports.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SerialPorts {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final long
		DEFAULT_OPEN_TIMEOUT = 50L
	;

	public static final String
		OWNER = SerialPorts.class.getName()
	;

	public static final CommPortIdentifier[]
		Empty = new CommPortIdentifier[0]
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	/**
	 * Returns an array of all available serial ports on this platform.
	 */
	public static CommPortIdentifier[] allSerialPorts() {
		final Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		if (thePorts == null || !thePorts.hasMoreElements())
			return Empty;

		final HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
		while (thePorts.hasMoreElements()) {
			final CommPortIdentifier com = (CommPortIdentifier)thePorts.nextElement();
			switch (com.getPortType()) {
				case CommPortIdentifier.PORT_SERIAL:
					h.add(com);
					break;
			}
		}
		return h.toArray(new CommPortIdentifier[h.size()]);
	}

	/**
	 * Returns an array of available serial ports (ones that haven't been opened
	 * by another application).
	 */
	public static CommPortIdentifier[] availableSerialPorts() {
		final Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		if (thePorts == null || !thePorts.hasMoreElements())
			return Empty;

		final HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
		while (thePorts.hasMoreElements()) {
			final CommPortIdentifier com = (CommPortIdentifier)thePorts.nextElement();
			switch (com.getPortType()) {
				case CommPortIdentifier.PORT_SERIAL:
					try {
						final SerialPort serial = (SerialPort)com.open(OWNER, 50);
						serial.close();
						h.add(com);
					} catch(PortInUseException e) {
					} catch(Throwable t) {
						throw new ResourceException("Failed to open serial port", t);
					}
					break;
			}
		}
		return h.toArray(new CommPortIdentifier[h.size()]);
	}

	/**
	 * Opens a serial port using the provided identifier.
	 */
	public static SerialPort open(final CommPortIdentifier id) {
		return open(id, DEFAULT_OPEN_TIMEOUT);
	}

	/**
	 * Opens a serial port using the provided identifier.
	 */
	public static SerialPort open(final CommPortIdentifier id, final long openTimeout) {
		if (id == null || id.getPortType() != CommPortIdentifier.PORT_SERIAL)
			return null;
		try {
			return (SerialPort)id.open(OWNER, (int)openTimeout);
		} catch(PortInUseException e) {
			return null;
		}
	}
	//</editor-fold>
}
