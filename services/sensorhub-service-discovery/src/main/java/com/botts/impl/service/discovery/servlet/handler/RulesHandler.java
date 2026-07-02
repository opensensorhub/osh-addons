/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.servlet.handler;

import com.botts.impl.service.discovery.DiscoveryServlet;
import com.botts.impl.service.discovery.ResourcePermissions;
import com.botts.impl.service.discovery.engine.RulesEngine;
import com.botts.impl.service.discovery.engine.rules.RuleManager;
import com.botts.impl.service.discovery.engine.rules.Rules;
import com.botts.impl.service.discovery.servlet.context.RequestContext;
import j2html.attributes.Attr;
import j2html.tags.specialized.ButtonTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static j2html.TagCreator.*;

/**
 * Handles requests for rules reporting and editing
 *
 * @author Nick Garay
 * @since May 19, 2022
 */
public class RulesHandler extends BaseHandler {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(RulesHandler.class);

    /**
     * Error message for undefined rule
     */
    private static final String UNDEFINED_RULE_ERROR = "Rule is undefined";

    /**
     * The valid endpoints for the resource
     */
    public static final String[] names = {"rules"};

    /**
     * The set of permissions
     */
    private final ResourcePermissions permission;

    /**
     * Constructor
     *
     * @param permission The permissions to validate against when processing the request
     */
    public RulesHandler(ResourcePermissions permission) {

        this.permission = permission;
    }

    @Override
    public String[] getNames() {
        return names;
    }

    @Override
    public void doGet(RequestContext context) throws IOException, SecurityException {

        context.checkPermission(permission.read);

        String result;

        String rulesFilePath = ((DiscoveryServlet) context.getServlet()).getConfig().rulesFilePath;

        if (context.isEndOfPath()) {

            result = RuleManager.readRules(new File(rulesFilePath), null);

        } else {

            result = RuleManager.readRules(new File(rulesFilePath), context.popNextPathElt());

            if (result.isEmpty()) {

                throw new IOException(UNDEFINED_RULE_ERROR);
            }
        }

        context.setResponseContentType(TEXT_HTML_MIME_TYPE);

        // Writing the message on the web page
        PrintWriter out = context.getWriter();

        // Check to see if user has permission to save and render page with appropriate controls
        try {

            context.checkPermission(permission.update);

            out.println(renderEditor(context, result, true));

        } catch (SecurityException e) {

            out.println(renderEditor(context, result, false));
        }
    }

    @Override
    public void doPost(RequestContext context) throws IOException, SecurityException {

        boolean fileSaved = true;

        context.checkPermission(permission.update);

        String rulesFilePath = ((DiscoveryServlet) context.getServlet()).getConfig().rulesFilePath;

        if (context.isEndOfPath()) {

            RuleManager.saveRules(new File(rulesFilePath), context.getReader());

        } else {

            String ruleId = context.popNextPathElt();

            if(RulesEngine.getInstance().getRules().getRule(ruleId) != null) {

                String content = context.getReader().readLine();

                if (content != null && !content.isEmpty()) {

                    RuleManager.updateRule(new File(rulesFilePath), ruleId, content);

                } else {

                    fileSaved = false;
                }

            } else {

                throw new IOException(UNDEFINED_RULE_ERROR);
            }
        }

        PrintWriter out = context.getWriter();

        if (fileSaved) {

            Rules rules = new Rules();

            RuleManager.loadRules(new File(rulesFilePath), rules);

            RulesEngine.getInstance().setRules(rules);

            out.println("\n" +
                            "   ____                      _____                             _    _       _     \n" +
                            "  / __ \\                    / ____|                           | |  | |     | |    \n" +
                            " | |  | |_ __   ___ _ __   | (___   ___ _ __  ___  ___  _ __  | |__| |_   _| |__  \n" +
                            " | |  | | '_ \\ / _ \\ '_ \\   \\___ \\ / _ \\ '_ \\/ __|/ _ \\| '__| |  __  | | | | '_ \\ \n" +
                            " | |__| | |_) |  __/ | | |  ____) |  __/ | | \\__ \\ (_) | |    | |  | | |_| | |_) |\n" +
                            "  \\____/| .__/ \\___|_| |_| |_____/ \\___|_| |_|___/\\___/|_|    |_|  |_|\\__,_|_.__/ \n" +
                            "        | |\n" +
                            "        |_|\n" +
                            "\n" +
                            "                                    Rules updated!\n");

        } else {

            out.println("Rules failed to update!");
        }
    }

