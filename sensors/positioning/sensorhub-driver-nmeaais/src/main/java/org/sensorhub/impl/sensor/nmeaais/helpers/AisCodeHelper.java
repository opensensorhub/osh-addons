/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais.helpers;

import net.opengis.swe.v20.Quantity;

import java.util.HashMap;
import java.util.Map;

/**
 * AIS integer-code lookup tables per ITU-R M.1371-5.
 * Each nested enum maps raw integer codes to human-readable descriptions.
 */
public class AisCodeHelper {
    /**
     * AIS Message Descriptions found at <a href="https://www.navcen.uscg.gov/ais-messages">navcen.uscg.gov</a>
     */
    public enum MessageType {
        TYPE_1(1,   "Scheduled position report; Class A shipborne mobile equipment"),
        TYPE_2(2,   "Assigned scheduled position report; Class A shipborne mobile equipment"),
        TYPE_3(3,   "Special position report, response to interrogation; Class A shipborne mobile equipment"),
        TYPE_4(4,   "Position, UTC, date and current slot number of base station"),
        TYPE_5(5,   "Scheduled static and voyage related vessel data report, Class A shipborne mobile equipment"),
        TYPE_6(6,   "Binary data for addressed communication"),
        TYPE_7(7,   "Acknowledgement of received addressed binary data"),
        TYPE_8(8,   "Binary data for broadcast communication"),
        TYPE_9(9,   "Position report for airborne stations involved in SAR operations only"),
        TYPE_10(10, "Request UTC and date"),
        TYPE_11(11, "Current UTC and date if available"),
        TYPE_12(12, "Safety related data for addressed communication"),
        TYPE_13(13, "Acknowledgement of received addressed safety related message"),
        TYPE_14(14, "Safety related data for broadcast communication"),
        TYPE_15(15, "Request for a specific message type can result in multiple responses from one or several stations"),
        TYPE_16(16, "Assignment of a specific report behaviour by competent authority using a Base station"),
        TYPE_17(17, "DGNSS corrections provided by a base station"),
        TYPE_18(18, "Standard position report for Class B shipborne mobile equipment to be used instead of Messages 1, 2, 3"),
        TYPE_19(19, "No longer required. Extended position report for Class B shipborne mobile equipment; contains additional static information"),
        TYPE_20(20, "Reserve slots for Base station(s)"),
        TYPE_21(21, "Position and status report for aids-to-navigation"),
        TYPE_22(22, "Management of channels and transceiver modes by a Base station"),
        TYPE_23(23, "Assignment of a specific report behaviour by competent authority using a Base station to a specific group of mobiles"),
        TYPE_24(24, "Additional data assigned to an MMSI Part A: Name Part B: Static Data"),
        TYPE_25(25, "Short unscheduled binary data transmission Broadcast or addressed"),
        TYPE_26(26, "Scheduled binary data transmission Broadcast or addressed"),
        TYPE_27(27, "Class A and Class B \"SO\" shipborne mobile equipment outside base station coverage");

        private final int code;
        private final String description;
        private static final Map<Integer, MessageType> LOOKUP = new HashMap<>();

        static {
            for (MessageType m : values()) LOOKUP.put(m.code, m);
        }

        MessageType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            MessageType m = LOOKUP.get(code);
            return m != null ? m.description : "Unknown message type " + code;
        }

