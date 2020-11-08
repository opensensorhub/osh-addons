package org.sensorhub.impl.sensor.audio;

import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_TRACE;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;
import static org.bytedeco.ffmpeg.global.avutil.av_get_bytes_per_sample;
import static org.bytedeco.ffmpeg.global.avutil.av_get_sample_fmt_name;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
import static org.bytedeco.ffmpeg.global.avutil.av_sample_fmt_is_planar;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
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
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
//import static org.bytedeco.ffmpeg.global.avcodec.*;
//import static org.bytedeco.ffmpeg.global.avutil.*;
//import static org.bytedeco.ffmpeg.global.swscale.*;
//import static org.bytedeco.ffmpeg.global.avformat.*;


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

public class FfmpegReader extends Thread
{
	//
	AudioConfig config;
	AudioOutput output;
	Path testPath;
	boolean isDir = false;
	
	//  Native class variables
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

	//  Audo Metadata
	int sampleRate;
	int bytesPerSample;
	int bitsPerSample;
	private double floatScale;				// Scaling factor used for int <-> float conversion				

	
	public FfmpegReader(AudioConfig config, AudioOutput output) {
		this.config = config;
		this.output = output;
		this.testPath = Paths.get(config.wavDir);
		isDir = testPath.toFile().isDirectory();
	}
	
//	public void findFormatInfo(String url) {
//		formatCtx  = avformat_alloc_context();
//		avformat_open_input(formatCtx, url, null, null);
//	}
//
	void printStreamInformation(AVCodec decoder, AVCodecContext decode_ctx, int audioStreamIndex) {
		System.err.println("Codec: " + decoder.long_name());
		System.err.printf("---------\n");
		System.err.printf("Stream:        %7d\n", audioStreamIndex);
		System.err.printf("Sample Format: %7s\n", av_get_sample_fmt_name(decode_ctx.sample_fmt()));
		System.err.printf("Sample Rate:   %7d\n", decode_ctx.sample_rate());
		System.err.printf("Sample Size:   %7d\n", av_get_bytes_per_sample(decode_ctx.sample_fmt()));
		av_get_bits_per_sample(decode_ctx.sample_fmt());
		System.err.printf("Channels:      %7d\n", decode_ctx.channels());
	}

	public void init(String inputFile) {
		// init FFMPEG objects
		av_log_set_level(AV_LOG_TRACE);

		// AVFormatContext
		formatCtx  = avformat_alloc_context();
		avformat_open_input(formatCtx, inputFile, null, null);

		av_dump_format(formatCtx, 0, inputFile, 0);
		
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

//		printStreamInformation(decoder, decodeCtx, 0);

		//  init frame and packet 
		avFrame = av_frame_alloc();
		avPacket = av_packet_alloc();
		av_init_packet(avPacket);
		
		//  Metadata
		BytePointer sampleFormatName = av_get_sample_fmt_name(decodeCtx.sample_fmt());
		bytesPerSample = av_get_bytes_per_sample(decodeCtx.sample_fmt());
		//bitsPerSample = av_get_bits_per_sample(decodeCtx.sample_fmt());  // returns 0 for wav CODEC
		bitsPerSample = bytesPerSample * 8;
		// Calculate the scaling factor for converting to a normalised double
		// TODO = Generalze for other formats/codecs
		if (bitsPerSample > 8) {
			// If more than 8 validBits, data is signed
			// Conversion required dividing by magnitude of max negative value
			int floatOffset = 0;
			floatScale = 1 << (bitsPerSample - 1);
		} else {
			// Else if 8 or less validBits, data is unsigned
			// Conversion required dividing by max positive value
			int floatOffset = -1;
			floatScale = 0.5 * ((1 << bitsPerSample) - 1);
		}

	}
	
