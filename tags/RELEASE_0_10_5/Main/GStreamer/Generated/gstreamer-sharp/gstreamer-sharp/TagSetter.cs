// This file was generated by the Gtk# code generator.
// Any changes made will be lost if regenerated.

namespace Gst {

	using System;

#region Autogenerated code
	public partial interface TagSetter : Gst.GLib.IWrapper {

		Gst.TagList TagList { 
			get;
		}
		void AddTag(Gst.TagList list, Gst.TagMergeMode mode);
		void ResetTags();
		Gst.TagMergeMode TagMergeMode { 
			get; set;
		}
		void AddTag(Gst.TagMergeMode mode, string tag, Gst.GLib.Value value);
	}

	[Gst.GLib.GInterface (typeof (TagSetterAdapter))]
	public partial interface TagSetterImplementor : Gst.GLib.IWrapper {

	}
#endregion
}