        public static MessageType fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    public enum NavigationalStatus {
        UNDER_WAY_ENGINE(0, "Under way using engine"),
        AT_ANCHOR(1, "At anchor"),
        NOT_UNDER_COMMAND(2, "Not under command"),
        RESTRICTED_MANEUVERABILITY(3, "Restricted maneuverability"),
        CONSTRAINED_DRAUGHT(4, "Constrained by her draught"),
        MOORED(5, "Moored"),
        AGROUND(6, "Aground"),
        FISHING(7, "Engaged in fishing"),
        UNDER_WAY_SAILING(8, "Under way sailing"),
        RESERVED_9(9, "Reserved (DG/HS/MP / HSC)"),
        RESERVED_10(10, "Reserved (DG/HS/MP / WIG)"),
        RESERVED_11(11, "Power-driven vessel towing astern (regional use)"),
        RESERVED_12(12, "Power-driven vessel pushing ahead or towing alongside (regional use)"),
        RESERVED_13(13, "Reserved for future use"),
        AIS_SART_MOB(14, "AIS-SART / MOB-AIS / EPIRB-AIS (active)"),
        UNDEFINED(15, "Undefined / default");

        private final int code;
        private final String description;
        private static final Map<Integer, NavigationalStatus> LOOKUP = new HashMap<>();

        static {
            for (NavigationalStatus s : values()) LOOKUP.put(s.code, s);
        }

        NavigationalStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            NavigationalStatus s = LOOKUP.get(code);
            return s != null ? s.description : "Unknown navigational status " + code;
        }

        public static NavigationalStatus fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS Ship Type codes per the USCG AIS Static Data Encoding Guide (26-05-01)
     * and ITU-R M.1371-5 (Message 5 Type of Ship).
     *
     * Vessels operating in U.S. waters should encode their ship type as denoted in
     * the USCG AIS Encoding Guide:
     *   <a href="https://www.navcen.uscg.gov/sites/default/files/pdf/AIS/AISGuide.pdf">AISGuide.pdg</a>
     *
     * Additional field definitions:
     *   <a href="https://www.navcen.uscg.gov/ais-class-a-static-voyage-message-5">www.navcen.uscg.gov</a>
     *
     * NOTE: Embolden / new code numbers from the current USCG guide may appear as
     * 'Reserved for future use' or not at all on legacy AIS systems.
     */
    public enum ShipType {
        // -------------------------------------------------------------------------
        // Code 0
        // -------------------------------------------------------------------------
        NOT_AVAILABLE(0, "Not available or no ship"),

        // -------------------------------------------------------------------------
        // Codes 1–19  (specific vessel types; new in current USCG guide)
        // -------------------------------------------------------------------------
        RESEARCH_VESSEL(1,  "Science / Research vessel"),
        TRAINING(2,         "Training vessel"),
        GOVERNMENT(3,       "Ship owned or operated by a government"),
        ICEBREAKER(4,       "Ice breaker"),
        BUOY_TENDER(5,      "Buoy (Aids to Navigation) tender"),
        CABLE_LAYER(6,      "Cable layer"),
        PIPE_LAYER(7,       "Pipe layer"),
        // 8 = Reserved for future use
        SPECIAL_PURPOSE(9,  "Special purpose ship, no additional information"),
        // 10 = Reserved for future use
        FPSO(11,            "FPSO (Floating, Production, Storage, Offloading) vessel"),
        FISH_FACTORY(12,    "Fish factory ship"),
        FISH_FARM_SUPPORT(13, "Fish farm support vessel"),
        OFFSHORE_SUPPORT(14,  "Offshore support vessel"),
        // 15, 16 = Reserved for future use
        CONSTRUCTION(17,    "Construction vessel"),
        CREW_BOAT(18,       "Crew boat"),
        SUPPORT_VESSEL(19,  "Support vessel, no additional information"),

        // -------------------------------------------------------------------------
        // Codes 20–29  Wing-in-Ground (WIG)
        // -------------------------------------------------------------------------
        WING_IN_GROUND(20,  "Wing in Ground (WIG), all ships of this type"),
        WIG_HAZARD_X(21,    "Wing in Ground (WIG), carrying DG/MHB/HS/MP, IMO hazard category X (formerly A)"),
        WIG_HAZARD_Y(22,    "Wing in Ground (WIG), carrying DG/MHB/HS/MP, IMO hazard category Y (formerly B)"),
        WIG_HAZARD_Z(23,    "Wing in Ground (WIG), carrying DG/MHB/HS/MP, IMO hazard category Z (formerly C)"),
        WIG_HAZARD_OS(24,   "Wing in Ground (WIG), carrying DG/MHB/HS/MP, IMO hazard category OS (formerly D)"),
        // 25–28 = Reserved for future use
        WIG_NO_INFO(29,     "Wing in Ground (WIG), no additional information"),

