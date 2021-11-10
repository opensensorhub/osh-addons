package org.sensorhub.impl.sensor.audio;

//  TODO- revisit at some point- pull common code from FfmpegWavReader into base class
public class FfmpegMp4Reader {
/**
 * 	public static void testMp4() throws InterruptedException {
		FfmpegWavReader reader = new FfmpegWavReader();
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
*/
}
