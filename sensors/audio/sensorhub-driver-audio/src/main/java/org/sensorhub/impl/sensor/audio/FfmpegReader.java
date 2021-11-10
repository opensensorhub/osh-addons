package org.sensorhub.impl.sensor.audio;

import static org.bytedeco.ffmpeg.global.avcodec.av_get_bits_per_sample;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
//import static org.bytedeco.ffmpeg.global.avformat.pr
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;
import static org.bytedeco.ffmpeg.global.avutil.av_get_bytes_per_sample;
import static org.bytedeco.ffmpeg.global.avutil.av_get_sample_fmt_name;
import static org.bytedeco.ffmpeg.global.avutil.av_sample_fmt_is_planar;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN;

import java.io.BufferedReader;

//import static org.bytedeco.ffmpeg.ffprobe

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.PointerPointer;

/**
 * 
 * 
 * AVERROR_EOF: -541478725 AVERROR_INPUT_CHANGED: -1668179713 AVERROR_EAGAIN():
 * -11 AVERROR_EINVAL(): -22
 * 
 * 
 * @author tcook
 *
 */

public class FfmpegReader extends Thread {
	AudioConfig config;
	AudioOutput output;

	// Native class variables
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
//	static final String DECODER = "mp3"; // "AV_CODEC_ID_PCM_S16LE";

	// Audo Metadata
	int sampleRate;
	int bytesPerSample;
	int bitsPerSample;
	private double floatScale; // Scaling factor used for int <-> float conversion

	boolean odDump = false; // enable for od-style output to syserr
	boolean bigEndian = true;

	Iterator<File> fileIterator;

	public FfmpegReader(AudioConfig config, AudioOutput output) {
		this.config = config;
		this.output = output;
	}

	public FfmpegReader() {
	}

	public static void testMp4() throws InterruptedException {
		FfmpegReader reader = new FfmpegReader();
		reader.config = new AudioConfig();
		reader.config.numSamplesPerArray = 256;

		reader.initMpeg4("C:/Users/tcook/root/sensorHub/audio/wavFiles/GuitarPnP.m4a");
		reader.readMpeg4();
	}

	public void initMpeg4(String url) {
		formatCtx = avformat_alloc_context();
		avformat_open_input(formatCtx, url, null, null);
		av_dump_format(formatCtx, 0, url, 0);
		AVCodecParameters params = formatCtx.streams(0).codecpar();
//			params.
		int id = params.codec_id();
//		decoder = avcodec_find_decoder(id);
		decoder = avcodec_find_decoder_by_name("aac");
		BytePointer bp = decoder.name();
		System.err.println(bp.getString());

		decodeCtx = avcodec_alloc_context3(decoder);

		// Have to manually set these or avcodec_open2() fails
		decodeCtx.sample_rate(48000);
		decodeCtx.channels(2);

		int status = avcodec_open2(decodeCtx, decoder, (PointerPointer<?>) null);
		if (status < 0) {
			throw new IllegalStateException("Error initializing decoder " + DECODER);
		}

		printStreamInformation(decoder, decodeCtx, 0);

		avFrame = av_frame_alloc();
		avPacket = av_packet_alloc();
		av_init_packet(avPacket);

		// Metadata
		BytePointer sampleFormatName = av_get_sample_fmt_name(decodeCtx.sample_fmt());
		bytesPerSample = av_get_bytes_per_sample(decodeCtx.sample_fmt());
		bitsPerSample = av_get_bits_per_sample(decodeCtx.sample_fmt()); // returns 0 for wav CODEC
		bitsPerSample = bytesPerSample * 8;
//			decoder = avcodec_find_decoder_by_name(DECODER); //
	}

