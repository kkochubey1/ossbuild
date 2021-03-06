// This file was generated by the Gtk# code generator.
// Any changes made will be lost if regenerated.

namespace Gst {

	using System;
	using System.Collections;
	using System.Runtime.InteropServices;

#region Autogenerated code
	public class Buffer : Gst.MiniObject {

		public Buffer(IntPtr raw) : base(raw) {}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern IntPtr gst_buffer_new();

		public Buffer () : base (IntPtr.Zero)
		{
			if (GetType () != typeof (Buffer)) {
				CreateNativeObject ();
				return;
			}
			Raw = gst_buffer_new();
		}

		[StructLayout (LayoutKind.Sequential)]
		struct GstBufferClass {
		}

		static uint class_offset = ((Gst.GLib.GType) typeof (Gst.MiniObject)).GetClassSize ();
		static Hashtable class_structs;

		static GstBufferClass GetClassStruct (Gst.GLib.GType gtype, bool use_cache)
		{
			if (class_structs == null)
				class_structs = new Hashtable ();

			if (use_cache && class_structs.Contains (gtype))
				return (GstBufferClass) class_structs [gtype];
			else {
				IntPtr class_ptr = new IntPtr (gtype.GetClassPtr ().ToInt64 () + class_offset);
				GstBufferClass class_struct = (GstBufferClass) Marshal.PtrToStructure (class_ptr, typeof (GstBufferClass));
				if (use_cache)
					class_structs.Add (gtype, class_struct);
				return class_struct;
			}
		}

