# FFMPEG Transcoder

## Overview

This module provides a process module that can decode and/or encode video.

## Configuration

When added to an OpenSensorHub node, the process has the following configuration options:

- **General:**
    - **Module ID:** *Not editable.*
      UUID automatically assigned by OpenSensorHub for this process instance.
    - **Module Class:** *Not editable.*
      The fully qualified name of the Java class implementing the process.
    - **Module Name:**
      A name for the instance of the process.
      Should be set to something short and human-readable that describes the upstream source of data.
    - **Description:** (Optional)
      URL to a SensorML description document for the process.
    - **Video Process ID:**
      A string that uniquely identifies this process in this OpenSensorHub node.
      This is used to differentiate between multiple processes of the same type.
    - **Video Source:**
      A module with video output. Once the transcoder starts, video from this source module
      will be decoded/encoded and outputted from the transcoder.
    - **Input Codec:** (Optional)
      The codec used for decoding the incoming video data. If incoming video is uncompressed,
      select either RGB or YUV.
    - **Output Codec:**
      The codec used for encoding the outgoing video data. If outgoing video should be uncompressed,
      select either RGB or YUV.
    - **Output Width:** (Optional)
      The width of the output video frame. Leave this empty to avoid scaling the video frame size.
    - **Output Height:** (Optional)
      The height of the output video frame. Leave this empty to avoid scaling the video frame size.
    - **Auto Start:**
      If checked, automatically start this sensor when the OpenSensorHub node is launched.
    - **Automatically Detect Input Codec:**
      If checked, automatically determine the input video codec based on the input's encoding information.