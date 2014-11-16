/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


/**
 * <p>
 * Generic confirmation dialog with OK and Cancel buttons
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 24, 2013
 */
public class ConfirmDialog extends Window implements ClickListener
{
    private static final long serialVersionUID = 421868377844545305L;
    private static String DEFAULT_CAPTION = "Please Confirm";
    private static String DEFAULT_OK_CAPTION = "Yes";
    private static String DEFAULT_CANCEL_CAPTION = "No";
    
    private boolean confirmed;
    private Button okButton, cancelButton;
    
        
    public ConfirmDialog(String message)
    {
        this(DEFAULT_CAPTION, message, DEFAULT_OK_CAPTION, DEFAULT_CANCEL_CAPTION);
    }
    
    
    public ConfirmDialog(String caption, String message, String okButtonText, String cancelButtonText)
    {
        super(caption);
        super.setModal(true);
        super.setClosable(false);
        super.setResizable(false);
        
        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setMargin(true);
        
        // confirmation message
        windowLayout.addComponent(new Label(message, ContentMode.HTML));
        windowLayout.setSpacing(true);
        
        // buttons
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidth(100.0f, Unit.PERCENTAGE);
        windowLayout.addComponent(buttonsLayout);
        
        okButton = new Button(okButtonText);
        buttonsLayout.addComponent(okButton);
        okButton.setTabIndex(1);
        okButton.addClickListener(this);
        buttonsLayout.setComponentAlignment(okButton, Alignment.MIDDLE_CENTER);
                
        cancelButton = new Button(cancelButtonText);
        buttonsLayout.addComponent(cancelButton);
        cancelButton.setTabIndex(0);
        cancelButton.setClickShortcut(KeyCode.ESCAPE, null);
        cancelButton.addClickListener(this);
        buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_CENTER);
                
        super.setContent(windowLayout);
    }
    
    
    public boolean isConfirmed()
    {
        return confirmed;
    }


    @Override
    public void buttonClick(ClickEvent event)
    {
        if (event.getComponent() == okButton)
            confirmed = true;
        this.close();    
    }

}