	public void readMpeg4() throws InterruptedException {
		boolean eof = false;
		AVInputFormat avFormat;
		int frameCnt = 0;
		int arrayCnt = 0;
		int totCnt = 46;

		while (!eof) {
			int res = av_read_frame(formatCtx, avPacket);
			if (res == AVERROR_EOF) {
				System.err.println("EOF reached");
				break;
			}
			if (res == AVERROR_EAGAIN()) {
				// System.err.println("EOF reached");
				// don't think we care about this one
				break;
			}
			if (res != 0) {
				System.err.println("Read Frame Error result = " + res);
				break;
			}
			if (avPacket.stream_index() != 0) { // TODO, support multiple streams?
				// Free the buffers used by the frame and reset all fields.
				av_packet_unref(avPacket);
				continue;
			}
			// Send packet to decoder
			// avPacket.size(512); //

			res = avcodec_send_packet(decodeCtx, avPacket);
			if (res != 0) {
				System.err.println("Send packet Error result = " + res);
				av_packet_unref(avPacket);
				break;
			}
			// The packet was sent successfully. We don't need it anymore.
			// => Free the buffers used by the frame and reset all fields.
			// ?? av_packet_unref(avPacket);

			// receive and handle frame(s)
			while (res == 0) {
				// nativeData.position(0);
				// nativeData.limit(0);
				// nativeData.put(frameData);
				// avPacket.data(nativeData);
				// avPacket.size(frameData.length); // this does not affect underlying buffer as
				// retrieved by avFrame.getExtendedData
				res = avcodec_receive_frame(decodeCtx, avFrame);
				if (res != 0) {
					if(res != 11)
						System.err.println("Receive Frame Error result = " + res);
					continue;
				}
				// handle frame
				int sampleFmt = av_sample_fmt_is_planar(decodeCtx.sample_fmt());
				// This means that the data of each channel is in its own buffer.
				// => frame->extended_data[i] contains data for the i-th channel.
//				System.err.println("Frame : " + frameCnt);
				int numSamples = avFrame.nb_samples();
//				System.err.println("FrameSamples: " + numSamples);
				AudioRecord rec = new AudioRecord();
				// for(int s = 0; s < avFrame.nb_samples(); ++s) {
				for (int c = 0; c < decodeCtx.channels(); ++c) {
					// float sample = getSample(decodeCtx, avFrame.extended_data(c), s);
					byte[] tbuff = new byte[numSamples * 2];
					BytePointer buff = avFrame.extended_data(c);
					// System.err.println("Capacity: " + buff.sizeof());
					buff.get(tbuff);
					rec.sampleData = new double[config.numSamplesPerArray];

					int sampleCnt = 0;
					for (int i = 0; i < numSamples * 2; i += 2) {
						double lval = normalizeSample(tbuff[i], tbuff[i + 1]);
						// if(tbuff[i] != 0 || tbuff[i+1] != 0)
						// System.err.printf(" [%x] [%d] %x %x %f: ", totCnt, sampleCnt, tbuff[i],
						// tbuff[i + 1], lval);
						if (odDump) {
							if (totCnt % 16 == 0)
								System.err.printf("\n[%04x]: ", totCnt);
							if (bigEndian)
								System.err.printf("%02x%02x ", tbuff[i], tbuff[i + 1]);
							else
								System.err.printf("%02x%02x ", tbuff[i + 1], tbuff[i]);
						}
						rec.sampleData[sampleCnt++] = lval;
						totCnt += 2;
						if (sampleCnt == config.numSamplesPerArray) {
							rec.sampleIndex = (sampleCnt * arrayCnt);
							rec.samplingRate = decodeCtx.sample_rate();
//							output.publishRecord(rec);
							// System.err.println("\n**sampleIdx, arrCnt " + sampleCnt + "," + arrayCnt);
							rec.sampleData = new double[config.numSamplesPerArray];
							sampleCnt = 0;
							arrayCnt++;
//							Thread.sleep(30L);
						}
					}
				}
				// }
				System.err.println();

				av_frame_unref(avFrame);
			}
			frameCnt++;
		}
	}

