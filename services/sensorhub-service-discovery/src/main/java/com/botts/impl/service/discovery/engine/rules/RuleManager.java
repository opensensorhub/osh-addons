/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.engine.rules;

import com.botts.impl.service.discovery.engine.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Class used to load rules for the rules engine from a variety of files
 *
 * @author Nicolas Garay
 * @since 24 Jan 2022
 */
public class RuleManager {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(RuleManager.class);

    /**
     * Loads rules given a path to a rule file.
     *
     * @param ruleFile The path to the rules file containing rules
     * @param rules    The collection of rules being created and amended by the rule loader
     * @throws IOException if the loader fails to read a file or directory
     */
    public static void loadRules(File ruleFile, Rules rules) throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(ruleFile))) {

            if (ruleFile.getName().endsWith(Constants.CSV_FILE_TYPE)) {

                loadRules(bufferedReader, rules, Constants.CSV_RULE_DELIM);

            } else if (ruleFile.getName().endsWith(Constants.JSON_FILE_TYPE)) {

                loadJsonRules(bufferedReader, rules);

            } else if (ruleFile.getName().endsWith(Constants.TXT_FILE_TYPE)) {

                loadRules(bufferedReader, rules, Constants.TXT_RULE_DELIM);
            }
        }
    }

    /**
     * Loads rules given a path to a rule file.
     *
     * @param ruleReader The instance of a buffered reader used to read the rules
     * @param rules      The collection of rules being created and amended by the rule loader
     * @throws IOException if the loader fails to read a file or directory
     */
    public static void loadRules(BufferedReader ruleReader, String bufferType, Rules rules) throws IOException {

        if (ruleReader != null) {

            if (bufferType.equalsIgnoreCase(Constants.CSV_FILE_TYPE)) {

                loadRules(ruleReader, rules, Constants.CSV_RULE_DELIM);

            } else if (bufferType.equalsIgnoreCase(Constants.JSON_FILE_TYPE)) {

                loadJsonRules(ruleReader, rules);

            } else if (bufferType.equalsIgnoreCase(Constants.TXT_FILE_TYPE)) {

                loadRules(ruleReader, rules, Constants.TXT_RULE_DELIM);
            }

        } else {

            throw new IOException("Reader cannot be undefined");
        }
    }

    /**
     * Loads rules file given the filename/path, the rules to amend, and the delimiter used within
     * a rule to identify terms
     *
     * @param bufferedReader The instance of a buffered reader used to read the rules
     * @param rules          The rules to amend
     * @param delimiter      Delimiter used within a rule to identify terms
     * @throws IOException if the loader fails to read a file or directory
     */
    private static void loadRules(BufferedReader bufferedReader, Rules rules, String delimiter) throws IOException {

        String line;

        while ((line = bufferedReader.readLine()) != null) {

            line = line.trim();

            if (!line.startsWith(Constants.COMMENT_START) && (line.length() > 0)) {

                String[] contents = line.split(delimiter);

                int tokenIdx = Constants.RULE_ID_IDX;

                String ruleId = contents[tokenIdx++];

                DataStreamRule rule = new DataStreamRule(ruleId, Arrays.copyOfRange(contents, tokenIdx, contents.length));

                rules.addRule(rule.getRuleId(), rule);
            }
        }
    }

    /**
     * Loads rules file given the filename/path and the rules to amend
     *
     * @param bufferedReader The instance of a buffered reader used to read the rules
     * @param rules          The rules to amend
     * @throws IOException if the loader fails to read a file or directory
     */
    private static void loadJsonRules(BufferedReader bufferedReader, Rules rules) throws IOException {

        throw new UnsupportedOperationException();
    }

    /**
     * Reads the contents of the given rules file, if a ruleId is given then only return
     * the line from the file describing the particular rule.
     *
     * @param ruleFile The rule file to read
     * @param ruleId   The id of the rule to "filter" and display
     * @return The requested contents
     * @throws IOException if there is an issue reading the requested file
     */
    public static String readRules(File ruleFile, String ruleId) throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(ruleFile))) {

            StringBuilder stringBuilder = new StringBuilder();

            String line;

            while ((line = bufferedReader.readLine()) != null) {

                if (ruleId != null) {

                    if (line.split(" ")[0].equalsIgnoreCase(ruleId)) {

                        stringBuilder.append(line).append("\\n");
                        break;
                    }

                } else {

                    stringBuilder.append(line).append("\\n");
                }
            }

            bufferedReader.close();

            return stringBuilder.toString();
        }
    }

    /**
     * Writes the rules file using the input reader as source of contents to write
     *
     * @param ruleFile    The file to write
     * @param inputReader The source of contents to write to the file
     * @throws IOException if there is an issue writing the file
     */
    public static void saveRules(File ruleFile, BufferedReader inputReader) throws IOException {

        String originalFileName = ruleFile.getAbsolutePath();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        String newFileName = originalFileName + ".bak." + dateTimeFormatter.format(LocalDateTime.now());

        Files.copy(Paths.get(originalFileName), Paths.get(newFileName));

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(originalFileName))) {

            String line;

            while ((line = inputReader.readLine()) != null) {

                bufferedWriter.write(line);

                bufferedWriter.newLine();
            }
        }
    }

    /**
     * Replaces the specific rule with a new updated rule specification
     *
     * @param ruleFile The file to update
     * @param ruleId   The id of the rule to update
     * @param content  The rule spec to use
     * @throws IOException if the file cannot be read or written
     */
    public static void updateRule(File ruleFile, String ruleId, String content) throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(ruleFile))) {

            StringBuilder inputBuffer = new StringBuilder();

            String line;

            while ((line = bufferedReader.readLine()) != null) {

                if (line.split(" ")[0].equalsIgnoreCase(ruleId)) {

                    line = content;
                }

                inputBuffer.append(line);

                inputBuffer.append('\n');
            }

            bufferedReader.close();

            BufferedReader inputBufferReader = new BufferedReader(new StringReader(inputBuffer.toString()));

            saveRules(ruleFile, inputBufferReader);

        }
    }
}
