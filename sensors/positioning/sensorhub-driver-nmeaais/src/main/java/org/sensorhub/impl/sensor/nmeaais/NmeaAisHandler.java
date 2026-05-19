package org.sensorhub.impl.sensor.nmeaais;

import org.sensorhub.impl.sensor.nmeaais.ReportTypes.PositionReport;

import java.util.HashMap;
import java.util.Map;

public class NmeaAisHandler {
    String nmeaAisMsg;
    String rawPayload;

    private final NmeaAisDriver nmeaAisDriver;

    /**
     * Buffer for assembling multi-sentence AIS messages.
     * Key: sequential ID (field 3 of the NMEA sentence, "1"–"9").
     */
    private final Map<String, FragmentBuffer> fragmentBuffers = new HashMap<>();

    public NmeaAisHandler(NmeaAisDriver driver) {
        this.nmeaAisDriver = driver;
    }

    public void handleNmeaAisMessage(String sentence) {
        String[] nmea = sentence.split(",");
        int fragmentCount  = Integer.parseInt(nmea[1]);
        int fragmentNumber = Integer.parseInt(nmea[2]);
        String sequentialId = nmea[3];
        String payload = nmea[5];

        // Check Fragment Count to see if there are multiple messages that will need to be combined
        if (fragmentCount == 1) {
            // Single-sentence message — process immediately
            this.nmeaAisMsg = sentence;
            this.rawPayload = payload;
            parsePayload(payload);
        } else {
            // Multi-sentence message — buffer until all fragments arrive
            reassembleAndProcess(sentence, fragmentCount, fragmentNumber, sequentialId, payload);
        }
    }

    /**
     * Buffers an individual fragment. When all fragments for a sequential ID have
     * arrived the payloads are concatenated in order and passed to {@link #parsePayload}.
     * The NMEA envelope from the first fragment is used for output metadata.
     */
    private void reassembleAndProcess(String sentence, int fragmentCount, int fragmentNumber,
                                      String sequentialId, String payload) {
        FragmentBuffer buf = fragmentBuffers.get(sequentialId);

        // If no buffer exists yet, or a stale buffer with a different fragment count is
        // sitting there (e.g. a previous message was never completed), start fresh.
        if (buf == null || buf.fragmentCount != fragmentCount) {
            buf = new FragmentBuffer(fragmentCount, sentence);
            fragmentBuffers.put(sequentialId, buf);
        }

        buf.addFragment(fragmentNumber, payload);

        if (buf.isComplete()) {
            fragmentBuffers.remove(sequentialId);
            this.nmeaAisMsg = buf.firstSentence;
            this.rawPayload = buf.getCombinedPayload();
            parsePayload(this.rawPayload);
        }
    }

    public void parsePayload(String payload) {
        int messageId = extractBits(payload, 0, 6);
        if (messageId == 1 || messageId == 2 || messageId == 3) {
            PositionReport report = parsePositionReport(payload);
            nmeaAisDriver.publishPositionReport(nmeaAisMsg, report);
        }
    }

    public PositionReport parsePositionReport(String payload) {
        PositionReport report = new PositionReport();

        report.messageId   = extractBits(payload, 0, 6);
        report.repeat      = extractBits(payload, 6, 2);
        report.mmsi        = String.format("%09d", extractBits(payload, 8, 30));
        report.navStatus   = extractBits(payload, 38, 4);
        report.rot         = signExtend(extractBits(payload, 42, 8), 8);
        report.sog         = extractBits(payload, 50, 10) / 10.0;
        report.posAccuracy = extractBits(payload, 60, 1);
        report.longitude   = signExtend(extractBits(payload, 61, 28), 28) / 600000.0;
        report.latitude    = signExtend(extractBits(payload, 89, 27), 27) / 600000.0;
        report.cog         = extractBits(payload, 116, 12) / 10.0;
        report.heading     = extractBits(payload, 128, 9);
        report.timeStamp   = extractBits(payload, 137, 6);
        report.smi         = extractBits(payload, 143, 2);
        report.spare       = extractBits(payload, 145, 3);
        report.raim        = extractBits(payload, 148, 1);
        report.commState   = extractBits(payload, 149, 19);
        report.bits        = payload.length() * 6;

        return report;
    }

    /**
     * Extracts {@code numBits} bits starting at {@code startBit} from an AIS
     * ASCII-armored payload string (6 bits per character, MSB first).
     */
    private int extractBits(String payload, int startBit, int numBits) {
        int result = 0;
        for (int i = 0; i < numBits; i++) {
            int bitPos = startBit + i;
            int charIndex = bitPos / 6;
            int bitInChar = 5 - (bitPos % 6); // MSB first within each 6-bit group
            int charVal = payload.charAt(charIndex) - 48;
            if (charVal > 40) charVal -= 8;
            int bit = (charVal >> bitInChar) & 1;
            result = (result << 1) | bit;
        }
        return result;
    }

    /**
     * Sign-extends a value that was extracted as an unsigned int from {@code numBits} bits
     * into a signed Java int using two's-complement interpretation.
     */
    private int signExtend(int value, int numBits) {
        if ((value & (1 << (numBits - 1))) != 0) {
            value -= (1 << numBits);
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Fragment reassembly support
    // -------------------------------------------------------------------------

    private static class FragmentBuffer {
        final int fragmentCount;
        /** NMEA sentence from the first fragment — used for the output's envelope fields. */
        final String firstSentence;
        private final String[] payloads;
        private int receivedCount;

        FragmentBuffer(int fragmentCount, String firstSentence) {
            this.fragmentCount = fragmentCount;
            this.firstSentence = firstSentence;
            this.payloads = new String[fragmentCount];
        }

        void addFragment(int fragmentNumber, String payload) {
            int index = fragmentNumber - 1; // fragment numbers are 1-based
            if (payloads[index] == null) {
                payloads[index] = payload;
                receivedCount++;
            }
        }

        boolean isComplete() {
            return receivedCount == fragmentCount;
        }

        /** Concatenates all fragment payloads in order into a single bit string. */
        String getCombinedPayload() {
            StringBuilder sb = new StringBuilder();
            for (String p : payloads) {
                sb.append(p);
            }
            return sb.toString();
        }
    }
}
