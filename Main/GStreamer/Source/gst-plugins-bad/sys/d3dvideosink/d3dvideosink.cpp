/* GStreamer
 * Copyright (C) 2008 Pioneers of the Inevitable <songbird@songbirdnest.com>
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

#include <gst/interfaces/xoverlay.h>

#include "windows.h"

#define WM_GRAPH_NOTIFY WM_APP + 1

GST_DEBUG_CATEGORY (d3dvideosink_debug);
#define GST_CAT_DEFAULT d3dvideosink_debug

/* TODO: Support RGB! */
static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (
        "video/x-raw-yuv,"
        "width = (int) [ 1, MAX ],"
        "height = (int) [ 1, MAX ],"
        "framerate = (fraction) [ 0, MAX ]," 
        "format = {(fourcc)YUY2, (fourcc)UYVY, (fourcc) YUVY, (fourcc)YV12 }")
    );

static void gst_d3dvideosink_init_interfaces (GType type);

GST_BOILERPLATE_FULL (GstD3DVideoSink, gst_d3dvideosink, GstVideoSink,
    GST_TYPE_VIDEO_SINK, gst_d3dvideosink_init_interfaces);

enum
{
  PROP_0,
  PROP_KEEP_ASPECT_RATIO,
  PROP_FULL_SCREEN
};

/* GObject methods */
static void gst_d3dvideosink_finalize (GObject * gobject);
static void gst_d3dvideosink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_d3dvideosink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

/* GstElement methods */
static GstStateChangeReturn gst_d3dvideosink_change_state (GstElement * element, GstStateChange transition);

/* GstBaseSink methods */
static gboolean gst_d3dvideosink_start (GstBaseSink * bsink);
static gboolean gst_d3dvideosink_stop (GstBaseSink * bsink);
static gboolean gst_d3dvideosink_unlock (GstBaseSink * bsink);
static gboolean gst_d3dvideosink_unlock_stop (GstBaseSink * bsink);
static gboolean gst_d3dvideosink_set_caps (GstBaseSink * bsink, GstCaps * caps);
static GstCaps *gst_d3dvideosink_get_caps (GstBaseSink * bsink);
static GstFlowReturn gst_d3dvideosink_show_frame (GstVideoSink *sink, GstBuffer *buffer);

/* GstXOverlay methods */
static void gst_d3dvideosink_set_window_id (GstXOverlay * overlay, ULONG window_id);
static void gst_d3dvideosink_expose (GstXOverlay * overlay);