        // -------------------------------------------------------------------------
        // Codes 30–39  Special vessel operations / types
        // -------------------------------------------------------------------------
        FISHING(30,         "Fishing vessel"),
        TOWING(31,          "Towing vessel"),
        TOWING_LARGE(32,    "Towing vessel; length of tow exceeds 200 m or breadth exceeds 25 m"),
        DREDGING(33,        "Dredger"),
        DIVING(34,          "Diving vessel"),
        MILITARY(35,        "Warship or naval auxiliary"),
        SAILING(36,         "Sailing vessel"),
        PLEASURE_CRAFT(37,  "Pleasure motor craft"),
        TRAWLER(38,         "Trawler"),
        PATROL_VESSEL(39,   "Patrol vessel"),

        // -------------------------------------------------------------------------
        // Codes 40–49  High Speed Craft (HSC)
        // -------------------------------------------------------------------------
        HIGH_SPEED_CRAFT(40,    "High Speed Craft (HSC), all ships of this type"),
        HSC_HAZARD_X(41,        "High Speed Craft (HSC), carrying DG/MHB/HS/MP, IMO hazard category X (formerly A)"),
        HSC_HAZARD_Y(42,        "High Speed Craft (HSC), carrying DG/MHB/HS/MP, IMO hazard category Y (formerly B)"),
        HSC_HAZARD_Z(43,        "High Speed Craft (HSC), carrying DG/MHB/HS/MP, IMO hazard category Z (formerly C)"),
        HSC_HAZARD_OS(44,       "High Speed Craft (HSC), carrying DG/MHB/HS/MP, IMO hazard category OS (formerly D)"),
        HSC_PASSENGER(45,       "High Speed Craft (HSC), carrying passengers"),
        HSC_RORO(46,            "High Speed Craft (HSC), Ro-Ro ship (vehicle / rail)"),
        // 47–48 = Reserved for future use
        HSC_NO_INFO(49,         "High Speed Craft (HSC), no additional information"),

        // -------------------------------------------------------------------------
        // Codes 50–59  Special craft (ITU / USCG)
        // -------------------------------------------------------------------------
        PILOT_VESSEL(50,        "Pilot vessel"),
        SEARCH_AND_RESCUE(51,   "Search and rescue vessel"),
        TUG(52,                 "Tug"),
        PORT_TENDER(53,         "Port or fish tender"),
        ANTI_POLLUTION(54,      "Anti-pollution or firefighting responder"),
        LAW_ENFORCEMENT(55,     "Law enforcement vessel"),
        SPARE_56(56,            "Spare 1 – for assignments to local vessels"),
        SPARE_57(57,            "Spare 2 – for assignments to local vessels"),
        MEDICAL_TRANSPORT(58,   "Medical transport (1949 Geneva Conventions and Additional Protocols)"),
        NON_COMBATANT(59,       "Ships of States not parties to an armed conflict (per RR 18)"),

        // -------------------------------------------------------------------------
        // Codes 60–69  Passenger ships
        // -------------------------------------------------------------------------
        PASSENGER(60,               "Passenger ship, all ships of this type"),
        PASSENGER_HAZARD_X(61,      "Passenger ship, carrying DG/MHB/HS/MP, IMO hazard category X (formerly A)"),
        PASSENGER_HAZARD_Y(62,      "Passenger ship, carrying DG/MHB/HS/MP, IMO hazard category Y (formerly B)"),
        PASSENGER_HAZARD_Z(63,      "Passenger ship, carrying DG/MHB/HS/MP, IMO hazard category Z (formerly C)"),
        PASSENGER_HAZARD_OS(64,     "Passenger ship, carrying DG/MHB/HS/MP, IMO hazard category OS (formerly D)"),
        PASSENGER_CRUISE(65,        "Passenger (cruise) ship"),
        PASSENGER_FERRY(66,         "Passenger (ferry) ship"),
        PASSENGER_EXCURSION(67,     "Passenger (excursion) ship (harbour cruise, whale watcher, etc.)"),
        // 68 = Reserved for future use
        PASSENGER_NO_INFO(69,       "Passenger ship, no additional information"),