	boolean firstFile = true;
	double startTime;  //  time of first file 
	public void init(File inputFile) throws IOException {
		// init FFMPEG objects
//		av_log_set_level(AV_LOG_TRACE);
		
		//  pull time from filename
		double baseTime = WavDirectoryIterator.filenameTimeParser(inputFile.getName().toString(), config.baseTimePattern);
		output.baseTime = baseTime;
		System.err.println("FFMpegReader.init().  FileTime is: " + Instant.ofEpochSecond((long)baseTime));
		if(firstFile) {
			startTime = config.startTimeOverride != null ? getTime(config.startTimeOverride) : baseTime;
			firstFile = false;
		} else {
			if(config.startTimeOverride != null) {
				// TODO
			}
		}

//		try {
//			String ffprobe = Loader.load(org.bytedeco.ffmpeg.ffprobe.class);
//			ProcessBuilder pb = new ProcessBuilder(ffprobe, inputFile.getCanonicalPath());
////			pb.inheritIO().start().waitFor();
//			//pb = pb.inheritIO();
//			Process p = pb.start();
//			p.waitFor();
//			BufferedReader reader = 
//	                new BufferedReader(new InputStreamReader(p.getErrorStream()));
//			StringBuilder builder = new StringBuilder();
//			String line = null;
//			while ( (line = reader.readLine()) != null) {
//			   builder.append(line);
//			   builder.append(System.getProperty("line.separator"));
//			}
//			String result = builder.toString();
//			System.err.println(result);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		//  Get Metadata
		AudioMetadata metadata = null;
		try {
			System.err.println(inputFile.getCanonicalPath());
			metadata = FfmpegUtil.ffProbe(Paths.get(inputFile.getCanonicalPath()));
			System.err.println(metadata);
		} catch (IOException | InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException(e);
		} 
		
		// AVFormatContext
		formatCtx = avformat_alloc_context();
//		avformat_open_input(formatCtx, inputFile.getCanonicalPath(), null, null);
		avformat_open_input(formatCtx, inputFile.getCanonicalPath(), null, null);

		int ok = avformat.avformat_find_stream_info(formatCtx, (AVDictionary)null);
		if (ok < 0) {
		   System.err.println("Could not find stream information for stream ");
		    return;
		}
		avformat.av_dump_format(formatCtx, 0, inputFile.getCanonicalPath(), 0);

		// init decoder context
//		decoder = avcodec_find_decoder_by_name(DECODER); //
		decoder = avcodec_find_decoder_by_name(metadata.codec); //
		// System.err.println(decoder);
		decodeCtx = avcodec_alloc_context3(decoder);
		// Have to manually set these or avcodec_open2() fails
//		decodeCtx.sample_rate(8000);
//		decodeCtx.sample_rate(16000);
		decodeCtx.sample_rate(metadata.sampleRate);
		decodeCtx.channels(metadata.numChannels);
//		decodeCtx.channels(2);

		// init decoder
		int status = avcodec_open2(decodeCtx, decoder, (PointerPointer<?>) null);
		if (status < 0) {
			throw new IllegalStateException("Error initializing decoder " + DECODER);
		}

//		printStreamInformation(decoder, decodeCtx, 0);

		// init frame and packet
		avFrame = av_frame_alloc();
		avPacket = av_packet_alloc();
		av_init_packet(avPacket);

		// Metadata
		BytePointer sampleFormatName = av_get_sample_fmt_name(decodeCtx.sample_fmt());
		bytesPerSample = av_get_bytes_per_sample(decodeCtx.sample_fmt());
		// bitsPerSample = av_get_bits_per_sample(decodeCtx.sample_fmt()); // returns 0
		// for wav CODEC
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
		System.err.println("End init()");
	}
	