/* WndProc methods */
static void gst_d3dvideosink_wnd_proc(GstD3DVideoSink *sink, HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

/* Paint/update methods */
static void gst_d3dvideosink_update(GstBaseSink * bsink) ;

/* TODO: event, preroll, buffer_alloc? 
 * buffer_alloc won't generally be all that useful because the renderers require a 
 * different stride to GStreamer's implicit values. 
 */

static gboolean
gst_d3dvideosink_interface_supported (GstImplementsInterface * iface,
    GType type)
{
  g_assert (type == GST_TYPE_X_OVERLAY);
  return TRUE;
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
    NULL,
  };

  static const GInterfaceInfo xoverlay_info = {
    (GInterfaceInitFunc) gst_d3dvideosink_xoverlay_interface_init,
    NULL,
    NULL,
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
  gobject_class->set_property =
      GST_DEBUG_FUNCPTR (gst_d3dvideosink_set_property);
  gobject_class->get_property =
      GST_DEBUG_FUNCPTR (gst_d3dvideosink_get_property);

  gstelement_class->change_state = GST_DEBUG_FUNCPTR (gst_d3dvideosink_change_state);

  gstbasesink_class->get_caps = GST_DEBUG_FUNCPTR (gst_d3dvideosink_get_caps);
  gstbasesink_class->set_caps = GST_DEBUG_FUNCPTR (gst_d3dvideosink_set_caps);
  gstbasesink_class->start = GST_DEBUG_FUNCPTR (gst_d3dvideosink_start);
  gstbasesink_class->stop = GST_DEBUG_FUNCPTR (gst_d3dvideosink_stop);
  gstbasesink_class->unlock = GST_DEBUG_FUNCPTR (gst_d3dvideosink_unlock);
  gstbasesink_class->unlock_stop =
      GST_DEBUG_FUNCPTR (gst_d3dvideosink_unlock_stop);

  gstvideosink_class->show_frame = GST_DEBUG_FUNCPTR (gst_d3dvideosink_show_frame);

  /* Add properties */
  g_object_class_install_property (G_OBJECT_CLASS (klass),
      PROP_KEEP_ASPECT_RATIO, g_param_spec_boolean ("force-aspect-ratio",
          "Force aspect ratio",
          "When enabled, scaling will respect original aspect ratio", FALSE,
          (GParamFlags)G_PARAM_READWRITE));
  g_object_class_install_property (G_OBJECT_CLASS (klass),
      PROP_FULL_SCREEN, g_param_spec_boolean ("fullscreen",
          "Full screen mode",
          "Use full-screen mode (not available when using XOverlay)", FALSE,
          (GParamFlags)G_PARAM_READWRITE));
}

static void
gst_d3dvideosink_clear (GstD3DVideoSink *sink)
{
  sink->keep_aspect_ratio = FALSE;
  sink->full_screen = FALSE;

  sink->window_closed = FALSE;
  sink->window_id = NULL;
  sink->is_new_window = FALSE;
}

static void
gst_d3dvideosink_init (GstD3DVideoSink * sink, GstD3DVideoSinkClass * klass)
{
  gst_d3dvideosink_clear (sink);

  /* TODO: Copied from GstVideoSink; should we use that as base class? */
  /* 20ms is more than enough, 80-130ms is noticable */
  gst_base_sink_set_max_lateness (GST_BASE_SINK (sink), 20 * GST_MSECOND);
  gst_base_sink_set_qos_enabled (GST_BASE_SINK (sink), TRUE);
}

static void
gst_d3dvideosink_finalize (GObject * gobject)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (gobject);
  G_OBJECT_CLASS (parent_class)->finalize (gobject);
}

static void
gst_d3dvideosink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (object);

  switch (prop_id) {
    case PROP_KEEP_ASPECT_RATIO:
      sink->keep_aspect_ratio = g_value_get_boolean (value);
      break;
    case PROP_FULL_SCREEN:
      sink->full_screen = g_value_get_boolean (value);
      break;
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
    case PROP_KEEP_ASPECT_RATIO:
      g_value_set_boolean (value, sink->keep_aspect_ratio);
      break;
    case PROP_FULL_SCREEN:
      g_value_set_boolean (value, sink->full_screen);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstCaps *
gst_d3dvideosink_get_caps (GstBaseSink * basesink)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (basesink);
  return sink->caps;
}

/* WNDPROC for application-supplied windows */
LRESULT APIENTRY WndProcHook (HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  /* Handle certain actions specially on the window passed to us.
   * Then forward back to the original window.
   */
  GstD3DVideoSink *sink = (GstD3DVideoSink *)GetProp (hWnd, L"GstD3DVideoSink");
  gst_d3dvideosink_wnd_proc (sink, hWnd, message, wParam, lParam);
  return CallWindowProc (sink->prevWndProc, hWnd, message, wParam, lParam);
}