        // -------------------------------------------------------------------------
        // Codes 70–79  Cargo ships
        // -------------------------------------------------------------------------
        CARGO(70,                   "Cargo ship, all ships of this type"),
        CARGO_HAZARD_X(71,          "Cargo ship, carrying DG/MHB/HS/MP, IMO hazard category X (formerly A)"),
        CARGO_HAZARD_Y(72,          "Cargo ship, carrying DG/MHB/HS/MP, IMO hazard category Y (formerly B)"),
        CARGO_HAZARD_Z(73,          "Cargo ship, carrying DG/MHB/HS/MP, IMO hazard category Z (formerly C)"),
        CARGO_HAZARD_OS(74,         "Cargo ship, carrying DG/MHB/HS/MP, IMO hazard category OS (formerly D)"),
        CARGO_BULK(75,              "Cargo ship, bulk carrier"),
        CARGO_CONTAINER(76,         "Cargo ship, container ship"),
        CARGO_RORO(77,              "Cargo ship, roll-on-roll-off carrier"),
        CARGO_LANDING_CRAFT(78,     "Cargo ship, landing craft"),
        CARGO_NO_INFO(79,           "Cargo ship, no additional information"),

        // -------------------------------------------------------------------------
        // Codes 80–89  Tankers
        // -------------------------------------------------------------------------
        TANKER(80,                  "Tanker, all ships of this type"),
        TANKER_HAZARD_X(81,         "Tanker, carrying DG/MHB/HS/MP, IMO hazard category X (formerly A)"),
        TANKER_HAZARD_Y(82,         "Tanker, carrying DG/MHB/HS/MP, IMO hazard category Y (formerly B)"),
        TANKER_HAZARD_Z(83,         "Tanker, carrying DG/MHB/HS/MP, IMO hazard category Z (formerly C)"),
        TANKER_HAZARD_OS(84,        "Tanker, carrying DG/MHB/HS/MP, IMO hazard category OS (formerly D)"),
        TANKER_NON_HAZARDOUS(85,    "Tanker, non-hazardous or non-pollutant carrier"),
        TUG_BARGE_INTEGRATED(86,    "Integrated / articulated tug and tank barge (ABCD values reflect tug and barge dimensions)"),
        // 87–88 = Reserved for future use
        TANKER_NO_INFO(89,          "Tanker, no additional information"),

        // -------------------------------------------------------------------------
        // Codes 90–99  Other types
        // -------------------------------------------------------------------------
        OTHER(90,               "Other type, all ships of this type"),
        OTHER_HAZARD_X(91,      "Other type, carrying DG/MHB/HS/MP, IMO hazard category X (formerly A)"),
        OTHER_HAZARD_Y(92,      "Other type, carrying DG/MHB/HS/MP, IMO hazard category Y (formerly B)"),
        OTHER_HAZARD_Z(93,      "Other type, carrying DG/MHB/HS/MP, IMO hazard category Z (formerly C)"),
        OTHER_HAZARD_OS(94,     "Other type, carrying DG/MHB/HS/MP, IMO hazard category OS (formerly D)"),
        // 95–98 = Reserved for future use
        OTHER_NO_INFO(99,       "Other type, no additional information");

        // -------------------------------------------------------------------------

        private final int code;
        private final String description;
        private static final Map<Integer, ShipType> LOOKUP = new HashMap<>();

        static {
            for (ShipType t : values()) LOOKUP.put(t.code, t);
        }

