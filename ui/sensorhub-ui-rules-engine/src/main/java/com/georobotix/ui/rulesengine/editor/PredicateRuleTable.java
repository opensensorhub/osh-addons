/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2026 GeoRobotiX Innovative Research. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.ui.rulesengine.editor;

import com.georobotix.ai.impl.rulesengine.Constants;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.ui.HorizontalLayout;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * A class representing a table for managing predicate rules in a user interface.
 * <p>
 * The PredicateRuleTable class extends from {@code VerticalLayout} and organizes the rule rows
 * in a vertical arrangement. Each rule row allows users to define or edit logical predicate rules
 * through a dynamic UI component.
 * <p>
 * The class provides functionalities for adding, editing, removing, loading, and saving rules.
 * Rule rows are defined as {@code RuleRow} objects, which are dynamically managed within the
 * `rulesTable`. Visual elements such as buttons for interactions and text fields for input
 * are configured in the constructor, and event listeners are used to handle user actions.
 */
public class PredicateRuleTable extends VerticalLayout {

    /**
     * Represents a container layout for displaying and managing a table of predicate rules.
     * The `rulesTable` is responsible for organizing rule rows, where each row allows
     * users to define or modify logical predicates in a UI-driven format.
     * <p>
     * Designed to dynamically handle a collection of rule rows, each of which represents
     * an individual rule consisting of a field selector, URI inputs, and logical operators.
     * <p>
     * This table is an instance of a `VerticalLayout`, meaning the rule rows are arranged
     * vertically. Functionalities such as adding or removing rule rows are managed
     * programmatically by the containing class.
     * <p>
     * The primary use case is to enable users to define and edit predicate rules
     * in a user interface environment.
     * <p>
     * Being `protected final`, the layout cannot be modified outside its containing class,
     * and its reference cannot be reassigned, ensuring a stable design for managing rules.
     */
    protected final VerticalLayout rulesTable = new VerticalLayout();

    /**
     * A list of string definitions used to initialize and manage rule-related
     * components in the rules table.
     * <p>
     * This field stores the definitions provided at the instantiation of the
     * containing class and may be used to configure rule parsing, display
     * behavior, or initialization of other associated components.
     */
    private final List<String> definitions;