/* WndProc for our default window, if the application didn't supply one */
LRESULT APIENTRY 
WndProc (HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  if (message == WM_CREATE) 
  {
    GstD3DVideoSink *sink = (GstD3DVideoSink *)lParam;

    /* lParam holds the parameter used when creating the window. */
    /* In our case, this is a pointer to the sink. So we immediately attach it for use in subsequent calls. */
    SetWindowLongPtr (hWnd, GWLP_USERDATA, (LONG)sink);

    /* signal application we created a window */
    gst_x_overlay_got_xwindow_id (GST_X_OVERLAY (sink), (gulong)hWnd);
  }

  GstD3DVideoSink *sink = (GstD3DVideoSink *)GetWindowLongPtr (hWnd, GWLP_USERDATA);
  gst_d3dvideosink_wnd_proc (sink, hWnd, message, wParam, lParam);

  switch (message) 
  {
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
    case WM_CLOSE:
    case WM_DESTROY:
      {
        sink->window_closed = TRUE;
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

  memset (&WndClass, 0, sizeof (WNDCLASS));
  WndClass.style = CS_HREDRAW | CS_VREDRAW;
  WndClass.hInstance = GetModuleHandle (NULL);
  WndClass.lpszClassName = L"GST-D3DSink";
  WndClass.hbrBackground = (HBRUSH) GetStockObject (BLACK_BRUSH);
  WndClass.cbClsExtra = 0;
  WndClass.cbWndExtra = 0;
  WndClass.lpfnWndProc = WndProc;
  WndClass.hCursor = LoadCursor (NULL, IDC_ARROW);
  RegisterClass (&WndClass);

  if (sink->full_screen) {
    /* This doesn't seem to work, it returns the wrong values! But when we
     * later use ShowWindow to show it maximized, it goes to full-screen
     * anyway. TODO: Figure out why. */
    width = GetSystemMetrics (SM_CXFULLSCREEN);
    height = GetSystemMetrics (SM_CYFULLSCREEN);
    offx = 0;
    offy = 0;

    style = WS_POPUP; /* No window decorations */
    exstyle = 0;

  } else {
    /* By default, create a normal top-level window, the size 
     * of the video.
     */
    RECT rect;
	
    /* targetWidth is the aspect-ratio-corrected size of the video. */
    /* GetSystemMetrics() returns the width of the dialog's border (doubled b/c of left and right borders). */
    width = sink->targetWidth + GetSystemMetrics (SM_CXSIZEFRAME) * 2;
    height = sink->targetHeight + GetSystemMetrics (SM_CYCAPTION) + (GetSystemMetrics (SM_CYSIZEFRAME) * 2);

    SystemParametersInfo (SPI_GETWORKAREA, NULL, &rect, 0);
    int screenwidth = rect.right - rect.left;
    int screenheight = rect.bottom - rect.top;
    offx = rect.left;
    offy = rect.top;

    /* Make it fit into the screen without changing the
     * aspect ratio. */
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
  }

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

  switch (sink->fourcc) {
    case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
      sink->d3dformat = D3DFMT_X8R8G8B8;
      sink->d3dfourcc = (D3DFORMAT)MAKEFOURCC('Y', 'U', 'Y', '2');
      break;
    case GST_MAKE_FOURCC ('Y', 'U', 'Y', 'V'):
      sink->d3dformat = D3DFMT_X8R8G8B8;
      sink->d3dfourcc = (D3DFORMAT)MAKEFOURCC('Y', 'U', 'Y', 'V');
      break;
    case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
      sink->d3dformat = D3DFMT_X8R8G8B8;
      sink->d3dfourcc = (D3DFORMAT)MAKEFOURCC('U', 'Y', 'V', 'Y');
      break;
    case GST_MAKE_FOURCC ('Y', 'V', '1', '2'):
      sink->d3dfourcc = (D3DFORMAT)MAKEFOURCC('Y', 'V', '1', '2');
      sink->d3dformat = D3DFMT_X8R8G8B8;
      break;
    default:
      GST_WARNING ("Failed to find compatible Direct3D format");
      goto destroy_window;
  }

  sink->d3d = Direct3DCreate9(D3D_SDK_VERSION);

  if (FAILED(sink->d3d->CheckDeviceFormatConversion(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, sink->d3dfourcc, sink->d3dformat))) {
    /* Prevent memory leak */
    sink->d3d->Release();
    sink->d3d = NULL;

    GST_WARNING ("Failed to find compatible Direct3D format");
    goto destroy_window;
  }

  if (SUCCEEDED(sink->d3d->CheckDeviceFormat(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, sink->d3dformat, D3DUSAGE_DEPTHSTENCIL, D3DRTYPE_SURFACE, D3DFMT_D32))) {
    sink->d3dstencilformat = D3DFMT_D32;
    sink->d3dEnableAutoDepthStencil = TRUE;
  } else if (SUCCEEDED(sink->d3d->CheckDeviceFormat(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, sink->d3dformat, D3DUSAGE_DEPTHSTENCIL, D3DRTYPE_SURFACE, D3DFMT_D24X8))) {
    sink->d3dstencilformat = D3DFMT_D24X8;
    sink->d3dEnableAutoDepthStencil = TRUE;
  } else if (SUCCEEDED(sink->d3d->CheckDeviceFormat(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, sink->d3dformat, D3DUSAGE_DEPTHSTENCIL, D3DRTYPE_SURFACE, D3DFMT_D16))) {
    sink->d3dstencilformat = D3DFMT_D16;
    sink->d3dEnableAutoDepthStencil = TRUE;
  } else {
    sink->d3dstencilformat = D3DFMT_X8R8G8B8;
    sink->d3dEnableAutoDepthStencil = FALSE;
    GST_WARNING ("Unable to select depth buffer");
  }
  
  D3DPRESENT_PARAMETERS d3dpp;
  ZeroMemory(&d3dpp, sizeof(d3dpp));
  d3dpp.Windowed = TRUE;
  d3dpp.SwapEffect = D3DSWAPEFFECT_DISCARD;
  d3dpp.hDeviceWindow = video_window;
  d3dpp.BackBufferCount = 1;
  d3dpp.BackBufferFormat = sink->d3dformat;
  d3dpp.BackBufferWidth = sink->targetWidth;
  d3dpp.BackBufferHeight = sink->targetHeight;
  d3dpp.EnableAutoDepthStencil = sink->d3dEnableAutoDepthStencil;
  d3dpp.AutoDepthStencilFormat = sink->d3dstencilformat;

  if (FAILED(sink->d3d->CreateDevice(
    D3DADAPTER_DEFAULT, 
    D3DDEVTYPE_HAL, 
    video_window, 
    D3DCREATE_SOFTWARE_VERTEXPROCESSING, 
    &d3dpp, 
    &(sink->d3ddev)
  ))) {
    /* Prevent memory leak */
    sink->d3d->Release();
    sink->d3d = NULL;

    GST_WARNING ("Unable to create Direct3D device");
    goto destroy_window;
  }

  if (FAILED(sink->d3ddev->GetDeviceCaps(
    &sink->d3dcaps
  ))) {
    /* Prevent memory leak */
    sink->d3d->Release();
    sink->d3d = NULL;

    GST_WARNING ("Unable to retrieve Direct3D device caps");
    goto destroy_window;
  }

  //Create offscreen plain surface
  sink->d3ddev->CreateOffscreenPlainSurface(sink->width, sink->height, sink->d3dfourcc, D3DPOOL_DEFAULT, &sink->d3dsurface, NULL);

  //Get a pointer to the backbuffer
  sink->d3ddev->GetBackBuffer(0, 0, D3DBACKBUFFER_TYPE_MONO, &sink->d3dbackbuffer);

  /* Now show the window, as appropriate */
  if (sink->full_screen) {
    ShowWindow (video_window, SW_SHOWMAXIMIZED);
    ShowCursor (FALSE);
  } else {
    ShowWindow (video_window, SW_SHOWNORMAL);
  }

  /* Trigger the initial paint of the window */
  UpdateWindow (video_window);

  ReleaseSemaphore (sink->window_created_signal, 1, NULL);

  /* start message loop processing our default window messages */
  MSG msg;
  while(TRUE)
  {
    while(PeekMessage(&msg, NULL, 0, 0, PM_REMOVE))
    {
      TranslateMessage(&msg);
      DispatchMessage(&msg);
    }
    
    if(msg.message == WM_QUIT)
      break;
    
    gst_d3dvideosink_update(GST_BASE_SINK(sink));
  }

  sink->d3dbackbuffer->Release();
  sink->d3dbackbuffer = NULL;

  sink->d3dsurface->Release();
  sink->d3dsurface = NULL;

  sink->d3ddev->Release();
  sink->d3ddev = NULL;

  sink->d3d->Release();
  sink->d3d = NULL;

  UnregisterClass(WndClass.lpszClassName, WndClass.hInstance);
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
  sink->window_created_signal = CreateSemaphore (NULL, 0, 1, NULL);
  if (sink->window_created_signal == NULL)
    goto failed;

  sink->window_thread = g_thread_create (
      (GThreadFunc) gst_d3dvideosink_window_thread, sink, TRUE, NULL);

  /* wait maximum 10 seconds for window to be created */
  if (WaitForSingleObject (sink->window_created_signal,
          10000) != WAIT_OBJECT_0)
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
  HWND videowindow = (HWND)window_id;

  if (videowindow == sink->window_id) {
    GST_DEBUG_OBJECT (sink, "Window already set");
    return;
  }

  /* TODO: What if we already have a window? What if we're already playing? */
  sink->window_id = videowindow;
}

static void gst_d3dvideosink_set_window_for_renderer (GstD3DVideoSink *sink)
{
  /* Application has requested a specific window ID */
  sink->prevWndProc = (WNDPROC) SetWindowLong (sink->window_id, GWL_WNDPROC, (LONG)WndProcHook);
  GST_DEBUG_OBJECT (sink, "Set wndproc to %p from %p", WndProcHook, sink->prevWndProc);
  SetProp (sink->window_id, L"GstD3DVideoSink", sink);
  /* This causes the new WNDPROC to become active */
  SetWindowPos (sink->window_id, 0, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);

  //if (!sink->renderersupport->SetRendererWindow (sink->window_id)) {
  //  GST_WARNING_OBJECT (sink, "Failed to set HWND %x on renderer", sink->window_id);
  //  return;
  //}
  sink->is_new_window = FALSE;

  /* This tells the renderer where the window is located, needed to 
   * start drawing in the right place.  */
  //sink->renderersupport->MoveWindow();
  GST_INFO_OBJECT (sink, "Set renderer window to %x", sink->window_id);
}

static void
gst_d3dvideosink_prepare_window (GstD3DVideoSink *sink)
{
  HRESULT hres;

  /* Give the app a last chance to supply a window id */
  if (!sink->window_id) {
    gst_x_overlay_prepare_xwindow_id (GST_X_OVERLAY (sink));
  }

  /* If the app supplied one, use it. Otherwise, go ahead
   * and create (and use) our own window */
  if (sink->window_id) {
    gst_d3dvideosink_set_window_for_renderer (sink);
  }
  else {
    gst_d3dvideosink_create_default_window (sink);
  }
}

static GstStateChangeReturn
gst_d3dvideosink_change_state (GstElement * element, GstStateChange transition)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (element);
  GstStateChangeReturn ret, rettmp;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      gst_d3dvideosink_clear (sink);
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

  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (bsink);
  gst_caps_ref(caps);
  sink->caps = caps;

  /* Do what you must here to convert caps into something Direct3D can use */
  GstStructure *s = gst_caps_get_structure (caps, 0);
  const gchar *name = gst_structure_get_name (s);

  gchar *capsstring = gst_caps_to_string (caps);
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

  if (sink->is_new_window) {
    SendMessage (sink->window_id, WM_CLOSE, NULL, NULL);
    while (!sink->window_closed);
    sink->is_new_window = FALSE;
  }

  return TRUE;
}

static GstFlowReturn
gst_d3dvideosink_show_frame (GstVideoSink *vsink, GstBuffer *buffer)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (vsink);
  GstFlowReturn ret;
  GstStateChangeReturn retst;

  if (sink->window_closed) {
    GST_WARNING_OBJECT (sink, "Window has been closed, stopping");
    return GST_FLOW_ERROR;
  }

  if (!sink->d3ddev) {
    return GST_FLOW_RESEND;
  }

  //RECT srcRect;
  //srcRect.left = 0;
  //srcRect.top = 0;
  //srcRect.bottom = sink->height;
  //srcRect.right = sink->width;
  //D3DXLoadSurfaceFromMemory(sink->d3dsurface, NULL, NULL, GST_BUFFER_DATA(buffer), sink->d3dfourcc, sink->width, NULL, &srcRect, D3DX_FILTER_NONE, D3DCOLOR_ARGB(255, 0, 0, 0));

  //Get a reference to the buffer containing the image we want to display on screen
  guint8 *source = GST_BUFFER_DATA(buffer);

  //Render
  sink->d3ddev->Clear(0, NULL, D3DCLEAR_TARGET, D3DCOLOR_XRGB(0, 0, 0), 1.0f, 0);
  sink->d3ddev->BeginScene();

  //Do your thing
  if (source) {
    D3DLOCKED_RECT lr;

    sink->d3dsurface->LockRect(&lr, NULL, 0);
    guint8 *dest = (guint8 *)lr.pBits;

    GST_DEBUG("DEST BUFFER: %d", dest);

    if (dest) {
      switch(sink->fourcc) 
      {
        case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
        case GST_MAKE_FOURCC ('Y', 'U', 'Y', 'V'):
        case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
          /* Nice and simple */
          int srcstride = GST_ROUND_UP_4 (sink->width * 2);
          int dststride = lr.Pitch;

          for (int i = 0; i < sink->height; i++) {
            memcpy (dest + dststride * i, source + srcstride * i, srcstride);
          }
          break;
      }
    }
    
    sink->d3dsurface->UnlockRect();
  }

  sink->d3ddev->StretchRect(sink->d3dsurface, NULL, sink->d3dbackbuffer, NULL, D3DTEXF_NONE);

  sink->d3ddev->EndScene();
  sink->d3ddev->Present(NULL, NULL, NULL, NULL);

  //gst_buffer_unref(buffer);
  

  GST_DEBUG("SHOWING FRAME");



  //GST_DEBUG_OBJECT (sink, "Pushing buffer through fakesrc->renderer");
  //GST_D3DVIDEOSINK_GRAPH_LOCK(sink);
  //if (!sink->graph_running){
  //  retst = gst_d3dvideosink_start_graph(sink);
  //  if (retst == GST_STATE_CHANGE_FAILURE)
  //    return GST_FLOW_WRONG_STATE;
  //}
  //ret = sink->fakesrc->GetOutputPin()->PushBuffer (buffer);
  //if (!sink->graph_running){
  //  retst = gst_d3dvideosink_pause_graph(sink);
  //  if (retst == GST_STATE_CHANGE_FAILURE)
  //    return GST_FLOW_WRONG_STATE;
  //}
  //GST_D3DVIDEOSINK_GRAPH_UNLOCK(sink);
  //GST_DEBUG_OBJECT (sink, "Done pushing buffer through fakesrc->renderer: %s", gst_flow_get_name(ret));

  return GST_FLOW_OK;
}