		static void OverrideClassStruct (Gst.GLib.GType gtype, GstBufferClass class_struct)
		{
			IntPtr class_ptr = new IntPtr (gtype.GetClassPtr ().ToInt64 () + class_offset);
			Marshal.StructureToPtr (class_struct, class_ptr, false);
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern bool gst_buffer_is_span_fast(IntPtr raw, IntPtr buf2);

		public bool IsSpanFast(Gst.Buffer buf2) {
			bool raw_ret = gst_buffer_is_span_fast(Handle, buf2 == null ? IntPtr.Zero : buf2.Handle);
			bool ret = raw_ret;
			return ret;
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern IntPtr gst_buffer_get_type();

		public static new Gst.GLib.GType GType { 
			get {
				IntPtr raw_ret = gst_buffer_get_type();
				Gst.GLib.GType ret = new Gst.GLib.GType(raw_ret);
				return ret;
			}
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern bool gst_buffer_is_metadata_writable(IntPtr raw);

		public bool IsMetadataWritable { 
			get {
				bool raw_ret = gst_buffer_is_metadata_writable(Handle);
				bool ret = raw_ret;
				return ret;
			}
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern IntPtr gst_buffer_span(IntPtr raw, uint offset, IntPtr buf2, uint len);

		public Gst.Buffer Span(uint offset, Gst.Buffer buf2, uint len) {
			IntPtr raw_ret = gst_buffer_span(Handle, offset, buf2 == null ? IntPtr.Zero : buf2.Handle, len);
			Gst.Buffer ret = Gst.MiniObject.GetObject(raw_ret, true) as Gst.Buffer;
			return ret;
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern void gst_buffer_copy_metadata(IntPtr raw, IntPtr src, int flags);

		public void CopyMetadata(Gst.Buffer src, Gst.BufferCopyFlags flags) {
			gst_buffer_copy_metadata(Handle, src == null ? IntPtr.Zero : src.Handle, (int) flags);
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern IntPtr gst_buffer_create_sub(IntPtr raw, uint offset, uint size);

		public Gst.Buffer CreateSub(uint offset, uint size) {
			IntPtr raw_ret = gst_buffer_create_sub(Handle, offset, size);
			Gst.Buffer ret = Gst.MiniObject.GetObject(raw_ret, true) as Gst.Buffer;
			return ret;
		}

		[DllImport("libgstreamer-0.10.dll", CallingConvention = CallingConvention.Cdecl)]
		static extern IntPtr gst_buffer_merge(IntPtr raw, IntPtr buf2);

		public Gst.Buffer Merge(Gst.Buffer buf2) {
			IntPtr raw_ret = gst_buffer_merge(Handle, buf2 == null ? IntPtr.Zero : buf2.Handle);
			Gst.Buffer ret = Gst.MiniObject.GetObject(raw_ret, true) as Gst.Buffer;
			return ret;
		}

#endregion
#region Customized extensions
#line 1 "Buffer.custom"
[DllImport ("libgstreamer-0.10.dll") ]
static extern IntPtr gst_buffer_try_new_and_alloc (uint size);

public Buffer (Gst.GLib.Value val) : base (val) { }

public Buffer (uint size) {
  IntPtr raw = gst_buffer_try_new_and_alloc (size);
  if (raw == IntPtr.Zero)
    throw new OutOfMemoryException ();
  Raw = raw;
}

public Buffer (IntPtr data, uint size) : this () {
  SetData (data, size);
}

public Buffer (byte[] data) : this () {
  SetData (data);
}

[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static uint gstsharp_gst_buffer_get_data_offset ();
[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static void gstsharp_gst_buffer_set_data (IntPtr handle, IntPtr data, uint size);
[DllImport ("libglib-2.0-0.dll") ]
extern static IntPtr g_try_malloc (int size);

static uint data_offset = gstsharp_gst_buffer_get_data_offset ();
public IntPtr Data {
  get {
    IntPtr raw_ptr;
    unsafe {
      raw_ptr = * ( (IntPtr *) ( ( (byte*) Handle) + data_offset));
    }

    return raw_ptr;
  }
}

public void SetData (IntPtr data, uint size) {
    if (!IsWritable)
      throw new ApplicationException ();

    gstsharp_gst_buffer_set_data (Handle, data, size);
}

public void SetData (byte[] data) {
    if (!IsWritable)
      throw new ApplicationException ();

    IntPtr raw_ptr = g_try_malloc (data.Length);
    if (raw_ptr == IntPtr.Zero)
      throw new OutOfMemoryException ();

    Marshal.Copy (data, 0, raw_ptr, data.Length);
    gstsharp_gst_buffer_set_data (Handle, raw_ptr, (uint) data.Length);
}

public byte[] ToByteArray () {
    byte[] data = new byte[Size];
    Marshal.Copy (Data, data, 0, (int) Size);

    return data;
}

[DllImport ("libgstreamer-0.10.dll") ]
static extern void gst_mini_object_unref (IntPtr raw);

/* FIXME: This is not optimal */
public void MakeMetadataWritable() {
  if (IsMetadataWritable)
    return;

  IntPtr old = Handle;
  IntPtr sub = gst_buffer_create_sub (Handle, 0, Size);
  Raw = sub;
  gst_mini_object_unref (old);
}

[DllImport ("libgstreamer-0.10.dll") ]
static extern IntPtr gst_buffer_get_caps (IntPtr raw);
[DllImport ("libgstreamer-0.10.dll") ]
static extern void gst_buffer_set_caps (IntPtr raw, IntPtr caps);

public Gst.Caps Caps {
  get {
    IntPtr raw_ret = gst_buffer_get_caps (Handle);
    Gst.Caps ret = raw_ret == IntPtr.Zero ? null : (Gst.Caps) Gst.GLib.Opaque.GetOpaque (raw_ret, typeof (Gst.Caps), true);
    return ret;
  } set  {
    if (!IsMetadataWritable)
      throw new ApplicationException ();
    gst_buffer_set_caps (Handle, value == null ? IntPtr.Zero : value.Handle);
  }
}

[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static uint gstsharp_gst_buffer_get_size_offset ();
static uint size_offset = gstsharp_gst_buffer_get_size_offset ();

public uint Size {
  get {
    unsafe {
      uint *raw_ptr = ( (uint*) ( ( (byte*) Handle) + size_offset));
      return *raw_ptr;
    }
  }

  set {
    if (!IsMetadataWritable)
      throw new ApplicationException ();

    unsafe {
      uint *raw_ptr = ( (uint*) ( ( (byte*) Handle) + size_offset));
      *raw_ptr = value;
    }
  }
}


[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static uint gstsharp_gst_buffer_get_timestamp_offset ();
static uint timestamp_offset = gstsharp_gst_buffer_get_timestamp_offset ();

public ulong Timestamp {
  get {
    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + timestamp_offset));
      return *raw_ptr;
    }
  }

  set {
    if (!IsMetadataWritable)
      throw new ApplicationException ();

    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + timestamp_offset));
      *raw_ptr = value;
    }
  }
}

[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static uint gstsharp_gst_buffer_get_duration_offset ();
static uint duration_offset = gstsharp_gst_buffer_get_duration_offset ();

public ulong Duration {
  get {
    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + duration_offset));
      return *raw_ptr;
    }
  }

  set {
    if (!IsMetadataWritable)
      throw new ApplicationException ();

    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + duration_offset));
      *raw_ptr = value;
    }
  }
}

[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static uint gstsharp_gst_buffer_get_offset_offset ();
static uint offset_offset = gstsharp_gst_buffer_get_offset_offset ();

public ulong Offset {
  get {
    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + offset_offset));
      return *raw_ptr;
    }
  }

  set {
    if (!IsMetadataWritable)
      throw new ApplicationException ();

    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + offset_offset));
      *raw_ptr = value;
    }
  }
}

[DllImport ("gstreamersharpglue-0.10.dll") ]
extern static uint gstsharp_gst_buffer_get_offset_end_offset ();
static uint offset_end_offset = gstsharp_gst_buffer_get_offset_end_offset ();

public ulong OffsetEnd {
  get {
    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + offset_end_offset));
      return *raw_ptr;
    }
  }

  set {
    if (!IsMetadataWritable)
      throw new ApplicationException ();

    unsafe {
      ulong *raw_ptr = ( (ulong*) ( ( (byte*) Handle) + offset_end_offset));
      *raw_ptr = value;
    }
  }
}

static Buffer () {
  Gst.GLib.GType.Register (Buffer.GType, typeof (Buffer));
}

#endregion
	}
}
