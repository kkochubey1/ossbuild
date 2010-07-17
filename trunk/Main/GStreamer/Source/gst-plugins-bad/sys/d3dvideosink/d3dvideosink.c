/* GStreamer
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "d3dvideosink.h"

#include <gst/video/video.h>
#include <gst/interfaces/xoverlay.h>

#include "windows.h"

#define IDT_DEVICELOST          1
#define WM_D3D_INIT_DEVICELOST  WM_USER + 1
#define WM_D3D_DEVICELOST       WM_USER + 2
#define WM_D3D_END_DEVICELOST   WM_USER + 3

/* Provide access to data that will be shared among all instantiations of this element */
#define GST_D3DVIDEOSINK_SHARED_D3D_LOCK	      g_static_mutex_lock (&shared_d3d_lock);
#define GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK      g_static_mutex_unlock (&shared_d3d_lock);
#define GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK	  g_static_mutex_lock (&shared_d3d_dev_lock);
#define GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK  g_static_mutex_unlock (&shared_d3d_dev_lock);
typedef struct _GstD3DVideoSinkShared GstD3DVideoSinkShared;
struct _GstD3DVideoSinkShared
{
  GList* element_list;
  gint32 element_count;

  gboolean device_lost;
  UINT_PTR device_lost_timer;

  HWND hidden_window_id;
  HANDLE hidden_window_created_signal;
  GThread* hidden_window_thread;

  LPDIRECT3D9 d3d;
  LPDIRECT3DDEVICE9 d3ddev;
  D3DCAPS9 d3dcaps;
  D3DFORMAT d3dformat;
  D3DFORMAT d3dfourcc;
  D3DFORMAT d3dstencilformat;
  gboolean d3dEnableAutoDepthStencil;
};
/* Holds our shared information */
static GstD3DVideoSinkShared shared;
/* Define a shared lock to synchronize the creation/destruction of the d3d device */
static GStaticMutex shared_d3d_lock = G_STATIC_MUTEX_INIT;
static GStaticMutex shared_d3d_dev_lock = G_STATIC_MUTEX_INIT;

GST_DEBUG_CATEGORY (d3dvideosink_debug);
#define GST_CAT_DEFAULT d3dvideosink_debug

/* TODO: Support RGB! */
static GstStaticPadTemplate sink_template = 
    GST_STATIC_PAD_TEMPLATE(
      "sink",
      GST_PAD_SINK,
      GST_PAD_ALWAYS,
      GST_STATIC_CAPS (
        GST_VIDEO_CAPS_YUV("{ YUY2, UYVY, YUVY }")
      )
    );

static void gst_d3dvideosink_init_interfaces (GType type);

GST_BOILERPLATE_FULL (GstD3DVideoSink, gst_d3dvideosink, GstVideoSink,
    GST_TYPE_VIDEO_SINK, gst_d3dvideosink_init_interfaces);

enum
{
  PROP_0
  //, PROP_KEEP_ASPECT_RATIO
  , PROP_LAST
};

