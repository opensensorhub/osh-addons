# FFMPEG Transcoder

## Overview

FFmpeg video decode/encode/transcode module.
Input and output may be compressed or uncompressed video.

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
    - **Input Format:** (Optional)
      The format used for decoding the incoming video data. If incoming video is uncompressed,
      select either RGB or YUV.
    - **Input Format Override:** (Optional)
      Manually specify any input codec or pixel format, allowing for any format not available in the short Input Codec list.
      Only required if the format is not in the Input Format list. Otherwise, leave blank.
      A full list of codecs and pixel formats is available at the end of this file.
    - **Output Format:**
      The format used for encoding the outgoing video data. If outgoing video should be uncompressed,
      select either RGB or YUV.
    - **Output Format Override:** (Optional)
      Manually specify any output codec or pixel format, allowing for any format not available in the short Output Codec list.
      Only required if the format is not in the Output Format list. Otherwise, leave blank.
      A full list of codecs and pixel formats is available at the end of this file.
    - **Output Width:** (Optional)
      The width of the output video frame. Leave this empty to avoid scaling the video frame size.
    - **Output Height:** (Optional)
      The height of the output video frame. Leave this empty to avoid scaling the video frame size.
    - **Auto Start:**
      If checked, automatically start this sensor when the OpenSensorHub node is launched.
    - **Automatically Detect Input Format:**
      If checked, automatically determine the input video format based on the input's encoding information.

## All Supported Formats
This list contains all the supported codecs and pixel formats. To use a format, copy it into one of the format
override config fields.