static void
gst_d3dvideosink_expose(GstXOverlay * overlay)
{
  GstD3DVideoSink *sink = GST_D3DVIDEOSINK (overlay);
  GstBuffer *last_buffer;

  last_buffer = gst_base_sink_get_last_buffer(GST_BASE_SINK(sink));
  if (last_buffer != NULL)
    gst_d3dvideosink_show_frame(GST_VIDEO_SINK(sink), last_buffer);
}

static void 
gst_d3dvideosink_update(GstBaseSink * bsink) 
{
  GstBuffer *last_buffer;

  last_buffer = gst_base_sink_get_last_buffer(bsink);
  if (last_buffer != NULL)
    gst_d3dvideosink_show_frame(GST_VIDEO_SINK(bsink), last_buffer);
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

/* Plugin entry point */
extern "C" static gboolean
plugin_init (GstPlugin * plugin)
{
  /* PRIMARY: this is the best videosink to use on windows */
  if (!gst_element_register (plugin, "d3dvideosink",
          GST_RANK_PRIMARY, GST_TYPE_D3DVIDEOSINK))
    return FALSE;

  return TRUE;
}

extern "C" GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "d3dsinkwrapper",
    "Direct3D sink wrapper plugin",
    plugin_init, VERSION, "LGPL", GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