	private double getTime(String s) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm:ss").withZone(ZoneId.of("GMT"));
		ZonedDateTime zdt = ZonedDateTime.parse(s, formatter);
		long sec = zdt.toInstant().getEpochSecond();
		return (double)sec;
	}

	private Iterator<File> createIterator() {
		Path p = Paths.get(config.wavDir);
		assert Files.exists(p);
		Iterator<File> it = WavDirectoryIterator.getFileIterator(p, "wav");
		return it;
	}

	@Override
	public void run() {
		Iterator<File> it = createIterator();
		while (it.hasNext()) {
			try {
				init(it.next());
				readWav();
				Thread.sleep(500L);
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	//  TODO - break into separate reader>
	public void readWav() throws InterruptedException {
		boolean eof = false;
		AVInputFormat avFormat;
		int frameCnt = 0;
		int arrayCnt = 0;
		int totCnt = 46; // hardwired for wav- need to compute this dynamically.

		while (!eof) {
			int res = av_read_frame(formatCtx, avPacket);
			if (res == AVERROR_EOF) {
				System.err.println("EOF reached");
				break;
			}
			if (res == AVERROR_EAGAIN()) {
				// System.err.println("EOF reached");
				// don't think we care about this one
				break;
			}
			if (res != 0) {
				System.err.println("Read Frame Error result = " + res);
				break;
			}
			if (avPacket.stream_index() != 0) { // TODO, support multiple streams?
				// Free the buffers used by the frame and reset all fields.
				av_packet_unref(avPacket);
				continue;
			}
			// Send packet to decoder
			// avPacket.size(512); //

			res = avcodec_send_packet(decodeCtx, avPacket);
			if (res != 0) {
				System.err.println("Send packet Error result = " + res);
				av_packet_unref(avPacket);
				break;
			}
			// The packet was sent successfully. We don't need it anymore.
			// => Free the buffers used by the frame and reset all fields.
			// ?? av_packet_unref(avPacket);

			// receive and handle frame(s)
			while (res == 0) {
				// nativeData.position(0);
				// nativeData.limit(0);
				// nativeData.put(frameData);
				// avPacket.data(nativeData);
				// avPacket.size(frameData.length); // this does not affect underlying buffer as
				// retrieved by avFrame.getExtendedData
				res = avcodec_receive_frame(decodeCtx, avFrame);
				if (res != 0) {
					if(res != -11)
						System.err.println("Receive Frame Error result = " + res);
					continue;
				}
				// handle frame
				int sampleFmt = av_sample_fmt_is_planar(decodeCtx.sample_fmt());
				// This means that the data of each channel is in its own buffer.
				// => frame->extended_data[i] contains data for the i-th channel.
//				System.err.println("Frame : " + frameCnt);
				int numSamples = avFrame.nb_samples();
//				System.err.println("FrameSamples: " + numSamples);
				AudioRecord rec = new AudioRecord();
				// for(int s = 0; s < avFrame.nb_samples(); ++s) {
				for (int c = 0; c < decodeCtx.channels(); ++c) {
					// float sample = getSample(decodeCtx, avFrame.extended_data(c), s);
					byte[] tbuff = new byte[numSamples * 2];
					BytePointer buff = avFrame.extended_data(c);
					// System.err.println("Capacity: " + buff.sizeof());
					buff.get(tbuff);
					rec.sampleData = new double[config.numSamplesPerArray];

					int sampleCnt = 0;
					for (int i = 0; i < numSamples * 2; i += 2) {
						double lval = normalizeSample(tbuff[i], tbuff[i + 1]);
						// if(tbuff[i] != 0 || tbuff[i+1] != 0)
						// System.err.printf(" [%x] [%d] %x %x %f: ", totCnt, sampleCnt, tbuff[i],
						// tbuff[i + 1], lval);
						if (odDump) {
							if (totCnt % 16 == 0)
								System.err.printf("\n[%04x]: ", totCnt);
							System.err.printf("%02x%02x ", tbuff[i + 1], tbuff[i]);
						}
						rec.sampleData[sampleCnt++] = lval;
						totCnt += 2;
						if (sampleCnt == config.numSamplesPerArray) {
							rec.sampleIndex = (sampleCnt * arrayCnt);
							rec.samplingRate = decodeCtx.sample_rate();
							output.publishRecord(rec);
							// System.err.println("\n**sampleIdx, arrCnt " + sampleCnt + "," + arrayCnt);
							rec.sampleData = new double[config.numSamplesPerArray];
							sampleCnt = 0;
							arrayCnt++;
							Thread.sleep(30L);
						}
					}
				}
//				System.err.println();

				av_frame_unref(avFrame);
			}
			frameCnt++;
		}
	}

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

	// TODO: generalize for different audio formats/codecs
	float getSample(AVCodecContext codecCtx, byte sample1, byte sample2) {
		long val = 0;
		float ret = 0;
		int sampleSize = av_get_bytes_per_sample(codecCtx.sample_fmt());

		switch (sampleSize) {
		case 1:
			// 8bit samples are always unsigned
			// val = REINTERPRET_CAST(uint8_t*, buffer)[sampleIndex];
			// make signed
			// val -= 127;
			break;

		case 2:
			// val = REINTERPRET_CAST(int16_t*, buffer)[sampleIndex];
			// return ByteBuffer.wrap(buff.).getFloat();
			// buff.byte
			// byte [] tbuff = new byte[2];
			// buff.get(tbuff);
			byte[] tbuff4 = { 0, 0, sample2, sample1 };
			float f = ByteBuffer.wrap(tbuff4).getFloat();
			return f;
		case 4:
			// val = REINTERPRET_CAST(int32_t*, buffer)[sampleIndex];
			break;

		case 8:
			// val = REINTERPRET_CAST(int64_t*, buffer)[sampleIndex];
			break;

		default:
			System.err.printf("Invalid sample size %d.\n", sampleSize);
			return 0;
		}

		return 0.0F;
	}

	/**
	 * v = -8, fffffff8 v &= ff (248, f8000000) val = 248
	 * 
	 * v = b1 (-1, FFFFFFFF) tmp = v << 8 (-256, FFFFFF00) val += tmp (-8)
	 */
	public double normalizeSample(byte b0, byte b1) {
		// Assumes twosComp, so will need to generalize this
		long res = (b0 & 0xff) + (b1 << 8);
		return (double) res / floatScale;
	}

	//    GUI_Decode_2019-03-27 141801.wav
	// D_Bouz_Pickin_2019-03-27 141801.wav
	@Deprecated // use WavDirectoryUtil/Iterator
	public static double filenameTimeParser(String fname, String timePattern) {
		String dateStr = fname.substring(11, fname.length() - 4);
		System.err.println(dateStr);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern).withZone(ZoneId.of("GMT"));
		ZonedDateTime zdt = ZonedDateTime.parse(dateStr, formatter);
		long sec = zdt.toInstant().getEpochSecond();
		long nano = zdt.toInstant().getNano();
		double time = sec + nano;
		
		return time;
	}
}