| Codecs (Compressed Video) | Pixel Formats (Uncompressed Video) |
|---------------------------|------------------------------------|
| H264                      | YUV420P                            |
| HEVC                      | YUV422P                            |
| MJPEG                     | YUV444P                            |
| VP8                       | YUV410P                            |
| VP9                       | YUV411P                            |
| MPEG2                     | YUV440P                            |
| MPEG4                     | YUVJ420P                           |
| AV1                       | YUVJ422P                           |
| THEORA                    | YUVJ444P                           |
| MPEG1VIDEO                | YUVJ440P                           |
| WMV1                      | YUV420P9BE                         |
| WMV2                      | YUV420P9LE                         |
| WMV3                      | YUV420P10BE                        |
| VC1                       | YUV420P10LE                        |
| FLV1                      | YUV420P12BE                        |
| FLASHSV                   | YUV420P12LE                        |
| FLASHSV2                  | YUV420P14BE                        |
| RV10                      | YUV420P14LE                        |
| RV20                      | YUV420P16BE                        |
| RV30                      | YUV420P16LE                        |
| RV40                      | YUV422P9BE                         |
| CINEPAK                   | YUV422P9LE                         |
| INDEO2                    | YUV422P10BE                        |
| INDEO3                    | YUV422P10LE                        |
| INDEO4                    | YUV422P12BE                        |
| INDEO5                    | YUV422P12LE                        |
| MSMPEG4V1                 | YUV422P14BE                        |
| MSMPEG4V2                 | YUV422P14LE                        |
| MSMPEG4V3                 | YUV422P16BE                        |
| H261                      | YUV422P16LE                        |
| H263                      | YUV444P9BE                         |
| H263I                     | YUV444P9LE                         |
| H263P                     | YUV444P10BE                        |
| SNOW                      | YUV444P10LE                        |
| SVQ1                      | YUV444P12BE                        |
| SVQ3                      | YUV444P12LE                        |
| DVVIDEO                   | YUV444P14BE                        |
| HUFFYUV                   | YUV444P14LE                        |
| FFVHUFF                   | YUV444P16BE                        |
| FFV1                      | YUV444P16LE                        |
| ASV1                      | YUYV422                            |
| ASV2                      | UYVY422                            |
| VCR1                      | YVYU422                            |
| CLJR                      | UYYVYY411                          |
| MDEC                      | NV12                               |
| ROQ                       | NV21                               |
| INTERPLAY_VIDEO           | NV16                               |
| XAN_WC3                   | NV20LE                             |
| XAN_WC4                   | NV20BE                             |
| RPZA                      | NV24                               |
| SMC                       | NV42                               |
| GIF                       | RGB24                              |
| PNG                       | BGR24                              |
| PPM                       | ARGB                               |
| PBM                       | RGBA                               |
| PGM                       | ABGR                               |
| PAM                       | BGRA                               |
| BMP                       | RGB0                               |
| TIFF                      | BGR0                               |
| SGI                       | RGB8                               |
| ALIAS_PIX                 | BGR8                               |
| DPX                       | RGB4                               |
| EXR                       | BGR4                               |
| WEBP                      | RGB4_BYTE                          |
| DIRAC                     | BGR4_BYTE                          |
| DNXHD                     | RGB48BE                            |
| PRORES                    | RGB48LE                            |
| JPEG2000                  | RGBA64BE                           |
| JPEGLS                    | RGBA64LE                           |
| HAP                       | BGR48BE                            |
|                           | BGR48LE                            |
|                           | BGRA64BE                           |
|                           | BGRA64LE                           |
|                           | RGB565BE                           |
|                           | RGB565LE                           |
|                           | RGB555BE                           |
|                           | RGB555LE                           |
|                           | RGB444BE                           |
|                           | RGB444LE                           |
|                           | BGR565BE                           |
|                           | BGR565LE                           |
|                           | BGR555BE                           |
|                           | BGR555LE                           |
|                           | BGR444BE                           |
|                           | BGR444LE                           |
|                           | GRAY8                              |
|                           | GRAY8A                             |
|                           | GRAY9BE                            |
|                           | GRAY9LE                            |
|                           | GRAY10BE                           |
|                           | GRAY10LE                           |
|                           | GRAY12BE                           |
|                           | GRAY12LE                           |
|                           | GRAY14BE                           |
|                           | GRAY14LE                           |
|                           | GRAY16BE                           |
|                           | GRAY16LE                           |
|                           | MONOWHITE                          |
|                           | MONOBLACK                          |
|                           | YA8                                |
|                           | YA16BE                             |
|                           | YA16LE                             |
|                           | YUVA420P                           |
|                           | YUVA422P                           |
|                           | YUVA444P                           |
|                           | YUVA420P9BE                        |
|                           | YUVA420P9LE                        |
|                           | YUVA422P9BE                        |
|                           | YUVA422P9LE                        |
|                           | YUVA444P9BE                        |
|                           | YUVA444P9LE                        |
|                           | YUVA420P10BE                       |
|                           | YUVA420P10LE                       |
|                           | YUVA422P10BE                       |
|                           | YUVA422P10LE                       |
|                           | YUVA444P10BE                       |
|                           | YUVA444P10LE                       |
|                           | YUVA420P16BE                       |
|                           | YUVA420P16LE                       |
|                           | YUVA422P16BE                       |
|                           | YUVA422P16LE                       |
|                           | YUVA444P16BE                       |
|                           | YUVA444P16LE                       |
|                           | BAYER_BGGR8                        |
|                           | BAYER_RGGB8                        |
|                           | BAYER_GBRG8                        |
|                           | BAYER_GRBG8                        |
|                           | BAYER_BGGR16LE                     |
|                           | BAYER_BGGR16BE                     |
|                           | BAYER_RGGB16LE                     |
|                           | BAYER_RGGB16BE                     |
|                           | BAYER_GBRG16LE                     |
|                           | BAYER_GBRG16BE                     |
|                           | BAYER_GRBG16LE                     |
|                           | BAYER_GRBG16BE                     |
|                           | GRAYF32BE                          |
|                           | GRAYF32LE                          |
|                           | PAL8                               |
|                           | XYZ12LE                            |
|                           | XYZ12BE                            |
|                           | XTOP                               |
|                           | P010LE                             |
|                           | P010BE                             |
|                           | P016LE                             |
|                           | P016BE                             |
|                           | P210BE                             |
|                           | P210LE                             |
|                           | P410BE                             |
|                           | P410LE                             |
|                           | P216BE                             |
|                           | P216LE                             |
|                           | P416BE                             |
|                           | P416LE                             |