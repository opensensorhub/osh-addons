/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2026 GeoRobotiX Innovative Research. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.ui.rulesengine.editor;

import com.georobotix.ai.impl.rulesengine.config.RuleEditor;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.MyBeanItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a form for editing and managing rules within a user interface framework.
 * This class extends the GenericConfigForm and provides functionality to display and
 * interact with predicate rules. The form is initialized with spacing, margins, and
 * a table for managing these rules.
 * <p>
 * The RuleEditorForm processes data stream components to extract unique definitions
 * from record structures and organizes these definitions for use in the rule table.
 */
public class RuleEditorForm extends GenericConfigForm {

    /**
     * A list of unique definitions extracted from data stream components and record structures.
     * These definitions are managed and used within the rule table of the RuleEditorForm.
     * The list is populated through recursive traversal of record structures to ensure all
     * relevant definitions are identified and stored without duplication.
     */
    private final List<String> definitions = new ArrayList<>();

    /**
     * Builds and configures the RuleEditorForm by initializing and adding components
     * based on the provided bean item and other parameters. This method is specifically
     * designed to handle RuleEditor instances and their associated rule configurations.
     *
     * @param title           The title for the form.
     * @param popupText       Text to be displayed in the form's popup.
     * @param beanItem        A bean item containing the object to be used for the form configuration.
     * @param includeSubForms A boolean flag indicating whether sub-forms should be included.
     */
    @Override
    public void build(String title, String popupText, MyBeanItem<Object> beanItem, boolean includeSubForms) {
        super.build(title, popupText, beanItem, includeSubForms);

        if (beanItem.getBean() instanceof RuleEditor) {

            getParentHub().getDatabaseRegistry().getObsSystemDatabases().forEach(iObsSystemDatabase -> {
                iObsSystemDatabase.getDataStreamStore().forEach((dataStreamKey, iDataStreamInfo) -> {
                    mineDataComponents(iDataStreamInfo.getRecordStructure());
                });
            });

            addComponent(new PredicateRuleTable(definitions));
        }
    }

    /**
     * Recursively traverses a given data component structure to extract unique definitions.
     * This method inspects each component in the structure, adds its definition to a
     * managed list if it does not already exist, and then processes its sub-components.
     *
     * @param recordStructure The root data component to traverse. This represents a node in the
     *                        hierarchical structure containing definitions and sub-components.
     */
    private void mineDataComponents(DataComponent recordStructure) {

        String definition = recordStructure.getDefinition();

        if (definition != null) {

            if (!definitions.contains(definition)) {

                definitions.add(definition);
            }
        }

        for (int idx = 0; idx < recordStructure.getComponentCount(); ++idx) {

            mineDataComponents(recordStructure.getComponent(idx));
        }
    }
}
