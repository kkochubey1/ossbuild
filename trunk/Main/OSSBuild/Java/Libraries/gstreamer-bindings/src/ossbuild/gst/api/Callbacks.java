
package ossbuild.gst.api;

import com.sun.jna.Pointer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ossbuild.gst.callbacks.IGWeakNotify;
import static ossbuild.gst.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Callbacks {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static final Object lock = new Object();
	private static Map<Pointer, IGWeakNotify> gWeakReferences = new HashMap<Pointer, IGWeakNotify>(5, 1.0f);
	private static Map<Pointer, Set<ICallback>> callbacks = new HashMap<Pointer, Set<ICallback>>(5, 1.0f);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean register(Pointer ptr, ICallback callback) {
		if (ptr == null || ptr == Pointer.NULL || callback == null)
			return false;
		
		synchronized(lock) {
			if (!gWeakReferences.containsKey(ptr)) {
				IGWeakNotify notify = new IGWeakNotify() {
					@Override
					public void notify(Pointer data, Pointer ptr) {
						//g_object_weak_unref(ptr, this, Pointer.NULL);
						synchronized(lock) {
							unregisterAll(ptr);
						}
					}
				};
				gWeakReferences.put(ptr, notify);
				g_object_weak_ref(ptr, notify, Pointer.NULL);
			}

			Set<ICallback> lst = callbacks.get(ptr);
			if (lst == null)
				callbacks.put(ptr, (lst = new HashSet<ICallback>(1, 1.0f)));

			//If the callback already exists, then add() would return false,
			//but we want to make it appear as though it worked b/c technically
			//it did - it's just that it was already registered -- but the intent
			//that we're now tracking it has succeeded.
			lst.add(callback);
			return true;
		}
	}

	public static boolean unregister(Pointer ptr, ICallback callback) {
		if (ptr == null || ptr == Pointer.NULL || callback == null)
			return false;

		synchronized(lock) {
			Set<ICallback> lst = callbacks.get(ptr);
			if (lst == null)
				return true;
			lst.remove(callback);
			
			//If there's nothing left, then free up the Set used for this pointer's callbacks
			if (lst.isEmpty()) {
				callbacks.remove(ptr);

				//Remove the weak ref
				if (gWeakReferences.containsKey(ptr)) {
					IGWeakNotify notify = gWeakReferences.remove(ptr);
					//g_object_weak_unref(ptr, notify, Pointer.NULL);
				}
			}
			//Unhook
			return true;
		}
	}

	public static boolean unregisterAll(Pointer ptr) {
		if (ptr == null || ptr == Pointer.NULL)
			return false;

		synchronized(lock) {
			Set<ICallback> lst = callbacks.remove(ptr);

			//Remove the weak ref
			if (gWeakReferences.containsKey(ptr)) {
				IGWeakNotify notify = gWeakReferences.remove(ptr);
				//g_object_weak_unref(ptr, notify, Pointer.NULL);
			}

			if (lst == null)
				return true;
			//Unhook all
			return true;
		}
	}
	//</editor-fold>
}