	@Override
	public void run() {
		init(config.wavFile);
		try {
			read();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void read() throws InterruptedException {
		boolean eof = false;
		AVInputFormat avFormat;
		int frameCnt = 0;
		while(!eof) {
			int res = av_read_frame(formatCtx, avPacket);
			if (res == AVERROR_EOF) {
				System.err.println("EOF reached");
				break;
			}
			if (res == AVERROR_EAGAIN()) {
//				System.err.println("EOF reached");
				// don't think we care about this one
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
			// ?? av_packet_unref(avPacket);

			// receive and handle frame(s)
			while(res == 0) {
				//				nativeData.position(0);
				//				nativeData.limit(0);
				//				nativeData.put(frameData);
				//				avPacket.data(nativeData);
				//avPacket.size(frameData.length);  //  this does not affect underlying buffer as retrieved by avFrame.getExtendedData
				res = avcodec_receive_frame(decodeCtx, avFrame);
				if(res != 0) {
					System.err.println("Receive Frame Error result = " + res);
					continue;
				}
				// handle frame
				int sampleFmt = av_sample_fmt_is_planar(decodeCtx.sample_fmt());
				// This means that the data of each channel is in its own buffer.
				// => frame->extended_data[i] contains data for the i-th channel.
				System.err.println("Frame : " + frameCnt);
				int numSamples = avFrame.nb_samples();
				System.err.println("FrameSamples: " + numSamples);
//				for(int s = 0; s < avFrame.nb_samples(); ++s) {
					for(int c = 0; c < decodeCtx.channels(); ++c) {
//						float sample = getSample(decodeCtx, avFrame.extended_data(c), s);
						byte [] tbuff = new byte[numSamples]; 
						BytePointer buff = avFrame.extended_data(c);
						buff.get(tbuff);
						double [] sampleData = new double[config.numSamplesPerArray];
						int sampleCnt = 0;
						int arrayCnt = 0;
						for(int i=0; i<numSamples; i+=2) {
							double lval = normalizeSample(tbuff[i], tbuff[i+1]);
							System.err.printf(" [%d] %x %x %f: ", i,tbuff[i], tbuff[i+1], lval);
							sampleData[sampleCnt++] = lval;
							if(sampleCnt == config.numSamplesPerArray ) {
								output.publishChunk(sampleData, (sampleCnt * arrayCnt), decodeCtx.sample_rate());
								sampleData = new double[config.numSamplesPerArray];
								sampleCnt = 0;
								arrayCnt++;
//								Thread.sleep(1000L);
							}
						}
					}
//				}
				System.err.println();

				av_frame_unref(avFrame);
			}
			frameCnt++;
		}
	}

	//  TODO: generalize for different audio formats/codecs
	float getSample(AVCodecContext codecCtx, byte sample1, byte sample2) {
		long val = 0;
		float ret = 0;
		int sampleSize = av_get_bytes_per_sample(codecCtx.sample_fmt());

		switch(sampleSize) {
		case 1:
			// 8bit samples are always unsigned
//			val = REINTERPRET_CAST(uint8_t*, buffer)[sampleIndex];
			// make signed
//			val -= 127;
			break;

		case 2:
//			val = REINTERPRET_CAST(int16_t*, buffer)[sampleIndex];
//			return ByteBuffer.wrap(buff.).getFloat();
//			buff.byte
//			byte [] tbuff = new byte[2]; 
//			buff.get(tbuff);
			byte [] tbuff4 = { 0, 0, sample2, sample1 };
			float f = ByteBuffer.wrap(tbuff4).getFloat();
			return f;
		case 4:
//			val = REINTERPRET_CAST(int32_t*, buffer)[sampleIndex];
			break;

		case 8:
//			val = REINTERPRET_CAST(int64_t*, buffer)[sampleIndex];
			break;

		default:
			System.err.printf("Invalid sample size %d.\n", sampleSize);
			return 0;
		}

		return 0.0F;
	}

	/**
		v = -8, fffffff8
		v &= ff  (248, f8000000)
		val = 248
		
		v = b1 (-1, FFFFFFFF)
		tmp = v << 8 (-256,  FFFFFF00)
		val += tmp (-8)
	 */
	public double normalizeSample(byte b0, byte b1) {
		//  Assumes twosComp, so will need to generalize this
		long res = (b0 & 0xff) + (b1 << 8);
		return (double)res/ floatScale;
	}

	public static void main_(String[] args) throws MalformedURLException {
//		FfmpegReader reader = new FfmpegReader();
//		String filepath = "C:/Users/tcook/root/sensorHub/audio/wavFiles/GUI_Decode_2019-03-27 132356.wav";
//		String url = Paths.get(filepath).toUri().toURL().toString();
//		reader.init(filepath);
//		reader.read();
	}
}
