
package ossbuild.extract;

import java.util.HashMap;
import java.util.Map;
import ossbuild.StringUtil;

/**
 * Maintains list of resource collections and their references to other
 * resource collections.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public final class Registry {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final Reference
		ROOT = new Reference(Registry.class.getName())
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public static final Reference getRoot() {
		return ROOT;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public static void visit(final IVisitor visitor) {
		ROOT.visit(visitor, false);
	}

	public static void visit(final IVisitor visitor, final boolean Recursive) {
		ROOT.visit(visitor, Recursive);
	}

	public static final boolean isEmpty() {
		return ROOT.isEmpty();
	}

	public static final int size() {
		return ROOT.size();
	}

	public static final int getSize() {
		return ROOT.getSize();
	}

	public static final Reference add(final String Name, final Resources Resources) {
		final Reference ref = new Reference(Name, Resources);
		if (ROOT.addReference(ref))
			return ref;
		return null;
	}

	public static final Reference add(final String Name, final Resources Resources, final Reference... References) {
		final Reference ref = new Reference(Name, Resources, References);
		if (ROOT.addReference(ref))
			return ref;
		return null;
	}

	public static final boolean remove(final String Name) {
		return ROOT.removeReference(Name);
	}

	public static final boolean clear() {
		return ROOT.clearReferences();
	}

	public static final boolean hasReference(final String Name) {
		return ROOT.references(Name);
	}

	public static final boolean hasReference(final String Name, final boolean Recursive) {
		return ROOT.references(Name, Recursive);
	}

	public static final Reference findReference(final String Name) {
		return ROOT.findReference(Name);
	}

	public static final Reference findReference(final String Name, final boolean Recursive) {
		return ROOT.findReference(Name, Recursive);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Interfaces">
	public static interface IVisitor {
		boolean visit(final Reference Reference);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Classes">
	public static final class Reference {
		//<editor-fold defaultstate="collapsed" desc="Constants">
		public static final Reference[]
			None = null
		;

		public static final Reference[]
			Empty = new Reference[0]
		;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Variables">
		private String name;
		private String title;
		private Resources resources;
		private Map<String, Reference> references;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		public Reference(final String Name) {
			init(Name, Name, Resources.Empty, Reference.Empty);
		}

		public Reference(final String Name, final Resources Resources) {
			init(Name, Name, Resources, Reference.Empty);
		}

		public Reference(final String Name, final Resources Resources, final Reference... References) {
			init(Name, Name, Resources, References);
		}

		public Reference(final String Name, final String Title, final Resources Resources) {
			init(Name, Title, Resources, Reference.Empty);
		}

		public Reference(final String Name, final String Title, final Resources Resources, final Reference... References) {
			init(Name, Title, Resources, References);
		}

		private void init(final String Name, final String Title, final Resources Resources, final Reference[] References) {
			if (StringUtil.isNullOrEmpty(Name))
				throw new NullPointerException("Name must not be empty");
			if (Resources == null)
				throw new NullPointerException("Resources must not be null");
			
			this.name = Name;
			this.title = Title;
			this.resources = Resources;

			if (References == null || References == Empty || References == None || References.length <= 0)
				this.references = null;
			else
				addReferences(References);
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters">
		public String getName() {
			return name;
		}

		public String getTitle() {
			return title;
		}

		public Resources getResources() {
			return resources;
		}

		public boolean isLoaded() {
			return resources.isLoaded();
		}

		public boolean isProcessed() {
			return resources.isProcessed();
		}

		public Reference[] getReferences() {
			if (references == null)
				return Empty;
			return references.values().toArray(new Reference[references.size()]);
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Public Methods">
		public void visit(final IVisitor visitor, final boolean Recursive) {
			if (visitor == null || references == null || references.isEmpty())
				return;
			for(Reference r : references.values()) {
				visitor.visit(r);
				if (Recursive)
					r.visit(visitor, Recursive);
			}
		}

		public boolean isEmpty() {
			return references == null || references.isEmpty();
		}

		public int size() {
			return (references != null ? references.size() : 0);
		}

		public int getSize() {
			return size();
		}

		public boolean addReference(final Reference Reference) {
			return addReferences(Reference);
		}

		public boolean addReferences(final Reference...References) {
			if (References == null || References.length <= 0)
				return true;

			if (references == null)
				references = new HashMap<String, Reference>(5, 0.5f);

			for(Reference r : References)
				if (r != null)
					references.put(r.getName(), r);

			return true;
		}
		
		public boolean removeReference(final Reference Reference) {
			if (Reference == null)
				throw new NullPointerException("Reference must not be null");
			
			return removeReference(Reference.getName());
		}

		public boolean removeReference(final String Name) {
			if (StringUtil.isNullOrEmpty(Name))
				throw new NullPointerException("Name must not be empty");

			if (references == null || references.isEmpty() || !references.containsKey(Name))
				return true;

			references.remove(Name);
			return true;
		}

		public boolean clearReferences() {
			if (references == null || references.isEmpty())
				return true;
			references.clear();
			return true;
		}

		public boolean references(final String Name) {
			return (findReference(Name) != null);
		}

		public boolean references(final String Name, final boolean Recursive) {
			return (findReference(Name, Recursive) != null);
		}

		public Reference findReference(final String Name) {
			return findReference(Name, false);
		}

		public Reference findReference(final String Name, final boolean Recursive) {
			if (StringUtil.isNullOrEmpty(Name))
				return null;

			if (name.equalsIgnoreCase(Name))
				return this;

			if (references == null || references.isEmpty())
				return null;

			if (references.containsKey(Name))
				return references.get(Name);

			if (Recursive) {
				Reference ref;
				for(Map.Entry<String, Reference> e : references.entrySet())
					if ((ref = e.getValue().findReference(e.getKey(), Recursive)) != null)
						return ref;
				return null;
			} else {
				return null;
			}
		}
		//</editor-fold>
	}
	//</editor-fold>
}