/* GObject methods */
static void gst_d3dvideosink_finalize (GObject *gobject);
static void gst_d3dvideosink_set_property (GObject *object, guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_d3dvideosink_get_property (GObject *object, guint prop_id, GValue *value, GParamSpec *pspec);

/* GstElement methods */
static GstStateChangeReturn gst_d3dvideosink_change_state (GstElement *element, GstStateChange transition);

/* GstBaseSink methods */
static gboolean gst_d3dvideosink_start (GstBaseSink *bsink);
static gboolean gst_d3dvideosink_stop (GstBaseSink *bsink);
static gboolean gst_d3dvideosink_unlock (GstBaseSink *bsink);
static gboolean gst_d3dvideosink_unlock_stop (GstBaseSink *bsink);
static gboolean gst_d3dvideosink_set_caps (GstBaseSink *bsink, GstCaps *caps);
static GstCaps *gst_d3dvideosink_get_caps (GstBaseSink *bsink);
static GstFlowReturn gst_d3dvideosink_show_frame (GstVideoSink *sink, GstBuffer *buffer);

/* GstXOverlay methods */
static void gst_d3dvideosink_set_window_id (GstXOverlay *overlay, ULONG window_id);
static void gst_d3dvideosink_expose (GstXOverlay *overlay);

/* WndProc methods */
static void gst_d3dvideosink_wnd_proc (GstD3DVideoSink *sink, HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

/* Paint/update methods */
static void gst_d3dvideosink_update (GstBaseSink *bsink);
static gboolean gst_d3dvideosink_refresh (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_update_all (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_refresh_all (GstD3DVideoSink *sink);

/* Misc methods */
static void gst_d3dvideosink_remove_window_for_renderer (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_initialize_direct3d (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_initialize_d3d_device (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_initialize_swap_chain (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_resize_swap_chain (GstD3DVideoSink *sink, gint width, gint height);
static gboolean gst_d3dvideosink_notify_device_lost (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_notify_device_reset (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_device_lost (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_release_swap_chain (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_release_d3d_device (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_release_direct3d (GstD3DVideoSink *sink);
static gboolean gst_d3dvideosink_window_size (GstD3DVideoSink *sink, gint *width, gint *height);
static gboolean gst_d3dvideosink_shared_hidden_window_thread (GstD3DVideoSink * sink);
LRESULT APIENTRY SharedHiddenWndProc (HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

/* TODO: event, preroll, buffer_alloc? 
 * buffer_alloc won't generally be all that useful because the renderers require a 
 * different stride to GStreamer's implicit values. 
 */

static gboolean
gst_d3dvideosink_interface_supported (GstImplementsInterface * iface,
    GType type)
{
  return (type == GST_TYPE_X_OVERLAY);
}

static void
gst_d3dvideosink_interface_init (GstImplementsInterfaceClass * klass)
{
  klass->supported = gst_d3dvideosink_interface_supported;
}


static void
gst_d3dvideosink_xoverlay_interface_init (GstXOverlayClass * iface)
{
  iface->set_xwindow_id = gst_d3dvideosink_set_window_id;
  iface->expose = gst_d3dvideosink_expose;
}

static void
gst_d3dvideosink_init_interfaces (GType type)
{
  static const GInterfaceInfo iface_info = {
    (GInterfaceInitFunc) gst_d3dvideosink_interface_init,
    NULL,
    NULL
  };

  static const GInterfaceInfo xoverlay_info = {
    (GInterfaceInitFunc) gst_d3dvideosink_xoverlay_interface_init,
    NULL,
    NULL
  };

  g_type_add_interface_static (type, GST_TYPE_IMPLEMENTS_INTERFACE,
      &iface_info);
  g_type_add_interface_static (type, GST_TYPE_X_OVERLAY, &xoverlay_info);

  GST_DEBUG_CATEGORY_INIT (d3dvideosink_debug, "d3dvideosink", 0, \
      "Direct3D video sink");
}
static void
gst_d3dvideosink_base_init (gpointer klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_template));

  gst_element_class_set_details_simple (element_class, "Direct3D video sink",
      "Sink/Video",
      "Display data using a Direct3D video renderer",
      "David Hoyt <dhoyt@hoytsoft.org>");
}

static void
gst_d3dvideosink_class_init (GstD3DVideoSinkClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSinkClass *gstbasesink_class;
  GstVideoSinkClass *gstvideosink_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;
  gstbasesink_class = (GstBaseSinkClass *) klass;
  gstvideosink_class = (GstVideoSinkClass *) klass;

  gobject_class->finalize = GST_DEBUG_FUNCPTR (gst_d3dvideosink_finalize);
  gobject_class->set_property = GST_DEBUG_FUNCPTR (gst_d3dvideosink_set_property);
  gobject_class->get_property = GST_DEBUG_FUNCPTR (gst_d3dvideosink_get_property);

  gstelement_class->change_state = GST_DEBUG_FUNCPTR (gst_d3dvideosink_change_state);

  gstbasesink_class->get_caps = GST_DEBUG_FUNCPTR (gst_d3dvideosink_get_caps);
  gstbasesink_class->set_caps = GST_DEBUG_FUNCPTR (gst_d3dvideosink_set_caps);
  gstbasesink_class->start = GST_DEBUG_FUNCPTR (gst_d3dvideosink_start);
  gstbasesink_class->stop = GST_DEBUG_FUNCPTR (gst_d3dvideosink_stop);
  gstbasesink_class->unlock = GST_DEBUG_FUNCPTR (gst_d3dvideosink_unlock);
  gstbasesink_class->unlock_stop = GST_DEBUG_FUNCPTR (gst_d3dvideosink_unlock_stop);

  gstvideosink_class->show_frame = GST_DEBUG_FUNCPTR (gst_d3dvideosink_show_frame);

  /* Add properties */
  //g_object_class_install_property (G_OBJECT_CLASS (klass),
  //    PROP_KEEP_ASPECT_RATIO, g_param_spec_boolean ("force-aspect-ratio",
  //        "Force aspect ratio",
  //        "When enabled, scaling will respect original aspect ratio", FALSE,
  //        (GParamFlags)G_PARAM_READWRITE));
}

static void
gst_d3dvideosink_clear (GstD3DVideoSink *sink)
{
  sink->keep_aspect_ratio = FALSE;

  sink->window_closed = FALSE;
  sink->window_id = NULL;
  sink->is_new_window = FALSE;
}

static void
gst_d3dvideosink_init (GstD3DVideoSink * sink, GstD3DVideoSinkClass * klass)
{
  gst_d3dvideosink_clear (sink);

  sink->d3d_swap_chain_lock = g_mutex_new();

  /* TODO: Copied from GstVideoSink; should we use that as base class? */
  /* 20ms is more than enough, 80-130ms is noticable */
  gst_base_sink_set_max_lateness (GST_BASE_SINK (sink), 50 * GST_MSECOND);
  gst_base_sink_set_qos_enabled (GST_BASE_SINK (sink), TRUE);
}

static void
gst_d3dvideosink_finalize (GObject * gobject)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (gobject);

  g_mutex_free (sink->d3d_swap_chain_lock);
  sink->d3d_swap_chain_lock = NULL;

  G_OBJECT_CLASS (parent_class)->finalize (gobject);
}

static void
gst_d3dvideosink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (object);

  switch (prop_id) {
    //case PROP_KEEP_ASPECT_RATIO:
    //  sink->keep_aspect_ratio = g_value_get_boolean (value);
    //  break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_d3dvideosink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (object);

  switch (prop_id) {
    //case PROP_KEEP_ASPECT_RATIO:
    //  g_value_set_boolean (value, sink->keep_aspect_ratio);
    //  break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstCaps *
gst_d3dvideosink_get_caps (GstBaseSink * basesink)
{
  //GstD3DVideoSink *sink = GST_D3DVIDEOSINK (basesink);
  //if (!sink)
  //  return NULL;
  //return sink->caps;
  return NULL;
}

static void 
gst_d3dvideosink_close_window (GstD3DVideoSink * sink) 
{
  if (!sink || !sink->window_id)
    return;
  
  if (!sink->is_new_window) {
    gst_d3dvideosink_remove_window_for_renderer(sink);
    return;
  }
  
  SendMessage (sink->window_id, WM_CLOSE, (WPARAM)NULL, (WPARAM)NULL);
  g_thread_join(sink->window_thread);
  sink->is_new_window = FALSE;
}

static gboolean  
gst_d3dvideosink_create_shared_hidden_window (GstD3DVideoSink * sink) 
{
  GST_DEBUG("Creating Direct3D hidden window");

  shared.hidden_window_created_signal = CreateSemaphore (NULL, 0, 1, NULL);
  if (shared.hidden_window_created_signal == NULL)
    goto failed;

  shared.hidden_window_thread = g_thread_create ((GThreadFunc)gst_d3dvideosink_shared_hidden_window_thread, sink, TRUE, NULL);

  /* wait maximum 60 seconds for window to be created */
  if (WaitForSingleObject (shared.hidden_window_created_signal, 60000) != WAIT_OBJECT_0)
    goto failed;

  CloseHandle (shared.hidden_window_created_signal);

  GST_DEBUG("Successfully created Direct3D hidden window, handle: %d", shared.hidden_window_id);

  return (shared.hidden_window_id != NULL);

failed:
  CloseHandle (shared.hidden_window_created_signal);
  GST_ELEMENT_ERROR (sink, RESOURCE, WRITE, ("Error creating Direct3D hidden window"), (NULL));
  return FALSE;
}

static gboolean  
gst_d3dvideosink_shared_hidden_window_thread (GstD3DVideoSink * sink) 
{
  WNDCLASS WndClass;
  HWND hWnd;
  MSG msg;

  memset (&WndClass, 0, sizeof (WNDCLASS));
  WndClass.hInstance = GetModuleHandle(NULL);
  WndClass.lpszClassName = L"GST-Shared-Hidden-D3DSink";
  WndClass.lpfnWndProc = SharedHiddenWndProc;
  if (!RegisterClass (&WndClass)) {
    GST_ERROR("Unable to register Direct3D hidden window class");
    return FALSE;
  }

  hWnd = CreateWindowEx (
    0, WndClass.lpszClassName, 
    L"GStreamer Direct3D hidden window",
    WS_POPUP, 
    0, 0, 1, 1, 
    HWND_MESSAGE, 
    NULL,
    WndClass.hInstance, 
    sink
  );

  if (hWnd == NULL) {
    GST_ERROR_OBJECT (sink, "Failed to create Direct3D hidden window");
    goto error;
  }

  GST_DEBUG("Direct3D hidden window handle: %d", hWnd);

  shared.hidden_window_id = hWnd;
  shared.device_lost_timer = 0;

  ReleaseSemaphore (shared.hidden_window_created_signal, 1, NULL);

  GST_DEBUG("Entering Direct3D hidden window message loop");

  /* start message loop processing */
  while(TRUE)
  {
    while(GetMessage(&msg, NULL, 0, 0))
    {
      TranslateMessage(&msg);
      DispatchMessage(&msg);
    }
    
    if(msg.message == WM_QUIT || msg.message == WM_CLOSE)
      break;
  }

  GST_DEBUG("Leaving Direct3D hidden window message loop");

success:
  /* Kill the device lost timer if it's running */
  if (shared.device_lost_timer != 0)
    KillTimer(hWnd, shared.device_lost_timer);
  UnregisterClass(WndClass.lpszClassName, WndClass.hInstance);

  shared.device_lost_timer = 0;
  return TRUE;

error:
  /* Kill the device lost timer if it's running */
  if (shared.device_lost_timer != 0)
    KillTimer(hWnd, shared.device_lost_timer);
  if (hWnd)
    DestroyWindow(hWnd);
  UnregisterClass(WndClass.lpszClassName, WndClass.hInstance);

  shared.hidden_window_id = NULL;
  shared.device_lost_timer = 0;

  ReleaseSemaphore (shared.hidden_window_created_signal, 1, NULL);
  return FALSE;
}

LRESULT APIENTRY 
SharedHiddenWndProc (HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  GstD3DVideoSink *sink;

  if (message == WM_CREATE) 
  {
    /* lParam holds a pointer to a CREATESTRUCT instance which in turn holds the parameter used when creating the window. */
    sink = (GstD3DVideoSink *)((LPCREATESTRUCT)lParam)->lpCreateParams;
    
    /* In our case, this is a pointer to the sink. So we immediately attach it for use in subsequent calls. */
    SetWindowLongPtr (hWnd, GWLP_USERDATA, (LONG)sink);
  }

  sink = (GstD3DVideoSink *)GetWindowLongPtr (hWnd, GWLP_USERDATA);

  switch (message) 
  {
    case WM_D3D_INIT_DEVICELOST:
      {
        if (!shared.device_lost) {
          //GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
          //GST_D3DVIDEOSINK_SHARED_D3D_LOCK

          shared.device_lost = TRUE;

          /* Handle device lost by creating a timer and posting WM_D3D_DEVICELOST twice a second */
          /* Create a timer to periodically check the d3d device and attempt to recreate it */
          shared.device_lost_timer = SetTimer(hWnd, IDT_DEVICELOST, 500, NULL);

          /* Try it once immediately */
          SendMessage(hWnd, WM_D3D_DEVICELOST, 0, 0);
        }
        break;
      }
    case WM_TIMER:
      {
        /* Did we receive a message to check if the device is available again? */
        if (wParam == IDT_DEVICELOST) {
          /* This will synchronously call SharedHiddenWndProc() because this thread is the one that created the window. */
          SendMessage(hWnd, WM_D3D_DEVICELOST, 0, 0);
          return 0;
        }
        break;
      }
    case WM_D3D_DEVICELOST:
      {
        gst_d3dvideosink_device_lost(sink);
        break;
      }
    case WM_D3D_END_DEVICELOST:
      {
        if (shared.device_lost) {
          /* gst_d3dvideosink_notify_device_reset() sends this message. */
          if (shared.device_lost_timer != 0)
            KillTimer(hWnd, shared.device_lost_timer);
          
          shared.device_lost_timer = 0;
          shared.device_lost = FALSE;

          /* Refresh the video with the last buffer */
          gst_d3dvideosink_update_all(sink);

          /* Then redraw just in case we don't have a last buffer */
          gst_d3dvideosink_refresh_all(sink);

          //GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
          //GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
        }
        break;
      }
    case WM_DESTROY:
      {
        PostQuitMessage(0);
        return 0;
      }
  }

  return DefWindowProc (hWnd, message, wParam, lParam);
}

static void 
gst_d3dvideosink_close_shared_hidden_window (GstD3DVideoSink * sink) 
{
  if (!shared.hidden_window_id)
    return;

  SendMessage (shared.hidden_window_id, WM_CLOSE, (WPARAM)NULL, (WPARAM)NULL);
  if (shared.hidden_window_thread) {
    g_thread_join(shared.hidden_window_thread);
    shared.hidden_window_thread = NULL;
  }
  shared.hidden_window_id = NULL;

  GST_DEBUG("Successfully closed Direct3D hidden window");
}

/* WNDPROC for application-supplied windows */
LRESULT APIENTRY WndProcHook (HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  /* Handle certain actions specially on the window passed to us.
   * Then forward back to the original window.
   */
  GstD3DVideoSink *sink = (GstD3DVideoSink *)GetProp (hWnd, L"GstD3DVideoSink");
  
  /* Check it */
  gst_d3dvideosink_wnd_proc (sink, hWnd, message, wParam, lParam);

  switch (message) 
  {
    case WM_ERASEBKGND:
      return TRUE;
  }
  return CallWindowProc (sink->prevWndProc, hWnd, message, wParam, lParam);
}

/* WndProc for our default window, if the application didn't supply one */
LRESULT APIENTRY 
WndProc (HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  GstD3DVideoSink *sink;

  if (message == WM_CREATE) 
  {
    /* lParam holds a pointer to a CREATESTRUCT instance which in turn holds the parameter used when creating the window. */
    GstD3DVideoSink *sink = (GstD3DVideoSink *)((LPCREATESTRUCT)lParam)->lpCreateParams;
    
    /* In our case, this is a pointer to the sink. So we immediately attach it for use in subsequent calls. */
    SetWindowLongPtr (hWnd, GWLP_USERDATA, (LONG)sink);

    /* signal application we created a window */
    gst_x_overlay_got_xwindow_id (GST_X_OVERLAY (sink), (gulong)hWnd);
  }

  
  sink = (GstD3DVideoSink *)GetWindowLongPtr (hWnd, GWLP_USERDATA);
  gst_d3dvideosink_wnd_proc (sink, hWnd, message, wParam, lParam);

  switch (message) 
  {
    case WM_ERASEBKGND:
      return TRUE;

    case WM_DESTROY:
      {
        PostQuitMessage(0);
        return 0;
      }
  }

  return DefWindowProc (hWnd, message, wParam, lParam);
}

static void 
gst_d3dvideosink_wnd_proc(GstD3DVideoSink *sink, HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  switch (message) 
  {
    case WM_PAINT:
      {
        gst_d3dvideosink_refresh(sink);
        break;
      }
    case WM_SIZE:
      {
        gint width;
        gint height;
        gst_d3dvideosink_window_size(sink, &width, &height);
        gst_d3dvideosink_resize_swap_chain(sink, width, height);
        gst_d3dvideosink_refresh(sink);
        //gst_d3dvideosink_resize_swap_chain(sink, MAX(1, ABS(LOWORD(lParam))), MAX(1, ABS(HIWORD(lParam))));
        break;
      }
    case WM_CLOSE:
    case WM_DESTROY:
      {
        sink->window_closed = TRUE;
        GST_ELEMENT_ERROR (sink, RESOURCE, NOT_FOUND, ("Output window was closed"), (NULL));
        break;
      }
  }
}

static gpointer
gst_d3dvideosink_window_thread (GstD3DVideoSink * sink)
{
  WNDCLASS WndClass;
  int width, height;
  int offx, offy;
  DWORD exstyle, style;
  HWND video_window;
  RECT rect;
  int screenwidth;
  int screenheight;
  MSG msg;

  memset (&WndClass, 0, sizeof(WNDCLASS));
  WndClass.style = CS_OWNDC | CS_HREDRAW | CS_VREDRAW;
  WndClass.hInstance = GetModuleHandle(NULL);
  WndClass.lpszClassName = L"GST-D3DSink";
  WndClass.hbrBackground = (HBRUSH) GetStockObject(BLACK_BRUSH);
  WndClass.hCursor = LoadCursor(NULL, IDC_ARROW);
  WndClass.hIcon = LoadIcon(NULL, IDI_APPLICATION);
  WndClass.cbClsExtra = 0;
  WndClass.cbWndExtra = 0;
  WndClass.lpfnWndProc = WndProc;
  RegisterClass (&WndClass);

  /* By default, create a normal top-level window, the size of the video. */

  /* targetWidth is the aspect-ratio-corrected size of the video. */
  /* GetSystemMetrics() returns the width of the dialog's border (doubled b/c of left and right borders). */
  width = sink->targetWidth + GetSystemMetrics (SM_CXSIZEFRAME) * 2;
  height = sink->targetHeight + GetSystemMetrics (SM_CYCAPTION) + (GetSystemMetrics (SM_CYSIZEFRAME) * 2);

  SystemParametersInfo(SPI_GETWORKAREA, (UINT)NULL, &rect, 0);
  screenwidth = rect.right - rect.left;
  screenheight = rect.bottom - rect.top;
  offx = rect.left;
  offy = rect.top;

  /* Make it fit into the screen without changing the aspect ratio. */
  if (width > screenwidth) {
    double ratio = (double)screenwidth/(double)width;
    width = screenwidth;
    height = (int)(height * ratio);
  }

  if (height > screenheight) {
    double ratio = (double)screenheight/(double)height;
    height = screenheight;
    width = (int)(width * ratio);
  }

  style = WS_OVERLAPPEDWINDOW; /* Normal top-level window */
  exstyle = 0;

  video_window = CreateWindowEx (
    exstyle, L"GST-D3DSink",
    L"GStreamer Direct3D sink default window",
    style, 
    offx, 
    offy, 
    width, 
    height, 
    NULL, 
    NULL,
    WndClass.hInstance, 
    sink
  );

  if (video_window == NULL) {
    GST_ERROR_OBJECT (sink, "Failed to create window");
    return NULL;
  }

  sink->is_new_window = TRUE;
  sink->window_id = video_window;

  /* Now show the window, as appropriate */
  ShowWindow (video_window, SW_SHOWNORMAL);

  /* Trigger the initial paint of the window */
  UpdateWindow (video_window);

  ReleaseSemaphore (sink->window_created_signal, 1, NULL);

  /* start message loop processing our default window messages */
  while(TRUE)
  {
    //while(PeekMessage(&msg, NULL, 0, 0, PM_REMOVE))
    while(GetMessage(&msg, NULL, 0, 0))
    {
      TranslateMessage(&msg);
      DispatchMessage(&msg);
    }
    
    if(msg.message == WM_QUIT || msg.message == WM_CLOSE)
      break;
  }

  UnregisterClass(WndClass.lpszClassName, WndClass.hInstance);
  sink->window_id = NULL;
  return NULL;

destroy_window:
  if (video_window) {
    DestroyWindow(video_window);
    UnregisterClass(WndClass.lpszClassName, WndClass.hInstance);
  }
  sink->window_id = NULL;
  ReleaseSemaphore (sink->window_created_signal, 1, NULL);
  return NULL;
}

static gboolean
gst_d3dvideosink_create_default_window (GstD3DVideoSink * sink)
{
  if (shared.device_lost)
    return FALSE;

  sink->window_created_signal = CreateSemaphore (NULL, 0, 1, NULL);
  if (sink->window_created_signal == NULL)
    goto failed;

  sink->window_thread = g_thread_create ((GThreadFunc)gst_d3dvideosink_window_thread, sink, TRUE, NULL);

  /* wait maximum 10 seconds for window to be created */
  if (WaitForSingleObject (sink->window_created_signal, 10000) != WAIT_OBJECT_0)
    goto failed;

  CloseHandle (sink->window_created_signal);
  return (sink->window_id != NULL);

failed:
  CloseHandle (sink->window_created_signal);
  GST_ELEMENT_ERROR (sink, RESOURCE, WRITE,
      ("Error creating our default window"), (NULL));
  return FALSE;
}

static void gst_d3dvideosink_set_window_id (GstXOverlay * overlay, ULONG window_id)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (overlay);
  HWND hWnd = (HWND)window_id;

  if (hWnd == sink->window_id) {
    GST_DEBUG_OBJECT (sink, "Window already set");
    return;
  }

  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SWAP_CHAIN_LOCK(sink);
  {
    /* If we're already playing/paused, then we need to lock the swap chain, and recreate it with the new window. */
    gst_d3dvideosink_release_swap_chain(sink);

    /* Close our existing window if there is one */
    gst_d3dvideosink_close_window(sink);

    /* Save our window id */
    sink->window_id = hWnd;
  }

success:
  GST_DEBUG("Direct3D window id succesfully changed for sink %d to %d", sink, hWnd);
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return;
error:
  GST_DEBUG("Error attempting to change the window id for sink %d to %d", sink, hWnd);
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return;
}

static void gst_d3dvideosink_set_window_for_renderer (GstD3DVideoSink *sink)
{
  WNDPROC currWndProc;

  /* Application has requested a specific window ID */
  sink->is_new_window = FALSE;
  currWndProc = (WNDPROC)GetWindowLongPtr(sink->window_id, GWL_WNDPROC);
  if (sink->prevWndProc != currWndProc && currWndProc != WndProcHook)
    sink->prevWndProc = (WNDPROC)SetWindowLongPtr(sink->window_id, GWL_WNDPROC, (LONG_PTR)WndProcHook);

  GST_DEBUG_OBJECT(sink, "Set wndproc to %p from %p", WndProcHook, sink->prevWndProc);

  /* Allows us to pick up the video sink inside the msg handler */
  SetProp(sink->window_id, L"GstD3DVideoSink", sink);

  /* This causes the new WNDPROC to become active */
  SetWindowPos(sink->window_id, 0, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);

  GST_DEBUG_OBJECT(sink, "Set renderer window to %x", sink->window_id);
  
  sink->is_new_window = FALSE;
}

static void gst_d3dvideosink_remove_window_for_renderer (GstD3DVideoSink *sink)
{
  GST_DEBUG("Removing rendering window hook");
  if (!sink->is_new_window && sink->window_id) {
    WNDPROC currWndProc;

    /* Retrieve current msg handler */
    currWndProc = (WNDPROC)GetWindowLongPtr(sink->window_id, GWL_WNDPROC);

    /* Return control of application window */
    if (sink->prevWndProc != NULL && currWndProc == WndProcHook)
      SetWindowLongPtr(sink->window_id, GWL_WNDPROC, (LONG_PTR)sink->prevWndProc);

    /* Remove the property associating our sink with the window */
    RemoveProp (sink->window_id, L"GstDShowVideoSink");

    /* This causes the old WNDPROC to become active */
    SetWindowPos(sink->window_id, 0, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);

    sink->prevWndProc = NULL;
    sink->window_id = NULL;
    sink->is_new_window = FALSE;
  }
}

static void
gst_d3dvideosink_prepare_window (GstD3DVideoSink *sink)
{
  /* Give the app a last chance to supply a window id */
  if (!sink->window_id) {
    gst_x_overlay_prepare_xwindow_id (GST_X_OVERLAY (sink));
  }

  /* If the app supplied one, use it. Otherwise, go ahead
   * and create (and use) our own window */
  if (sink->window_id) {
    gst_d3dvideosink_set_window_for_renderer (sink);
  } else {
    gst_d3dvideosink_create_default_window (sink);
  }

  gst_d3dvideosink_initialize_swap_chain(sink);
}

static GstStateChangeReturn
gst_d3dvideosink_change_state (GstElement * element, GstStateChange transition)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (element);
  GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      gst_d3dvideosink_initialize_direct3d(sink);
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      gst_d3dvideosink_remove_window_for_renderer(sink);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      gst_d3dvideosink_release_direct3d(sink);
      gst_d3dvideosink_clear(sink);
      break;
  }

  return ret;
}

static gboolean
gst_d3dvideosink_start (GstBaseSink * bsink)
{
  HRESULT hres = S_FALSE;
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (bsink);

  return TRUE;
}

static gboolean
gst_d3dvideosink_set_caps (GstBaseSink * bsink, GstCaps * caps)
{
  guint32 fourcc;
  gint width;
  gint height;
  gint par_n;
  gint par_d;
  gint bpp;
  gint targetWidth;
  gint targetHeight;
  GstStructure *s;
  const gchar *name;
  gchar *capsstring;

  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (bsink);

  /* Do what you must here to convert caps into something Direct3D can use */
  s = gst_caps_get_structure (caps, 0);
  name = gst_structure_get_name (s);

  capsstring = gst_caps_to_string (caps);
  GST_DEBUG ("Setting caps to: %s", capsstring);
  g_free (capsstring);

  if (!strcmp (name, "video/x-raw-yuv")) {
    if (!gst_structure_get_fourcc (s, "format", &fourcc)) {
      GST_WARNING ("Failed to convert caps, missing fourcc");
      return FALSE;
    }

    if (!gst_structure_get_int (s, "width", &width)) {
      GST_WARNING ("Failed to convert caps, missing width");
      return FALSE;
    }

    if (!gst_structure_get_int (s, "height", &height)) {
      GST_WARNING ("Failed to convert caps, missing height");
      return FALSE;
    }

    switch (fourcc) {
      case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
        bpp = 16;
        break;
      case GST_MAKE_FOURCC ('Y', 'U', 'Y', 'V'):
        bpp = 16;
        break;
      case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
        bpp = 16;
        break;
      case GST_MAKE_FOURCC ('Y', 'V', '1', '2'):
        bpp = 12;
        break;
      default:
        GST_WARNING ("Failed to convert caps, unknown fourcc");
        return FALSE;
    }

    if (gst_structure_get_fraction (s, "pixel-aspect-ratio", &par_n, &par_d)) {
      targetWidth = width * par_n / par_d;
      targetHeight = height;
    } else {
      targetWidth = width;
      targetHeight = height;
    }
  
    sink->bpp = bpp;
    sink->width = width;
    sink->height = height;
    sink->fourcc = fourcc;
    sink->targetWidth = targetWidth;
    sink->targetHeight = targetHeight;

  } else {
    GST_WARNING ("Failed to convert caps, unknown caps type");
    return FALSE;
  }

  /* Create a window (or start using an application-supplied one, then connect the graph */
  gst_d3dvideosink_prepare_window (sink);

  return TRUE;
}

static gboolean
gst_d3dvideosink_stop (GstBaseSink * bsink)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (bsink);
  gst_d3dvideosink_close_window(sink);
  gst_d3dvideosink_release_swap_chain(sink);
  return TRUE;
}

static GstFlowReturn
gst_d3dvideosink_show_frame (GstVideoSink *vsink, GstBuffer *buffer)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (vsink);
  
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SWAP_CHAIN_LOCK(sink);
  {
    HRESULT hr;
    guint8 *source;
    LPDIRECT3DSURFACE9 backBuffer;

    if (!shared.d3ddev) {
      if (!shared.device_lost) {
        GST_WARNING("No Direct3D device has been created, stopping");
        goto error;
      } else {
        GST_WARNING("Direct3D device is lost. Maintaining flow until it has been reset.");
        goto success;
      }
    }

    if (!sink->d3d_offscreen_surface) {
      GST_WARNING("No Direct3D offscreen surface has been created, stopping");
      goto error;
    }

    if (!sink->d3d_swap_chain) {
      GST_WARNING("No Direct3D swap chain has been created, stopping");
      goto error;
    }

    if (sink->window_closed) {
      GST_WARNING("Window has been closed, stopping");
      goto error;
    }

    /* Get a reference to the buffer containing the image we want to display on screen */
    source = GST_BUFFER_DATA(buffer);

    /* Set the render target to our swap chain */
    IDirect3DSwapChain9_GetBackBuffer(sink->d3d_swap_chain, 0, D3DBACKBUFFER_TYPE_MONO, &backBuffer);
    IDirect3DDevice9_SetRenderTarget(shared.d3ddev, 0, backBuffer);
    IDirect3DSurface9_Release(backBuffer);

    /* Clear the target */
    IDirect3DDevice9_Clear(shared.d3ddev, 0, NULL, D3DCLEAR_TARGET, D3DCOLOR_XRGB(0, 0, 0), 1.0f, 0);

    if (SUCCEEDED(IDirect3DDevice9_BeginScene(shared.d3ddev))) {
      if (source) {
        D3DLOCKED_RECT lr;
        guint8 *dest;

        IDirect3DSurface9_LockRect(sink->d3d_offscreen_surface, &lr, NULL, 0);
        dest = (guint8 *)lr.pBits;

        if (dest) {
          int i;
          int width;
          int height;
          int srcstride;
          int dststride;

          width = sink->width;
          height = sink->height;

          /* Push the bytes to the video card */
          switch(sink->fourcc) {
            case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
            case GST_MAKE_FOURCC ('Y', 'U', 'Y', 'V'):
            case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
              /* Nice and simple */
              srcstride = GST_ROUND_UP_4 (width * 2);
              dststride = lr.Pitch;

              for (i = 0; i < height; ++i)
                memcpy (dest + dststride * i, source + srcstride * i, srcstride);
              break;
            
            default:
              break;
            //case GST_MAKE_FOURCC ('Y', 'V', '1', '2'):
            //  {
            //    guint8 *src;
            //    guint8 *dst;
            //    gint32 rows;
            //    int component;

            //    for (component = 0; component < 3; component++) {
            //      // TODO: Get format properly rather than hard-coding it. Use gst_video_* APIs *?
            //      if (component == 0) {
            //        srcstride = GST_ROUND_UP_4 (width);
            //        src = source;
            //      }
            //      else {
            //        srcstride = GST_ROUND_UP_4 ( GST_ROUND_UP_2 (width) / 2);
            //        if (component == 1)
            //          src = source + GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height);
            //        else
            //          src = source + GST_ROUND_UP_4 (width) * GST_ROUND_UP_2 (height) +
            //                  srcstride * (GST_ROUND_UP_2 (height) / 2);
            //      }

            //      /* Is there a better way to do this? This is ICK! */
            //      if (component == 0) {
            //        dststride = width;
            //        dst = dest;
            //        rows = height;
            //      } else if (component == 1) {
            //        dststride = width / 2;
            //        dst = dest + width * height;
            //        rows = height/2;
            //      }
            //      else {
            //        dststride = width / 2;
            //        dst = dest + width * height +
            //                       width/2 * height/2;
            //        rows = height/2;
            //      }

            //      for (i = 0; i < rows; i++) {
            //        memcpy (dst + i * dststride, src + i * srcstride, srcstride);
            //      }
            //    }
            //  }
            //  break;
          }
        }
        IDirect3DSurface9_UnlockRect(sink->d3d_offscreen_surface);
      }
      //if (sink->keep_aspect_ratio) {
      //  int width;
      //  int height;
      //  int window_width;
      //  int window_height;
      //  RECT r;
      //  D3DPRESENT_PARAMETERS d3dpp;

      //  width = sink->width;
      //  height = sink->height;

      //  //IDirect3DSwapChain9_GetPresentParameters(sink->d3d_swap_chain, &d3dpp);
      //  //window_width = d3dpp.BackBufferWidth;
      //  //window_height = d3dpp.BackBufferHeight;

      //  GetWindowRect(sink->window_id, &r);
      //  window_width = MAX(1, ABS(r.right - r.left)) - (sink->is_new_window ? GetSystemMetrics (SM_CXSIZEFRAME) * 2 : 0);
      //  window_height = MAX(1, ABS(r.bottom - r.top)) - (sink->is_new_window ? GetSystemMetrics (SM_CYCAPTION) + (GetSystemMetrics (SM_CYSIZEFRAME) * 2) : 0);

      //  if (width > window_width) {
      //    double ratio = (double)window_width/(double)width;
      //    width = window_width;
      //    height = (int)(height * ratio);
      //  }

      //  if (height > window_height) {
      //    double ratio = (double)window_height/(double)height;
      //    height = window_height;
      //    width = (int)(width * ratio);
      //  }

      //  r.top = MAX(0, (window_height - height) / 2);
      //  r.bottom = MAX(window_height, window_height - r.top + height);
      //  r.left = MAX(0, (window_width - width) / 2);
      //  r.right = MAX(window_width, window_width - r.left + width);

      //  GST_DEBUG("aspect ratio. top: %d, bottom: %d, left: %d, right: %d", r.top, r.bottom, r.left, r.right);

      //  IDirect3DDevice9_StretchRect(shared.d3ddev, sink->d3d_offscreen_surface, NULL, backBuffer, &r, D3DTEXF_NONE);
      //} else {
      //  IDirect3DDevice9_StretchRect(shared.d3ddev, sink->d3d_offscreen_surface, NULL, backBuffer, NULL, D3DTEXF_NONE);
      //}
      IDirect3DDevice9_StretchRect(shared.d3ddev, sink->d3d_offscreen_surface, NULL, backBuffer, NULL, D3DTEXF_NONE);
      IDirect3DDevice9_EndScene(shared.d3ddev);
    }
    /* Swap back and front buffers on video card and present to the user */
    if (FAILED(hr = IDirect3DSwapChain9_Present(sink->d3d_swap_chain, NULL, NULL, NULL, NULL, 0))) {
      switch(hr) 
      {
        case D3DERR_DEVICELOST:
        case D3DERR_DEVICENOTRESET:
          gst_d3dvideosink_notify_device_lost(sink);
          break;
        default:
          goto wrong_state;
      }
    }
  }

success:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return GST_FLOW_OK;
wrong_state:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return GST_FLOW_WRONG_STATE;
unexpected:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return GST_FLOW_UNEXPECTED;
error:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return GST_FLOW_ERROR;
}

/* Simply redraws the last item on our offscreen surface to the window */
static gboolean 
gst_d3dvideosink_refresh (GstD3DVideoSink *sink)
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SWAP_CHAIN_LOCK(sink);
  {
    HRESULT hr;
    LPDIRECT3DSURFACE9 backBuffer;

    if (!shared.d3ddev) {
      if (!shared.device_lost)
        GST_DEBUG_OBJECT (sink, "No Direct3D device has been created");
      goto error;
    }

    if (!sink->d3d_offscreen_surface) {
      GST_DEBUG_OBJECT (sink, "No Direct3D offscreen surface has been created");
      goto error;
    }

    if (!sink->d3d_swap_chain) {
      GST_DEBUG_OBJECT (sink, "No Direct3D swap chain has been created");
      goto error;
    }

    if (sink->window_closed) {
      GST_DEBUG_OBJECT (sink, "Window has been closed");
      goto error;
    }

    /* Set the render target to our swap chain */
    IDirect3DSwapChain9_GetBackBuffer(sink->d3d_swap_chain, 0, D3DBACKBUFFER_TYPE_MONO, &backBuffer);
    IDirect3DDevice9_SetRenderTarget(shared.d3ddev, 0, backBuffer);
    IDirect3DSurface9_Release(backBuffer);

    /* Clear the target */
    IDirect3DDevice9_Clear(shared.d3ddev, 0, NULL, D3DCLEAR_TARGET, D3DCOLOR_XRGB(0, 0, 0), 1.0f, 0);

    if (SUCCEEDED(IDirect3DDevice9_BeginScene(shared.d3ddev))) {
      IDirect3DDevice9_StretchRect(shared.d3ddev, sink->d3d_offscreen_surface, NULL, backBuffer, NULL, D3DTEXF_NONE);
      IDirect3DDevice9_EndScene(shared.d3ddev);
    }
  
    /* Swap back and front buffers on video card and present to the user */
    if (FAILED(hr = IDirect3DSwapChain9_Present(sink->d3d_swap_chain, NULL, NULL, NULL, NULL, 0))) {
      switch(hr) 
      {
        case D3DERR_DEVICELOST:
        case D3DERR_DEVICENOTRESET:
          gst_d3dvideosink_notify_device_lost(sink);
          break;
        default:
          goto error;
      }
    }
  }

success:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean 
gst_d3dvideosink_update_all (GstD3DVideoSink *sink)
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK
  {
    GList *item;
    GstD3DVideoSink *s;
    for(item = g_list_first(shared.element_list); item; item = item->next) {
      s = item->data;
      gst_d3dvideosink_update(GST_BASE_SINK(s));
    }
  }
success:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean 
gst_d3dvideosink_refresh_all (GstD3DVideoSink *sink)
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK
  {
    GList *item;
    GstD3DVideoSink *s;
    for(item = g_list_first(shared.element_list); item; item = item->next) {
      s = item->data;
      gst_d3dvideosink_refresh(s);
    }
  }
success:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static void
gst_d3dvideosink_expose(GstXOverlay * overlay)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (overlay);
  GstBuffer *last_buffer;

  last_buffer = gst_base_sink_get_last_buffer(GST_BASE_SINK(sink));
  if (last_buffer) {
    gst_d3dvideosink_show_frame(GST_VIDEO_SINK(sink), last_buffer);
    gst_buffer_unref(last_buffer);
  }
}

static void 
gst_d3dvideosink_update(GstBaseSink * bsink) 
{
  GstBuffer *last_buffer;

  last_buffer = gst_base_sink_get_last_buffer(bsink);
  if (last_buffer) {
    gst_d3dvideosink_show_frame(GST_VIDEO_SINK(bsink), last_buffer);
    gst_buffer_unref(last_buffer);
  }
}

/* TODO: How can we implement these? Figure that out... */
static gboolean
gst_d3dvideosink_unlock (GstBaseSink * bsink)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (bsink);

  return TRUE;
}

static gboolean
gst_d3dvideosink_unlock_stop (GstBaseSink * bsink)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (bsink);

  return TRUE;
}

static gboolean
gst_d3dvideosink_initialize_direct3d (GstD3DVideoSink *sink)
{
  /* Let's hope this is never a problem (they have millions of d3d elements going at the same time) */
  if (shared.element_count >= G_MAXINT32) {
    GST_ERROR("There are too many d3dvideosink elements. Creating more elements would put this element into an unknown state.");
    return FALSE;
  }

  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK

  /* Add to our GList containing all of our elements. */
  /* GLists are doubly-linked lists and calling prepend() prevents it from having to traverse the entire list just to add one item. */
  shared.element_list = g_list_prepend(shared.element_list, sink);

  /* Increment our count of the number of elements we have */
  shared.element_count++;
  if (shared.element_count > 1)
    goto success;

  /* We want to initialize direct3d only for the first element that's using it. */
  /* We'll destroy this once all elements using direct3d have been finalized. */
  /* See gst_d3dvideosink_release_direct3d() for details. */
  
  /* We create a window that's hidden and used by the Direct3D device. The */
  /* device is shared among all d3dvideosink windows. */

  GST_DEBUG("Creating hidden window for Direct3D");
  if (!gst_d3dvideosink_create_shared_hidden_window(sink))
    goto error;

  GST_DEBUG("Initializing Direct3D");
  if (!gst_d3dvideosink_initialize_d3d_device(sink))
    goto error;
  GST_DEBUG("Direct3D initialization complete");

success:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean
gst_d3dvideosink_initialize_d3d_device (GstD3DVideoSink *sink)
{
  HRESULT hr;
  LPDIRECT3D9 d3d;
  D3DDISPLAYMODE d3ddm;
  LPDIRECT3DDEVICE9 d3ddev;
  D3DPRESENT_PARAMETERS d3dpp;

  d3d = Direct3DCreate9(D3D_SDK_VERSION);
  if (!d3d) {
    GST_WARNING ("Unable to create Direct3D interface");
    goto error;
  }

  if (FAILED(IDirect3D9_GetAdapterDisplayMode(d3d, D3DADAPTER_DEFAULT, &d3ddm))) {
    /* Prevent memory leak */
    IDirect3D9_Release(d3d);
    GST_WARNING ("Unable to request adapter display mode");
    goto error;
  }
  
  ZeroMemory(&d3dpp, sizeof(d3dpp));
  //d3dpp.Flags = D3DPRESENTFLAG_VIDEO;
  d3dpp.Windowed = TRUE;
  d3dpp.SwapEffect = D3DSWAPEFFECT_DISCARD;
  d3dpp.BackBufferCount = 1;
  d3dpp.BackBufferFormat = d3ddm.Format;
  d3dpp.BackBufferWidth = 1;
  d3dpp.BackBufferHeight = 1;
  d3dpp.MultiSampleType = D3DMULTISAMPLE_NONE;
  d3dpp.PresentationInterval = D3DPRESENT_INTERVAL_DEFAULT; //D3DPRESENT_INTERVAL_IMMEDIATE;

  GST_DEBUG("Creating Direct3D device for hidden window %d", shared.hidden_window_id);

  if (FAILED(hr = IDirect3D9_CreateDevice(
    d3d, 
    D3DADAPTER_DEFAULT, 
    D3DDEVTYPE_HAL, 
    shared.hidden_window_id, 
    D3DCREATE_SOFTWARE_VERTEXPROCESSING, 
    &d3dpp, 
    &d3ddev
  ))) {
    /* Prevent memory leak */
    IDirect3D9_Release(d3d);
    GST_WARNING ("Unable to create Direct3D device. Result: %d (0x%x)", hr, hr);
    goto error;
  }

  //if (FAILED(IDirect3DDevice9_GetDeviceCaps(
  //  d3ddev, 
  //  &d3dcaps
  //))) {
  //  /* Prevent memory leak */
  //  IDirect3D9_Release(d3d);
  //  GST_WARNING ("Unable to retrieve Direct3D device caps");
  //  goto error;
  //}

  shared.d3d = d3d;
  shared.d3ddev = d3ddev;

success:
  return TRUE;
error:
  return FALSE;
}

static gboolean
gst_d3dvideosink_initialize_swap_chain (GstD3DVideoSink *sink)
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SWAP_CHAIN_LOCK(sink);
  {
    gint width;
    gint height;
    //D3DDISPLAYMODE mode;
    D3DPRESENT_PARAMETERS d3dpp;
    D3DFORMAT d3dformat;
    D3DFORMAT d3dfourcc;
    //D3DFORMAT d3dstencilformat;
    LPDIRECT3DSWAPCHAIN9 d3dswapchain;
    LPDIRECT3DSURFACE9 d3dsurface;
    //gboolean d3dEnableAutoDepthStencil;

    /* This should always work since gst_d3dvideosink_initialize_direct3d() should have always been called previously */
    if (!shared.d3ddev) {
      GST_ERROR("Direct3D device has not been initialized");
      goto error;
    }

    GST_DEBUG("Initializing Direct3D swap chain for sink %d", sink);
 
    switch (sink->fourcc) {
      case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
        d3dformat = D3DFMT_X8R8G8B8;
        d3dfourcc = (D3DFORMAT)MAKEFOURCC('Y', 'U', 'Y', '2');
        break;
      case GST_MAKE_FOURCC ('Y', 'U', 'Y', 'V'):
        d3dformat = D3DFMT_X8R8G8B8;
        d3dfourcc = (D3DFORMAT)MAKEFOURCC('Y', 'U', 'Y', 'V');
        break;
      case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
        d3dformat = D3DFMT_X8R8G8B8;
        d3dfourcc = (D3DFORMAT)MAKEFOURCC('U', 'Y', 'V', 'Y');
        break;
      case GST_MAKE_FOURCC ('Y', 'V', '1', '2'):
        d3dformat = D3DFMT_X8R8G8B8;
        d3dfourcc = (D3DFORMAT)MAKEFOURCC('Y', 'V', '1', '2');
        break;
      default:
        GST_WARNING ("Failed to find compatible Direct3D format");
        goto error;
    }

    GST_DEBUG("Determined Direct3D format: %d", d3dformat);

    //Stencil/depth buffers aren't created by default when using swap chains
    //if (SUCCEEDED(IDirect3D9_CheckDeviceFormat(shared.d3d, D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, d3dformat, D3DUSAGE_DEPTHSTENCIL, D3DRTYPE_SURFACE, D3DFMT_D32))) {
    //  d3dstencilformat = D3DFMT_D32;
    //  d3dEnableAutoDepthStencil = TRUE;
    //} else if (SUCCEEDED(IDirect3D9_CheckDeviceFormat(shared.d3d, D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, d3dformat, D3DUSAGE_DEPTHSTENCIL, D3DRTYPE_SURFACE, D3DFMT_D24X8))) {
    //  d3dstencilformat = D3DFMT_D24X8;
    //  d3dEnableAutoDepthStencil = TRUE;
    //} else if (SUCCEEDED(IDirect3D9_CheckDeviceFormat(shared.d3d, D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, d3dformat, D3DUSAGE_DEPTHSTENCIL, D3DRTYPE_SURFACE, D3DFMT_D16))) {
    //  d3dstencilformat = D3DFMT_D16;
    //  d3dEnableAutoDepthStencil = TRUE;
    //} else {
    //  d3dstencilformat = D3DFMT_X8R8G8B8;
    //  d3dEnableAutoDepthStencil = FALSE;
    //}
    //
    //GST_DEBUG("Determined Direct3D stencil format: %d", d3dstencilformat);

    GST_DEBUG("Direct3D back buffer size: %dx%d", sink->targetWidth, sink->targetHeight);

    /* Get the current size of the window */
    gst_d3dvideosink_window_size(sink, &width, &height);

    ZeroMemory(&d3dpp, sizeof(d3dpp));
    d3dpp.Windowed = TRUE;
    d3dpp.SwapEffect = D3DSWAPEFFECT_DISCARD;
    d3dpp.hDeviceWindow = sink->window_id;
    d3dpp.BackBufferFormat = d3dformat;
    d3dpp.BackBufferWidth = width;
    d3dpp.BackBufferHeight = height;

    if (FAILED(IDirect3DDevice9_CreateAdditionalSwapChain(shared.d3ddev, &d3dpp, &d3dswapchain)))
      goto error;

    if (FAILED(IDirect3DDevice9_CreateOffscreenPlainSurface(shared.d3ddev, sink->width, sink->height, d3dfourcc, D3DPOOL_DEFAULT, &d3dsurface, NULL))) {
      /* Ensure that we release our newly created swap chain to prevent memory leaks */
      IDirect3DSwapChain9_Release(d3dswapchain);
      goto error;
    }

    sink->d3dformat = d3dformat;
    sink->d3dfourcc = d3dfourcc;
    sink->d3d_swap_chain = d3dswapchain;
    sink->d3d_offscreen_surface = d3dsurface;
  }

success:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean
gst_d3dvideosink_resize_swap_chain (GstD3DVideoSink *sink, gint width, gint height)
{
  if (width <= 0 || height <= 0) {
    GST_DEBUG("Invalid size");
    return FALSE;
  }

  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SWAP_CHAIN_LOCK(sink);
  {
    int ref_count;
    D3DPRESENT_PARAMETERS d3dpp;
    LPDIRECT3DSWAPCHAIN9 d3dswapchain;

    GST_DEBUG("Resizing Direct3D swap chain for sink %d to %dx%d", sink, width, height);

    if (!shared.d3d || !shared.d3ddev) {
      if (!shared.device_lost)
        GST_WARNING("Direct3D device has not been initialized");
      goto error;
    }

    if (!sink->d3d_swap_chain) {
      GST_DEBUG("Direct3D swap chain has not been initialized");
      goto error;
    }

    /* Get the parameters used to create this swap chain */
    if (FAILED(IDirect3DSwapChain9_GetPresentParameters(sink->d3d_swap_chain, &d3dpp))) {
      GST_DEBUG("Unable to determine Direct3D present parameters for swap chain");
      goto error;
    }

    /* Release twice because IDirect3DSwapChain9_GetPresentParameters() adds a reference */
    while((ref_count = IDirect3DSwapChain9_Release(sink->d3d_swap_chain)) > 0)
      ;
    sink->d3d_swap_chain = NULL;
    GST_DEBUG("Old Direct3D swap chain released. Reference count: %d", ref_count);

    /* Adjust back buffer width/height */
    d3dpp.BackBufferWidth = width;
    d3dpp.BackBufferHeight = height;

    if (FAILED(IDirect3DDevice9_CreateAdditionalSwapChain(shared.d3ddev, &d3dpp, &d3dswapchain)))
      goto error;

    sink->d3d_swap_chain = d3dswapchain;
  }

success:
  GST_DEBUG("Direct3D swap chain succesfully resized for sink %d", sink);
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_DEBUG("Error attempting to resize the Direct3D swap chain for sink %d", sink);
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean
gst_d3dvideosink_release_swap_chain (GstD3DVideoSink *sink)
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SWAP_CHAIN_LOCK(sink);
  {
    GST_DEBUG("Releasing Direct3D swap chain for sink %d", sink);

     /* This should always work since gst_d3dvideosink_initialize_direct3d() should have always been called previously */
    if (!shared.d3d || !shared.d3ddev) {
      if (!shared.device_lost)
        GST_ERROR("Direct3D device has not been initialized");
      goto error;
    }

    if (!sink->d3d_swap_chain && !sink->d3d_offscreen_surface)
      goto success;

    if (sink->d3d_offscreen_surface) {
      int ref_count;
      ref_count = IDirect3DSurface9_Release(sink->d3d_offscreen_surface);
      sink->d3d_offscreen_surface = NULL;
      GST_DEBUG("Direct3D offscreen surface released for sink %d. Reference count: %d", sink, ref_count);
    }

    if (sink->d3d_swap_chain) {
      int ref_count;
      ref_count = IDirect3DSwapChain9_Release(sink->d3d_swap_chain);
      sink->d3d_swap_chain = NULL;
      GST_DEBUG("Direct3D swap chain released for sink %d. Reference count: %d", sink, ref_count);
    }
  }

success:
  GST_DEBUG("Direct3D swap chain succesfully released for sink %d", sink);
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_DEBUG("Error attempting to release the Direct3D swap chain for sink %d", sink);
  GST_D3DVIDEOSINK_SWAP_CHAIN_UNLOCK(sink);
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean gst_d3dvideosink_notify_device_lost (GstD3DVideoSink *sink) 
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK
  {
    /* Send notification asynchronously */
    PostMessage(shared.hidden_window_id, WM_D3D_INIT_DEVICELOST, 0, 0);
  }
success:
  GST_DEBUG("Succesfully sent notification of device lost event for sink %d", sink);
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_DEBUG("Error attempting to send notification of device lost event for sink %d", sink);
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean gst_d3dvideosink_notify_device_reset (GstD3DVideoSink *sink) 
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK
  {
    /* Send notification synchronously -- let's ensure the timer's been killed before returning */
    SendMessage(shared.hidden_window_id, WM_D3D_END_DEVICELOST, 0, 0);
  }
success:
  GST_DEBUG("Succesfully sent notification of device reset event for sink %d", sink);
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_DEBUG("Error attempting to send notification of reset lost event for sink %d", sink);
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean gst_d3dvideosink_device_lost (GstD3DVideoSink *sink) {
  /* Must be called from hidden window's message loop! */

  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK
  {
    GST_DEBUG("Direct3D device lost. Resetting the device.");

    if (g_thread_self() != shared.hidden_window_thread) {
      GST_ERROR("Direct3D device can only be reset by the thread that created it.");
      goto error;
    }

    if (!shared.device_lost && (!shared.d3d || !shared.d3ddev)) {
      GST_ERROR("Direct3D device has not been initialized");
      goto error;
    }

    {
      GList* item;
      GstD3DVideoSink *s;

      /* This is technically a bit different from the normal. We don't call reset(), instead */
      /* we recreate everything from scratch. */

      /* Release all swap chains, surfaces, buffers, etc. */
      for(item = g_list_first(shared.element_list); item; item = item->next) {
        s = item->data;
        gst_d3dvideosink_release_swap_chain(s);
      }

      /* Release the device */
      if (!gst_d3dvideosink_release_d3d_device(NULL))
        goto error;

      /* Recreate device */
      if (!gst_d3dvideosink_initialize_d3d_device(NULL))
        goto error;

      /* Reinitialize all swap chains, surfaces, buffers, etc. */
      for(item = g_list_first(shared.element_list); item; item = item->next) {
        s = item->data;
        gst_d3dvideosink_initialize_swap_chain(s);
      }
    }

    /* Let the hidden window know that it's okay to kill the timer */
    gst_d3dvideosink_notify_device_reset(sink);
  }

success:
  GST_DEBUG("Direct3D device has successfully been reset.");
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_DEBUG("Unable to successfully reset the Direct3D device.");
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean
gst_d3dvideosink_release_d3d_device (GstD3DVideoSink *sink)
{
  GST_DEBUG("Cleaning all Direct3D objects");

  if (shared.d3ddev) {
    int ref_count;
    ref_count = IDirect3DDevice9_Release(shared.d3ddev);
    shared.d3ddev = NULL;
    GST_DEBUG("Direct3D device released. Reference count: %d", ref_count);
  }

  if (shared.d3d) {
    int ref_count;
    ref_count = IDirect3D9_Release(shared.d3d);
    shared.d3d = NULL;
    GST_DEBUG("Direct3D object released. Reference count: %d", ref_count);
  }

  return TRUE;
}

static gboolean
gst_d3dvideosink_release_direct3d (GstD3DVideoSink *sink)
{
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_LOCK
  GST_D3DVIDEOSINK_SHARED_D3D_LOCK

  /* Remove item from the list */
  shared.element_list = g_list_remove(shared.element_list, sink);

  /* Decrement our count of the number of elements we have */
  shared.element_count--;
  if (shared.element_count < 0)
    shared.element_count = 0;
  if (shared.element_count > 0)
    goto success;

  gst_d3dvideosink_release_d3d_device(sink);

  GST_DEBUG("Closing hidden Direct3D window");
  gst_d3dvideosink_close_shared_hidden_window(sink);

success:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return TRUE;
error:
  GST_D3DVIDEOSINK_SHARED_D3D_UNLOCK
  GST_D3DVIDEOSINK_SHARED_D3D_DEV_UNLOCK
  return FALSE;
}

static gboolean
gst_d3dvideosink_window_size (GstD3DVideoSink *sink, gint *width, gint *height)
{
  if (!sink || !sink->window_id) {
    if (width && height) {
      *width = 0;
      *height = 0;
    }
    return FALSE;
  }

  {
    RECT sz;
    GetWindowRect(sink->window_id, &sz);
    *width = MAX(1, ABS(sz.right - sz.left)) - (sink->is_new_window ? GetSystemMetrics (SM_CXSIZEFRAME) * 2 : 0);
    *height = MAX(1, ABS(sz.bottom - sz.top)) - (sink->is_new_window ? GetSystemMetrics (SM_CYCAPTION) + (GetSystemMetrics (SM_CYSIZEFRAME) * 2) : 0);
  }
  return TRUE;
}

/* Plugin entry point */
static gboolean
plugin_init (GstPlugin *plugin)
{
  /* PRIMARY: this is the best videosink to use on windows */
  if (!gst_element_register (plugin, "d3dvideosink",
          GST_RANK_PRIMARY, GST_TYPE_D3DVIDEOSINK))
    return FALSE;

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "d3dsinkwrapper",
    "Direct3D sink wrapper plugin",
    plugin_init, VERSION, "LGPL", GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