    /**
     * Constructs an instance of the PredicateRuleTable class.
     * <p>
     * This constructor initializes the rule table UI, including components for
     * adding, loading, saving, and editing rules. It styles elements, sets up
     * scrolling behavior, and initializes the rules table with predefined
     * definitions. Additionally, it binds event listeners for user interactions
     * such as adding and managing rule entries.
     *
     * @param definitions a list of string definitions used to initialize
     *                    rule-related components. These definitions are passed
     *                    to other methods or components for rule parsing
     *                    and display configuration.
     */
    public PredicateRuleTable(final List<String> definitions) {

        this.definitions = definitions;

        setSpacing(true);
        setMargin(true);
        setWidth("100%");

        Page.getCurrent().getStyles().add(
                ".predicate-scroll-pane > .v-panel-contentLayout { " +
                        "  overflow-x: auto; " +
                        "  overflow-y: auto; " +
                        "} "
        );

        HorizontalLayout fileManagementLayout = new HorizontalLayout();
        fileManagementLayout.setSpacing(true);

        TextField ruleFileName = new TextField();
        ruleFileName.setWidth("200px");
        ruleFileName.setPlaceholder("/rule/file/name.txt");

        Button loadRulesBtn = new Button();
        loadRulesBtn.setDescription("Load Rules");
        loadRulesBtn.setIcon(FontAwesome.FILE);
        loadRulesBtn.addClickListener(e -> {
            String filename = ruleFileName.getValue();
            filename = filename.trim();
            if (filename.isEmpty()) return;
            File ruleFile = new File(filename);
            if (ruleFile.exists()) {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(ruleFile))) {

                    rulesTable.removeAllComponents();

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {

                        line = line.trim();

                        if (!line.startsWith(Constants.COMMENT_START) && (line.length() > 0)) {

                            String[] contents = line.split(" ");

                            int tokenIdx = Constants.RULE_ID_IDX;

                            String ruleId = contents[tokenIdx++];

                            contents = Arrays.copyOfRange(contents, tokenIdx, contents.length);

                            addRuleRow(ruleId, contents);
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        Button saveRulesBtn = new Button();
        saveRulesBtn.setDescription("Save Rules");
        saveRulesBtn.setIcon(FontAwesome.SAVE);
        saveRulesBtn.addClickListener(e -> {
            try {
                FileWriter fileWriter = new FileWriter(ruleFileName.getValue());
                rulesTable.forEach(component -> {
                    if ((component instanceof RuleRow ruleRow)) {
                        try {
                            fileWriter.append(ruleRow.toExpression()).append("\n");
                        } catch (IOException ignore) {
                        }
                    }
                });
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException ignore) {
            }
        });

        fileManagementLayout.addComponent(ruleFileName);
        fileManagementLayout.addComponent(loadRulesBtn);
        fileManagementLayout.addComponent(saveRulesBtn);

        addComponent(fileManagementLayout);

        rulesTable.setSpacing(true);
        rulesTable.setMargin(false);
        rulesTable.setWidthUndefined();   // <-- important

        // contentLayout inside panel must be allowed to grow wider than panel
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSpacing(true);
        contentLayout.setMargin(true);
        contentLayout.setWidthUndefined();      // <-- important
        contentLayout.addComponent(rulesTable);

        Panel scrollPane = new Panel();
        scrollPane.setWidth("100%");     // <-- fixed width, real scroll container
        scrollPane.setHeight("500px");
        scrollPane.addStyleName("predicate-scroll-pane");
        scrollPane.setContent(contentLayout);

        addComponent(scrollPane);

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setSpacing(true);

        Button addRuleBtn = new Button();
        addRuleBtn.setDescription("Add Rule");
        addRuleBtn.setIcon(FontAwesome.PLUS);
        addRuleBtn.addClickListener(e -> addRuleRow());

        Button resetBtn = new Button();
        resetBtn.setDescription("Reset");
        resetBtn.setIcon(FontAwesome.REPEAT);
        resetBtn.addClickListener(e -> {
            rulesTable.removeAllComponents();
            addRuleRow();
        });

        actionsLayout.addComponent(addRuleBtn);
        actionsLayout.addComponent(resetBtn);

        addComponent(actionsLayout);

        addRuleRow();
    }

    /**
     * Adds a new rule row to the rules table.
     * <p>
     * This method creates a new {@code RuleRow} using the provided rule ID and contents.
     * The created rule row has a remove handler bound to it, allowing for dynamic
     * removal from the rule table. The width for the row is set to be undefined, and
     * the row is added to the {@code rulesTable} component.
     *
     * @param ruleId   the unique identifier for the rule. It is used to populate the
     *                 field selector in the rule row.
     * @param contents an array of strings representing the components of the rule's
     *                 expression. These components are parsed to configure the rule
     *                 row with the appropriate URIs and operations.
     */
    private void addRuleRow(String ruleId, String[] contents) {
        RuleRow row = new RuleRow(definitions, ruleId, contents);
        row.setRemoveHandler(() -> rulesTable.removeComponent(row));
        row.setWidthUndefined();          // <-- important
        rulesTable.addComponent(row);
    }

    /**
     * Adds a new rule row to the rules table.
     * <p>
     * This method creates an instance of {@code RuleRow}, configures it with a remove handler
     * for dynamic removal from the rule table, sets its width property to be undefined,
     * and adds it to the {@code rulesTable} layout.
     * <p>
     * Key behavior:
     * - The remove handler allows the row to be removed when invoked.
     * - The row's width is not explicitly fixed, enabling it to adapt to its contents.
     * - The row is added as a child component to the {@code rulesTable}.
     */
    private void addRuleRow() {
        RuleRow row = new RuleRow(definitions);
        row.setRemoveHandler(() -> rulesTable.removeComponent(row));
        row.setWidthUndefined();          // <-- important
        rulesTable.addComponent(row);
    }
}
