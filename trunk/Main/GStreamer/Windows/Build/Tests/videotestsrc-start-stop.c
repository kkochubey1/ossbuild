/* GStreamer
 *
 * Copyright (C) 2009 Wim Taymans <wim.taymans@gmail.com>
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

#include <gst/gst.h>

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <windows.h>

typedef struct _App App;

struct _App
{
  GstElement *pipeline;

  GMainLoop *loop;
};

App s_app;

static void
test_thread (App* app)
{
	int i;
	for(i = 0; i < 300; ++i) {
		g_print ("Playing...\n");
		gst_element_set_state (app->pipeline, GST_STATE_PLAYING);
		Sleep(100);
		g_print ("Null...\n");
		gst_element_set_state (app->pipeline, GST_STATE_NULL);
	}
	g_print ("Test complete\n");
	gst_object_unref (app->pipeline);
	g_main_loop_quit(app->loop);
}

int
main (int argc, char *argv[])
{
  App *app = &s_app;
  GstElement* pipeline;
  GstElement* videotestsrc;
  GstElement* ffmpegcolorspace;
  GstElement* videosink;

  gst_init (&argc, &argv);

  ///* create a mainloop to get messages */
  app->loop = g_main_loop_new (NULL, TRUE);

  g_message ("creating elements");

  pipeline = gst_pipeline_new("pipeline");
  g_assert (pipeline);
  
  videotestsrc = gst_element_factory_make ("videotestsrc", "videotestsrc");
  g_assert (videotestsrc);

  ffmpegcolorspace = gst_element_factory_make ("ffmpegcolorspace", "ffmpegcolorspace");
  g_assert (ffmpegcolorspace);

  videosink = gst_element_factory_make ("d3dvideosink", "videosink");
  g_assert (videosink);

  gst_bin_add_many(GST_BIN(pipeline), videotestsrc, ffmpegcolorspace, videosink, NULL);
  gst_element_link_many(videotestsrc, ffmpegcolorspace, videosink, NULL);

  g_message ("elements added and linked");

  app->pipeline = pipeline;
  g_thread_create ((GThreadFunc)test_thread, app, FALSE, NULL);

  /* this mainloop is stopped when we receive an error or EOS */
  g_main_loop_run (app->loop);

  g_message ("stopping");

  //gst_element_set_state (app->pipeline, GST_STATE_NULL);

  g_main_loop_unref (app->loop);

  return 0;
}