/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2026 GeoRobotiX Innovative Research. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.ui.rulesengine.editor;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;

import java.util.List;

/**
 * Represents a visual component for constructing URI-based expressions
 * with optional negation and logical operators. The component includes
 * fields for unary operators, URI input, and logical operators, and a
 * remove button for removal functionality.
 *
 * <h2>Key Features:</h2>
 * - Unary operator selection.
 * - URI input field with placeholder text.
 * - Logical operator selection (optional).
 * - Remove button to handle component-specific actions.
 */
public class UriColumn extends HorizontalLayout {

    /**
     * A handler that encapsulates the action to be executed when a specific removal logic is performed.
     * Typically used to define custom behavior for removing elements or performing cleanup operations.
     */
    private Runnable removeHandler;

    /**
     * A ComboBox component used to represent unary operators in the context of URI column manipulation.
     * This ComboBox allows the user to select from a predefined list of unary operations.
     * It is a private final member, ensuring immutability within the class and restricting access
     * to within the declaring class.
     */
    private final ComboBox<String> unary = new ComboBox<>();

    /**
     * A private final TextField instance used to represent the URI input field within the {@code UriColumn} class.
     * This field is used for user input specific to URI data in the context of the class's functionality.
     */
    private final ComboBox<String> uri = new ComboBox<>();

    /**
     * A ComboBox component used to select an operator for constructing a URI column.
     * This ComboBox is initialized to handle String values and provides
     * a dropdown interface for selecting specific operator options.
     * It is declared as a final field to ensure immutability of the reference.
     */
    private final ComboBox<String> op = new ComboBox<>();

    /**
     * Constructs a UriColumn component with specified URI definitions, an initial negation state,
     * and a default URI value. Configures the layout to include:
     * - A dropdown for a logical unary operator (e.g., "!") with predefined options.
     * - A URI input field with a placeholder and predefined definitions.
     * - A remove button that allows the user to remove the URI entry and triggers an optional handler.
     * <p>
     * The unary operator dropdown supports toggling between no operator and "!". The URI input field
     * is initialized with the given placeholder and list of definitions.
     *
     * @param definitions A list of string values to populate the URI input field's dropdown options.
     * @param negate      A boolean flag indicating whether the unary operator should be initialized with negation ("!").
     * @param value       The initial value to set in the URI input field.
     */
    public UriColumn(final List<String> definitions, boolean negate, String value) {
        unary.setItems("", "!");
        unary.setEmptySelectionAllowed(false);
        unary.setWidth(60, Unit.PIXELS);
        unary.setValue(negate ? "!" : "");

        uri.setWidth(500, Unit.PIXELS);
        uri.setPlaceholder("http://...");
        uri.setItems(definitions);
        uri.setValue(value);

        Button remove = new Button();
        remove.setDescription("Remove URI");
        remove.setIcon(FontAwesome.TRASH_O, "Remove URI");
        remove.addClickListener(e -> {
            if (removeHandler != null) removeHandler.run();
        });

        addComponents(unary, uri, remove);
    }

    /**
     * Constructs a UriColumn component with the specified list of URI definitions,
     * an initial negation state, a binary operator, and a default URI value.
     * Configures the layout to include:
     * - A dropdown for a binary logical operator (e.g., "&&", "||") with predefined options.
     * - A dropdown for a logical unary operator (e.g., "!") with predefined options.
     * - A URI input field with a placeholder text and predefined definitions.
     * - A remove button that triggers an optional removal handler when clicked.
     * <p>
     * The component initializes the binary operator dropdown to allow empty selection,
     * the unary operator based on the provided negation state, and the URI input field
     * with the provided definitions and value.
     *
     * @param definitions A list of string values to populate the URI input field's dropdown options.
     * @param negate      A boolean flag indicating whether the unary operator should be initialized with negation ("!").
     * @param operator    The initial value to set in the binary operator dropdown (e.g., "&&" or "||").
     * @param value       The initial value to set in the URI input field.
     */
    public UriColumn(List<String> definitions, boolean negate, String operator, String value) {

        op.setItems("&&", "||");
        op.setEmptySelectionAllowed(true);
        op.setWidth(80, Unit.PIXELS);
        op.setValue(operator);

        unary.setItems("", "!");
        unary.setEmptySelectionAllowed(false);
        unary.setWidth(60, Unit.PIXELS);
        unary.setValue(negate ? "!" : "");

        uri.setWidthUndefined();
        uri.setPlaceholder("http://...");
        uri.setWidth(500, Unit.PIXELS);
        uri.setItems(definitions);
        uri.setValue(value);

        Button remove = new Button();
        remove.setDescription("Remove URI");
        remove.setIcon(FontAwesome.TRASH_O, "Remove URI");
        remove.addClickListener(e -> {
            if (removeHandler != null) removeHandler.run();
        });

        addComponents(op, unary, uri, remove);
    }

    /**
     * Sets the handler to be executed when the remove button is clicked.
     * The handler defines the behavior triggered by the removal action.
     *
     * @param handler A Runnable representing the action to execute upon clicking the remove button.
     *                If set to null, clicking the remove button will perform no action.
     */
    public void setRemoveHandler(Runnable handler) {
        this.removeHandler = handler;
    }

    /**
     * Builds and returns a string representation based on the values of the unary operator,
     * binary operator, and URI field. If all fields are null or empty, the result will be null.
     * <p>
     * The constructed string follows this order:
     * 1. Binary operator, if present.
     * 2. Unary operator, if present.
     * 3. URI value, if present.
     * <p>
     * Each value is appended with a leading space for proper formatting.
     *
     * @return A formatted string combining the binary operator, unary operator, and URI value,
     * or null if all individual values are either null or empty.
     */
    public String toExpression() {

        String expr = "";
        String o = op.getValue();
        if (o != null) {
            expr += " " + o;
        }

        String u = unary.getValue();
        if (u != null && !u.trim().isEmpty()) {
            expr += " " + u;
        }

        String v = uri.getValue();
        if (v != null && !v.trim().isEmpty()) {
            expr += " " + v;
        }

        if (expr.isEmpty()) {
            expr = null;
        }

        return expr;
    }
}
