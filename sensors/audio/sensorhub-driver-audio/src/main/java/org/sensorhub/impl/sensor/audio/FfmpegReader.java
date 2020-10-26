package org.sensorhub.impl.sensor.audio;

import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_TRACE;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

/**
 * 
 * 
AVERROR_EOF: -541478725
AVERROR_INPUT_CHANGED: -1668179713
AVERROR_EAGAIN(): -11
AVERROR_EINVAL(): -22
 * 
 * 
 * @author tcook
 *
 */

public class FfmpegReader {

	AVCodec decoder = null;
	AVCodec encoder = null;
	AVFormatContext formatCtx;
	AVCodecContext decodeCtx = null;
	AVCodecContext encodeCtx = null;
	SwsContext sws_ctx = null;
	AVFrame avFrame = null;
	AVFrame sws_frame = null;
	AVPacket avPacket = null;
	AVPacket enc_pkt = null;
	static final String DECODER = "pcm_s16le"; // "AV_CODEC_ID_PCM_S16LE";
	//	static final String DECODER = "h264";

	public void findFormatInfo(String url) {
		//		AVInputFormat avFormat = 
		formatCtx  = avformat_alloc_context();
		avformat_open_input(formatCtx, url, null, null);
		//  Following call is Throwing #  EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x00007ffa8d854635, pid=23288, tid=9072
		//		avformat_find_stream_info(avfCtx, (PointerPointer<?>)null);
	}

	void printStreamInformation(AVCodec decoder, AVCodecContext decode_ctx, int audioStreamIndex) {
		System.err.println("Codec: " + decoder.long_name());
		//	    if(codec->sample_fmts != NULL) {
		//	        fprintf(stderr, "Supported sample formats: ");
		//	        for(int i = 0; codec->sample_fmts[i] != -1; ++i) {
		//	            fprintf(stderr, "%s", av_get_sample_fmt_name(codec->sample_fmts[i]));
		//	            if(codec->sample_fmts[i+1] != -1) {
		//	                fprintf(stderr, ", ");
		//	            }
		//	        }
		//	        fprintf(stderr, "\n");
		//	    }
		System.err.printf("---------\n");
		System.err.printf("Stream:        %7d\n", audioStreamIndex);
		System.err.printf("Sample Format: %7s\n", av_get_sample_fmt_name(decode_ctx.sample_fmt()));
		System.err.printf("Sample Rate:   %7d\n", decode_ctx.sample_rate());
		System.err.printf("Sample Size:   %7d\n", av_get_bytes_per_sample(decode_ctx.sample_fmt()));
		System.err.printf("Channels:      %7d\n", decode_ctx.channels());
		//	    System.err.printf("Float Output:  %7s\n", !RAW_OUT_ON_PLANAR || av_sample_fmt_is_planar(codecCtx->sample_fmt) ? "yes" : "no");
	}

	public void init(String inputFile) {
		// init FFMPEG objects
		av_log_set_level(AV_LOG_TRACE);

		// AVFormatContext
		formatCtx  = avformat_alloc_context();
		avformat_open_input(formatCtx, inputFile, null, null);

		// init decoder context
		decoder = avcodec_find_decoder_by_name(DECODER); //
		//		System.err.println(decoder);
		decodeCtx = avcodec_alloc_context3(decoder);
		//  Have to manually set these  or avcodec_open2() fails
		decodeCtx.sample_rate(8000);
		decodeCtx.channels(1);

		// init decoder
		int status = avcodec_open2(decodeCtx, decoder, (PointerPointer<?>)null);
		if (status < 0) {
			throw new IllegalStateException("Error initializing decoder " + DECODER);
		}

		printStreamInformation(decoder, decodeCtx, 0);

		//  init frame and packet 
		avFrame = av_frame_alloc();
		avPacket = av_packet_alloc();
		av_init_packet(avPacket);
	}

