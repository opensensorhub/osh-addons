/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2026 GeoRobotiX Innovative Research. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.ui.rulesengine.editor;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Represents a row of rules in a user interface, composed of a selectable field,
 * a grid of URI components, and actions to add or remove URIs or the entire row.
 * This class provides functionality to build expressions based on user input,
 * dynamically manage URI components, and handle the removal of rows when needed.
 */
public class RuleRow extends HorizontalLayout {

    /**
     * A {@code TextField} component used to capture and display the identifier
     * of a rule within a {@code RuleRow}.
     * <p>
     * The {@code ruleIdField} serves as an input field where users can specify
     * or edit the unique identifier for the rule that is represented by
     * the containing {@code RuleRow}.
     * <p>
     * This field is configured as a final component and is initialized
     * with default settings to ensure consistent appearance and behavior
     * across instances.
     */
    private final TextField ruleIdField = new TextField();

    /**
     * A GridLayout used to organize and arrange URI-related columns within a rule row.
     * This grid serves as the container for dynamically added or removed URI components,
     * such as those represented by the {@link UriColumn} class. The layout is initialized
     * with a single row and a single column as a basis for expanding and managing rule
     * configurations.
     * <p>
     * The uriGrid is primarily used within the {@link RuleRow} class to handle the
     * visual and logical representation of URI operations and their configurations.
     * Columns in this grid allow users to specify URI-related details, logical
     * negations, and operators through its individual components.
     */
    private final GridLayout uriGrid = new GridLayout(1, 1);

    /**
     * A callback handler that is invoked when the removal of a URI column is triggered.
     * This variable is typically assigned with a {@link Runnable} implementation
     * that defines the specific behavior for handling the removal action.
     */
    private Runnable removeHandler;

    /**
     * A list of string definitions associated with a RuleRow instance.
     * Represents contextual information, such as predefined values,
     * options, or configurations used during the initialization and
     * management of rule components.
     * <p>
     * This list is immutable once initialized and is used to populate
     * or configure specific properties or UI elements related to the rule.
     */
    private final List<String> definitions;


    /**
     * Constructs a new RuleRow instance.
     * This constructor initializes a rule row in the user interface, including a field selector,
     * a dynamic URI grid for configuring logical conditions, and buttons for adding URI columns
     * or removing the entire rule row.
     *
     * @param definitions a list of string definitions used to initialize the rule's context,
     *                    such as available options or predefined values for certain components.
     */
    public RuleRow(final List<String> definitions) {
        this.definitions = definitions;
        setSpacing(true);
        setWidthUndefined();

        // Field selector
        ruleIdField.setWidth(250, Unit.PIXELS);
        ruleIdField.setPlaceholder("Rule ID");

        // URI grid (1 row, dynamic columns)
        uriGrid.setSpacing(true);

        addUriColumn();

        Button addUriBtn = new Button();
        addUriBtn.setDescription("Add URI");
        addUriBtn.setIcon(FontAwesome.PLUS, "Add URI");
        addUriBtn.addClickListener(e -> addUriColumn());

        Button removeRuleBtn = new Button();
        removeRuleBtn.setDescription("Remove Rule");
        removeRuleBtn.setIcon(FontAwesome.TRASH_O, "Remove Rule");
        removeRuleBtn.addClickListener(e -> removeHandler.run());

        addComponents(removeRuleBtn, ruleIdField, uriGrid, addUriBtn);
    }

    /**
     * Constructs a new RuleRow instance.
     * This constructor initializes a rule row in the user interface, including a field selector,
     * a dynamic URI grid for configuring logical conditions, and buttons for adding URI columns
     * or removing the entire rule row.
     *
     * @param definitions a list of string definitions used to initialize the rule's context,
     *                    such as available options or predefined values for certain components.
     * @param ruleId      the identifier representing the specific rule to be displayed or
     *                    modified in this row.
     * @param contents    an array of string expressions or tokens defining the initial condition
     *                    set for the rule. These could include URIs and logical operators (e.g., "&&").
     */
    public RuleRow(final List<String> definitions, String ruleId, String[] contents) {
        this.definitions = definitions;
        setSpacing(true);
        setWidthUndefined();

        // Field selector
        this.ruleIdField.setWidth(250, Unit.PIXELS);
        ruleIdField.setPlaceholder("Rule ID");
        this.ruleIdField.setValue(ruleId);

        // URI grid (1 row, dynamic columns)
        uriGrid.setSpacing(true);

        boolean negation = false;
        String op = null;
        String uri = null;

        Stack<String> stack = new Stack<>();
        List<String> list = new ArrayList<>(List.of(contents));
        Collections.reverse(list);
        stack.addAll(list);

        while (!stack.isEmpty()) {

            String value = stack.peek();
            switch (value.toUpperCase()) {
                case "!":
                case "NOT":
                    negation = true;
                    stack.pop();
                    break;
                case "||":
                case "OR":
                    stack.pop();
                    op = "||";
                    break;
                case "&&":
                case "AND":
                    stack.pop();
                    op = "&&";
                    break;
                default:
                    uri = stack.pop();
                    addUriColumn(negation, op, uri);
                    negation = false;
                    op = null;
                    break;
            }
        }

        Button addUriBtn = new Button();
        addUriBtn.setDescription("Add URI");
        addUriBtn.setIcon(FontAwesome.PLUS, "Add URI");
        addUriBtn.addClickListener(e -> addUriColumn());

        Button removeRuleBtn = new Button();
        removeRuleBtn.setDescription("Remove Rule");
        removeRuleBtn.setIcon(FontAwesome.TRASH_O, "Remove Rule");
        removeRuleBtn.addClickListener(e -> removeHandler.run());

        addComponents(removeRuleBtn, this.ruleIdField, uriGrid, addUriBtn);
    }

