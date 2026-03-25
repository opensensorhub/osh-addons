package org.sensorhub.impl.sensor.mavsdk.ui;

import com.vaadin.ui.*;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.mavsdk.control.UnmannedControlEnableLocation;
import org.sensorhub.impl.system.CommandStreamTransactionHandler;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.ui.AdminUI;
import org.sensorhub.ui.DisplayUtils;
import org.sensorhub.ui.SWEControlForm;
import org.sensorhub.ui.SensorAdminPanel;
import org.sensorhub.ui.api.UIConstants;

import java.security.SecureRandom;
import java.util.Random;

public class MavLinkUI extends SensorAdminPanel {

    VerticalLayout commandsSection;
    CommandStreamTransactionHandler commandTransactionHandler;

    public class CheckBoxControlForm extends HorizontalLayout {

        transient IStreamingControlInterface controlInput;
        transient String userID;
        transient CommandStreamTransactionHandler commandTxn;
        transient DataComponent component;
        transient Random random = new SecureRandom();

        public CheckBoxControlForm(IStreamingControlInterface controlInput) {
            this.controlInput = controlInput;
            this.component = controlInput.getCommandDescription().copy();
            buildForm();
        }

        @Override
        public void attach() {
            super.attach();

            if (controlInput != null) {
                var ui = ((AdminUI)this.getUI());

                var user = ui.getSecurityHandler().getCurrentUser();
                this.userID = user != null ? user.getId() : "adminUI";

                var sysUID = controlInput.getParentProducer().getUniqueIdentifier();
                var eventBus = ui.getParentHub().getEventBus();
                var db = ui.getParentHub().getDatabaseRegistry().getFederatedDatabase();
                var sysTxnHandler = new SystemDatabaseTransactionHandler(eventBus, db);
                this.commandTxn = sysTxnHandler.getCommandStreamHandler(sysUID, controlInput.getName());
            }
        }

        private void buildForm() {
            CheckBox box = new CheckBox(component.getLabel());
            Button button = new Button("Send Command");
            button.addStyleName(UIConstants.STYLE_SMALL);

            addComponent(box);
            addComponent(button);

            setComponentAlignment(button, Alignment.MIDDLE_LEFT);

            box.addValueChangeListener(event -> {
                if (!component.hasData())
                    component.renewDataBlock();
                component.getData().setBooleanValue(event.getValue());
            });

            button.addClickListener((Button.ClickListener) event -> {
                try {
                    var cmdData = component.getData().clone();

                    if (commandTxn != null) {
                        var cmd = new CommandData.Builder()
                                .withSender(userID)
                                .withCommandStream(commandTxn.getCommandStreamKey().getInternalID())
                                .withParams(cmdData)
                                .build();
                        commandTxn.submitCommand(random.nextLong(), cmd, null);
                    }
                } catch (Exception e) {
                    DisplayUtils.showErrorPopup("Error while sending command with checkbox", e);
                }
            });
        }
    }

    @Override
    protected void buildControlInputsPanels(ISensorModule<?> module) {
        if (module != null) {
            VerticalLayout oldSection;

            // command inputs
            oldSection = commandsSection;
            commandsSection = new VerticalLayout();
            commandsSection.setMargin(false);
            commandsSection.setSpacing(true);
            for (IStreamingControlInterface input: module.getCommandInputs().values())
            {
                    var panel = newPanel(null);
                    Component sweForm = new SWEControlForm(input);
                    if (input.getName().equals("mavEnableLocationControl"))
                        sweForm = new CheckBoxControlForm(input);
                    ((Layout) panel.getContent()).addComponent(sweForm);
                    commandsSection.addComponent(panel);
            }

            if (oldSection != null)
                replaceComponent(oldSection, commandsSection);
            else
                addComponent(commandsSection);
        }
    }
}