    @Override
    public void doPut(RequestContext context) throws IOException, SecurityException {

        throw new IOException(UNSUPPORTED_OP_ERROR);
    }

    @Override
    public void doDelete(RequestContext context) throws IOException, SecurityException {

        throw new IOException(UNSUPPORTED_OP_ERROR);
    }

    /**
     * Render the editor with its contents
     *
     * @param editorContents The contents to place in the editor
     * @return HTML Page
     */
    private String renderEditor(RequestContext context, String editorContents, boolean canModify) {

        ButtonTag saveButton = button("Save")
                .attr(Attr.ID, "saveButton").attr(Attr.TYPE, "button");

        if (!canModify) {

            saveButton.attr(Attr.DISABLED);
        }

        return document(
                html(
                        head(
                                title("Open Sensor Hub Discovery Rules"),
                                script().withSrc("https://cdnjs.cloudflare.com/ajax/libs/ace/1.5.0/ace.js"),
                                style(
                                        "#editor { \n" +
                                                "   position: absolute;\n" +
                                                "   top: 0;\n" +
                                                "   right: 0;\n" +
                                                "   bottom: 0;\n" +
                                                "   left: 0;\n" +
                                                "}\n" +
                                                "#toolbar { \n" +
                                                "   position: absolute;\n" +
                                                "   top: 5%;\n" +
                                                "   right: 5%;\n" +
                                                "   z-index:100;\n" +
                                                "}\n" +
                                                "#saveButton { \n" +
                                                "   position: relative;\n" +
                                                "   margin: 10px;\n" +
                                                "}\n" +
                                                "saveButton:disabled,\n" +
                                                "saveButton[disabled] {\n" +
                                                "   border: 1px solid #999999;\n" +
                                                "   background-color: darkgrey;\n" +
                                                "   color: #fff;\n" +
                                                "   padding: 10px;\n" +
                                                "   cursor: not-allowed;\n" +
                                                "}\n"
                                )
                        ),
                        body(
                                div().attr(Attr.ID, "editor"),
                                div(
                                        saveButton
                                ).attr(Attr.ID, "toolbar"),
                                script("\n" +
                                        "let editor = ace.edit(\"editor\");\n" +
                                        "editor.setTheme(\"ace/theme/monokai\");\n" +
                                        "editor.session.setMode(\"ace/mode/text\");\n" +
                                        "editor.session.setValue(\"" +
                                        editorContents.replace("\n", "\\n") + "\")\n" +
                                        "\n" +
                                        "let submit = async function() {\n" +
                                        "   let rules = editor.session.getValue();\n" +
                                        "   try {\n" +
                                        "       const response = await fetch(\"" + context.getFullPath() + "\", {\n" +
                                        "           method: 'post',\n" +
                                        "           headers: {\n" +
                                        "               'Accept': '" + PLAIN_TEXT_MIME_TYPE + "',\n" +
                                        "               'Content-Type': '" + PLAIN_TEXT_MIME_TYPE + "'\n" +
                                        "           },\n" +
                                        "           body: rules\n" +
                                        "       });\n" +
                                        "       button.disabled = true;\n" +
                                        "       editor.session.setValue(await response.text());\n" +
                                        "   } catch(err) {\n" +
                                        "       console.error(`Error: ${err}`);\n" +
                                        "   }\n" +
                                        "}\n" +
                                        "let button = document.getElementById(\"saveButton\");\n" +
                                        "button.onclick = submit;\n"
                                )
                        )
                )
        );
    }
}