    /**
     * Adds a new URI column to the `uriGrid` dynamically. The column can represent
     * a URI with optional logical and negation operators. This method adjusts the
     * grid layout to accommodate the new column and sets a remove handler to enable
     * column removal functionality.
     *
     * @param negate A boolean flag indicating whether to apply a negation operator
     *               to the URI in the new column.
     * @param op     The logical operator ("&&" or "||") to be associated with the
     *               new URI column. This can be null for single-column grids.
     * @param uri    The URI value to be set in the new column.
     */
    private void addUriColumn(boolean negate, String op, String uri) {
        int col = uriGrid.getColumns();
        uriGrid.setColumns(col + 1);

        UriColumn colComp;
        if (col == 1) {
            colComp = new UriColumn(definitions, negate, uri);
        } else {
            colComp = new UriColumn(definitions, negate, op, uri);
        }
        colComp.setRemoveHandler(() -> removeUriColumn(colComp));

        uriGrid.addComponent(colComp, col, 0);
    }

    /**
     * Dynamically adds a new URI column to the `uriGrid` layout.
     * <p>
     * This method increases the column count of the grid and inserts a new
     * `UriColumn` component into the grid at the incremented column position.
     * The new URI column is initialized with a remove handler that allows
     * the column to be removed dynamically when triggered.
     * <p>
     * The method performs the following steps:
     * 1. Retrieves the current number of columns in `uriGrid`.
     * 2. Increments the column count to make room for the new URI column.
     * 3. Creates a new `UriColumn` instance with default settings.
     * 4. Configures the remove handler of the new column to invoke the
     * `removeUriColumn` method, enabling its removal functionality.
     * 5. Adds the new `UriColumn` to the grid at the specified column and row.
     */
    private void addUriColumn() {
        int col = uriGrid.getColumns();
        uriGrid.setColumns(col + 1);

        UriColumn colComp;
        if (col == 1) {
            colComp = new UriColumn(definitions, false, null);
        } else {
            colComp = new UriColumn(definitions, false, null, null);
            colComp.setRemoveHandler(() -> removeUriColumn(colComp));
        }
        colComp.setRemoveHandler(() -> removeUriColumn(colComp));

        uriGrid.addComponent(colComp, col, 0);
    }

    private void removeUriColumn(UriColumn colComp) {
        int colIndex = uriGrid.getComponentArea(colComp).getColumn1();
        uriGrid.removeComponent(colComp);

        int cols = uriGrid.getColumns();
        for (int c = colIndex + 1; c < cols; c++) {
            Component comp = uriGrid.getComponent(c, 0);
            uriGrid.removeComponent(comp);
            uriGrid.addComponent(comp, c - 1, 0);
        }

        uriGrid.setColumns(cols - 1);
    }

    /**
     * Constructs a string representation of the rule's expression by combining
     * the selected field and the components built from the URI grid.
     * <p>
     * This method processes the `uriGrid` to collect all configured URI column
     * values, concatenates them, and prefixes the combined result with the
     * field selected in `fieldBox`. If the `fieldBox` value is null or no valid
     * URI components exist in the grid, the method returns null.
     *
     * @return a string representing the field combined with the URI column expressions,
     * or null if the field or URI components are not specified.
     */
    public String toExpression() {
        String field = ruleIdField.getValue();
        if (field == null) return null;

        List<String> parts = new ArrayList<>();

        for (int c = 0; c < uriGrid.getColumns(); c++) {
            Component comp = uriGrid.getComponent(c, 0);
            if (comp instanceof UriColumn) {
                String built = ((UriColumn) comp).toExpression();
                if (built != null) parts.add(built);
            }
        }

        if (parts.isEmpty()) return null;

        return field + String.join("", parts);
    }

    /**
     * Sets the remove handler for this rule row.
     * <p>
     * The remove handler is a {@code Runnable} instance that will be invoked
     * when the associated remove action is triggered. This allows external
     * logic to define and execute the behavior for removing a rule row, such as
     * updating the UI or performing cleanup operations.
     *
     * @param removeHandler a {@code Runnable} that defines the removal logic for the rule row
     */
    public void setRemoveHandler(Runnable removeHandler) {
        this.removeHandler = removeHandler;
    }
}