        ShipType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        /**
         * Returns a human-readable description for the given numeric AIS ship type code.
         * Falls back to range-based "Reserved for future use" labels for codes not
         * mapped to a named constant.
         */
        public static String getDescription(int code) {
            ShipType t = LOOKUP.get(code);
            if (t != null) return t.description;

            // Range-based fallbacks for reserved / unassigned codes
            if (code == 8)                     return "Reserved for future use";
            if (code == 10)                    return "Reserved for future use";
            if (code >= 15 && code <= 16)      return "Reserved for future use";
            if (code >= 25 && code <= 28)      return "Wing in Ground (WIG), reserved for future use";
            if (code >= 47 && code <= 48)      return "High Speed Craft (HSC), reserved for future use";
            if (code == 68)                    return "Passenger ship, reserved for future use";
            if (code >= 87 && code <= 88)      return "Tanker, reserved for future use";
            if (code >= 95 && code <= 98)      return "Other type, reserved for future use";
            if (code >= 100 && code <= 199)    return "Reserved for regional use";
            if (code >= 200 && code <= 255)    return "Reserved for future use";

            return "Unknown ship type " + code;
        }

        /**
         * Returns the {@link ShipType} for the given code, or {@code null} if the code
         * is not mapped to a named constant (e.g. reserved ranges).
         */
        public static ShipType fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    public enum EpfdType {
        UNDEFINED(0, "Undefined"),
        GPS(1, "GPS"),
        GLONASS(2, "GLONASS"),
        COMBINED_GPS_GLONASS(3, "Combined GPS/GLONASS"),
        LORAN_C(4, "Loran-C"),
        CHAYKA(5, "Chayka"),
        INTEGRATED_NAV(6, "Integrated navigation system"),
        SURVEYED(7, "Surveyed"),
        GALILEO(8, "Galileo"),
        INTERNAL_GNSS(15, "Internal GNSS");

        private final int code;
        private final String description;
        private static final Map<Integer, EpfdType> LOOKUP = new HashMap<>();

        static {
            for (EpfdType e : values()) LOOKUP.put(e.code, e);
        }

        EpfdType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static String getDescription(int code) {
            EpfdType e = LOOKUP.get(code);
            return e != null ? e.description : "Unknown EPFD type " + code;
        }

        public static EpfdType fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS Position Accuracy Descriptions found at <a href="https://www.navcen.uscg.gov/ais-messages">navcen.uscg.gov</a>
     */
    public enum PosAcc {
        LOW(0, "Low (> 10 m) or Default"),
        HIGH(1, "High (<= 10 m)");

        private final int code;
        private final String description;
        private static final Map<Integer, PosAcc> LOOKUP = new HashMap<>();

        static {
            for (PosAcc p : values()) LOOKUP.put(p.code, p);
        }

        PosAcc(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static String getDescription(int code) {
            PosAcc p = LOOKUP.get(code);
            return p != null ? p.description : "Unknown Position Accuracy Number: " + code;
        }

        public static PosAcc fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS Off-Position Indicator per <a href="https://www.navcen.uscg.gov/ais-aton-report">navcen.uscg.gov</a>.
     * Valid only when UTC second is 0–59; for floating AtoN only.
     */
    public enum OffPositionIndicator {
        ON_POSITION(0,  "On position"),
        OFF_POSITION(1, "Off position (floating AtoN only)");

        private final int code;
        private final String description;
        private static final Map<Integer, OffPositionIndicator> LOOKUP = new HashMap<>();

        static {
            for (OffPositionIndicator o : values()) LOOKUP.put(o.code, o);
        }

        OffPositionIndicator(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            OffPositionIndicator o = LOOKUP.get(code);
            return o != null ? o.description : "Unknown off-position indicator: " + code;
        }

        public static OffPositionIndicator fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS UTC Second special status codes per ITU-R M.1371-5.
     * Values 0–59 are valid UTC seconds. Values 60–63 are status codes indicating
     * the timestamp is not a real UTC second and the offPositionIndicator is inapplicable.
     */
    public enum UtcSecondStatus {
        NOT_AVAILABLE(60, "Not available (default)"),
        MANUAL_INPUT(61,  "Manual input mode"),
        DEAD_RECKONING(62, "Dead reckoning mode"),
        INOPERATIVE(63,   "Positioning system inoperative");

        private final int code;
        private final String description;
        private static final Map<Integer, UtcSecondStatus> LOOKUP = new HashMap<>();

        static {
            for (UtcSecondStatus s : values()) LOOKUP.put(s.code, s);
        }

        UtcSecondStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            UtcSecondStatus s = LOOKUP.get(code);
            return s != null ? s.description : "UTC second: " + code;
        }

        public static UtcSecondStatus fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS Virtual AtoN flag per <a href="https://www.navcen.uscg.gov/ais-aton-report">navcen.uscg.gov</a>.
     */
    public enum VirtualAtoN {
        REAL(0,    "Default = real AtoN at indicated position"),
        VIRTUAL(1, "Virtual AtoN, does not physically exist");

        private final int code;
        private final String description;
        private static final Map<Integer, VirtualAtoN> LOOKUP = new HashMap<>();

        static {
            for (VirtualAtoN v : values()) LOOKUP.put(v.code, v);
        }

        VirtualAtoN(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            VirtualAtoN v = LOOKUP.get(code);
            return v != null ? v.description : "Unknown virtual AtoN flag: " + code;
        }

        public static VirtualAtoN fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS Assigned Mode flag per <a href="https://www.navcen.uscg.gov/ais-aton-report">navcen.uscg.gov</a>.
     */
    public enum AssignedMode {
        AUTONOMOUS(0, "Station operating in autonomous and continuous mode (default)"),
        ASSIGNED(1,   "Station operating in assigned mode");

        private final int code;
        private final String description;
        private static final Map<Integer, AssignedMode> LOOKUP = new HashMap<>();

        static {
            for (AssignedMode a : values()) LOOKUP.put(a.code, a);
        }

        AssignedMode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            AssignedMode a = LOOKUP.get(code);
            return a != null ? a.description : "Unknown assigned mode flag: " + code;
        }

        public static AssignedMode fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * AIS Aid To Navigation Type Descriptions found at <a href="https://www.navcen.uscg.gov/ais-aton-report">navcen.uscg.gov</a>
     */
    public enum AtoNType {
        DEFAULT(0,                  "Default, Type of AtoN not specified"),
        REFERENCE_POINT(1,          "Reference point"),
        RACON(2,                    "RACON"),
        FIXED_STRUCTURE(3,          "Fixed structures off-shore, such as oil platforms, wind farms"),
        EMERGENCY_WRECK(4,          "Emergency Wreck Marking Buoy"),
        LIGHT_NO_SECTORS(5,         "Light, without sectors"),
        LIGHT_WITH_SECTORS(6,       "Light, with sectors"),
        LEADING_LIGHT_FRONT(7,      "Leading Light Front"),
        LEADING_LIGHT_REAR(8,       "Leading Light Rear"),
        BEACON_CARDINAL_N(9,        "Beacon, Cardinal N"),
        BEACON_CARDINAL_E(10,       "Beacon, Cardinal E"),
        BEACON_CARDINAL_S(11,       "Beacon, Cardinal S"),
        BEACON_CARDINAL_W(12,       "Beacon, Cardinal W"),
        BEACON_PORT(13,             "Beacon, Port hand"),
        BEACON_STARBOARD(14,        "Beacon, Starboard hand"),
        BEACON_PREF_PORT(15,        "Beacon, Preferred Channel port hand"),
        BEACON_PREF_STARBOARD(16,   "Beacon, Preferred Channel starboard hand"),
        BEACON_ISOLATED_DANGER(17,  "Beacon, Isolated danger"),
        BEACON_SAFE_WATER(18,       "Beacon, Safe water"),
        BEACON_SPECIAL(19,          "Beacon, Special mark"),
        CARDINAL_N(20,              "Cardinal Mark N"),
        CARDINAL_E(21,              "Cardinal Mark E"),
        CARDINAL_S(22,              "Cardinal Mark S"),
        CARDINAL_W(23,              "Cardinal Mark W"),
        PORT_HAND(24,               "Port hand Mark"),
        STARBOARD_HAND(25,          "Starboard hand Mark"),
        PREF_CHANNEL_PORT(26,       "Preferred Channel Port hand"),
        PREF_CHANNEL_STARBOARD(27,  "Preferred Channel Starboard hand"),
        ISOLATED_DANGER(28,         "Isolated danger"),
        SAFE_WATER(29,              "Safe Water"),
        SPECIAL_MARK(30,            "Special Mark"),
        LIGHT_VESSEL(31,            "Light Vessel/LANBY/Rigs");

        private final int code;
        private final String description;
        private static final Map<Integer, AtoNType> LOOKUP = new HashMap<>();

        static {
            for (AtoNType a : values()) LOOKUP.put(a.code, a);
        }

        AtoNType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static String getDescription(int code) {
            AtoNType p = LOOKUP.get(code);
            return p != null ? p.description : "Unknown Type of Aid to Navigation code: " + code;
        }

        public static AtoNType fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    public record RotRecord(Double rot, String roti){}

    public static RotRecord getRotData(int code){
        Double rot;
        String roti;
        double sign = Math.signum(code);

        switch (code){
            case -128:
                rot = null;
                roti = "Not Available";
                break;
            case 127:
                rot = 10.00;
                roti = ">10 deg/min; No TI available";
                break;
            case -127:
                rot = -10.00;
                roti = "<-10 deg/min; No TI available";
                break;
            default:
                rot = sign * Math.pow(code/4.733,2);
                roti = "TI available";
        }

        return new RotRecord(rot, roti);
    }

    /**
     * Special Maneuver Indicator per <a href="https://www.navcen.uscg.gov/ais-class-a-reports">navcen.uscg.gov</a>.
     */
    public enum Spi {
        DEFAULT(0, "Data Not Available (Deafult)"),
        NOT_ENGAGED(1, "Not Engaged in Special Maneuver"),
        ENGAGED(2,   "Engaged in Special Maneuver");

        private final int code;
        private final String description;
        private static final Map<Integer, Spi> LOOKUP = new HashMap<>();

        static {
            for (Spi S : values()) LOOKUP.put(S.code, S);
        }

        Spi(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            Spi a = LOOKUP.get(code);
            return a != null ? a.description : "Unknown SPI Value: " + code;
        }

        public static Spi fromCode(int code) {
            return LOOKUP.get(code);
        }
    }
    /**
     *CLASS B UNIT FLAG per <a href="https://www.navcen.uscg.gov/ais-class-b-reports">navcen.uscg.gov</a>.
     */
    public enum ClassBUnitFlag {
        SOTDMA(0, "Class B SOTDMA unit"),
        CS(1, "Class B 'CS' unit");

        private final int code;
        private final String description;
        private static final Map<Integer, ClassBUnitFlag> LOOKUP = new HashMap<>();

        static {
            for (ClassBUnitFlag f : values()) LOOKUP.put(f.code, f);
        }

        ClassBUnitFlag(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            ClassBUnitFlag a = LOOKUP.get(code);
            return a != null ? a.description : "Unknown Class B Unit Flag Value: " + code;
        }

        public static ClassBUnitFlag fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * Class B Display Flag per <a href="https://www.navcen.uscg.gov/ais-class-b-reports">navcen.uscg.gov</a>.
     */
    public enum DisplayFlag {
        NO_DISPLAY(0, "No display available (default)"),
        HAS_DISPLAY(1, "Equipped with display for Message 12 and 14");

        private final int code;
        private final String description;
        private static final Map<Integer, DisplayFlag> LOOKUP = new HashMap<>();

        static {
            for (DisplayFlag f : values()) LOOKUP.put(f.code, f);
        }

        DisplayFlag(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            DisplayFlag f = LOOKUP.get(code);
            return f != null ? f.description : "Unknown Display Flag value: " + code;
        }

        public static DisplayFlag fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * Class B DSC Flag per <a href="https://www.navcen.uscg.gov/ais-class-b-reports">navcen.uscg.gov</a>.
     */
    public enum DscFlag {
        NOT_EQUIPPED(0, "Not equipped with DSC function (default)"),
        EQUIPPED(1,     "Equipped with DSC function");

        private final int code;
        private final String description;
        private static final Map<Integer, DscFlag> LOOKUP = new HashMap<>();

        static {
            for (DscFlag f : values()) LOOKUP.put(f.code, f);
        }

        DscFlag(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            DscFlag f = LOOKUP.get(code);
            return f != null ? f.description : "Unknown DSC Flag value: " + code;
        }

        public static DscFlag fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * Class B Band Flag per <a href="https://www.navcen.uscg.gov/ais-class-b-reports">navcen.uscg.gov</a>.
     */
    public enum BandFlag {
        UPPER_BAND(0,  "Capable of operating over the upper 525 kHz band of the marine band (default)"),
        WHOLE_BAND(1,  "Capable of operating over the whole marine band");

        private final int code;
        private final String description;
        private static final Map<Integer, BandFlag> LOOKUP = new HashMap<>();

        static {
            for (BandFlag f : values()) LOOKUP.put(f.code, f);
        }

        BandFlag(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            BandFlag f = LOOKUP.get(code);
            return f != null ? f.description : "Unknown Band Flag value: " + code;
        }

        public static BandFlag fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * Class B Message 22 Flag per <a href="https://www.navcen.uscg.gov/ais-class-b-reports">navcen.uscg.gov</a>.
     */
    public enum Message22Flag {
        NO_FREQ_MGMT(0, "No frequency management via Message 22 (default)"),
        FREQ_MGMT(1,    "Frequency management via Message 22");

        private final int code;
        private final String description;
        private static final Map<Integer, Message22Flag> LOOKUP = new HashMap<>();

        static {
            for (Message22Flag f : values()) LOOKUP.put(f.code, f);
        }

        Message22Flag(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            Message22Flag f = LOOKUP.get(code);
            return f != null ? f.description : "Unknown Message 22 Flag value: " + code;
        }

        public static Message22Flag fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * Communication State Selector Flag per <a href="https://www.navcen.uscg.gov/ais-class-b-reports">navcen.uscg.gov</a>.
     * Always 1 (ITDMA) for Class B CS units.
     */
    public enum CommStateSelectorFlag {
        SOTDMA(0, "SOTDMA communication state follows (default)"),
        ITDMA(1,  "ITDMA communication state follows (always 1 for Class B CS)");

        private final int code;
        private final String description;
        private static final Map<Integer, CommStateSelectorFlag> LOOKUP = new HashMap<>();

        static {
            for (CommStateSelectorFlag f : values()) LOOKUP.put(f.code, f);
        }

        CommStateSelectorFlag(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() { return code; }
        public String getDescription() { return description; }

        public static String getDescription(int code) {
            CommStateSelectorFlag f = LOOKUP.get(code);
            return f != null ? f.description : "Unknown Comm State Selector Flag value: " + code;
        }

        public static CommStateSelectorFlag fromCode(int code) {
            return LOOKUP.get(code);
        }
    }

    /**
     * Strips AIS '@' padding characters and trims whitespace from a vessel name or
     * destination string. The AIS character set uses '@' as a null/filler character.
     *
     * @param raw the raw string from the AIS library (may be null)
     * @return cleaned string, or empty string if null/blank
     */
    public static String cleanVesselName(String raw) {
        if (raw == null) return "";
        return raw.replace("@", "").trim();
    }

}