	public void read() {
		boolean eof = false;
		AVInputFormat avFormat;
		int frameCnt = 0;
		while(!eof) {
			int res = av_read_frame(formatCtx, avPacket);
			if (res == AVERROR_EOF) {
				System.err.println("EOF reached");
				break;
			}
			if(res != 0) {
				System.err.println("Read Frame Error result = " + res);
				break;
			}
			if(avPacket.stream_index() != 0) {  // TODO, support multiple streams?
				// Free the buffers used by the frame and reset all fields.
				av_packet_unref(avPacket);
				continue;
			}
			//  Send packet to decoder
			res = avcodec_send_packet(decodeCtx, avPacket);
			if(res != 0) {
				System.err.println("Send packet Error result = " + res);
				av_packet_unref(avPacket);
				break;
			}
			 // The packet was sent successfully. We don't need it anymore.
		    // => Free the buffers used by the frame and reset all fields.
			av_packet_unref(avPacket);

			// receive and handle frame(s)
			while(res == 0) {
				byte [] frameData = new byte[200];

//				nativeData.position(0);
//				nativeData.limit(0);
//				nativeData.put(frameData);
//				avPacket.data(nativeData);
				avPacket.size(frameData.length);
//				int sendRes = avcodec_send_packet(decodeCtx, avPacket);
				res = avcodec_receive_frame(decodeCtx, avFrame);
				if(res != 0) {
					//System.err.println("Receive Frame Error result = " + res);
					continue;
				}
				// handle frame
				int sampleFmt = av_sample_fmt_is_planar(decodeCtx.sample_fmt());
				// This means that the data of each channel is in its own buffer.
		        // => frame->extended_data[i] contains data for the i-th channel.
				System.err.println("Frame : " + frameCnt);
				for(int s = 0; s < avFrame.nb_samples(); ++s) {
		            for(int c = 0; c < decodeCtx.channels(); ++c) {
		                float sample = 0;//getSample(decodeCtx, avFrame.extended_data(c), s);
		                System.err.printf("%d, ",s);
		            }
		        }
		        System.err.println();
				
				av_frame_unref(avFrame);
//				int cnt = 0;
//				for(byte b: frameData) {
//					System.err.print(b + " ");
//					if(++cnt % 40 == 0)
//						System.err.println();
//				}
//				System.err.println("\n");
				

			}
			frameCnt++;
		}
	}

	public void read2() {
		AVInputFormat avFormat;
		AVDictionary options = new AVDictionary(null);

		int status = avcodec_open2(decodeCtx, decoder, (PointerPointer<?>)null);
		if (status < 0) {
			throw new IllegalStateException("Error initializing decoder " + DECODER);
		}

		// decode frame
		BytePointer nativeData = new BytePointer(20000);
		byte [] frameData = new byte[20000];
		int ret1 = avcodec_send_packet(decodeCtx, avPacket);
		for(int i=0; i<4000; i++) {
			avPacket.data(nativeData);
			avPacket.size(frameData.length);
			//			int ret1 = avcodec_send_packet(decode_ctx, dec_pkt);
			int ret2 = avcodec_receive_frame(decodeCtx, avFrame);
			av_packet_unref(avPacket);

			System.err.println("Frame, ret1, ret1 : " + i + ", " + ret1 + ", " + ret2);
			int cnt = 0;
			for(byte b: frameData) {
				System.err.print(b + " ");
				if(cnt++ % 40 == 0)
					System.err.println();
			}
			System.err.println("\n");
		}
	}

	public static void main(String[] args) throws MalformedURLException {
		FfmpegReader reader = new FfmpegReader();
		String filepath = "C:/Users/tcook/root/sensorHub/audio/wavFiles/GUI_Decode_2019-03-27 132356.wav";
		String url = Paths.get(filepath).toUri().toURL().toString();
		reader.init(filepath);
		reader.read();
		//		reader.findFormatInfo(url);
	}
}
