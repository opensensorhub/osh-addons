/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sensorhub.api.security.ISecurityManager;
import de.fraunhofer.iosb.ilt.frostserver.http.common.ServletV1P0;


/**
 * <p>
 * Extension of FROST STA 1.0 servlet
 * </p>
 *
 * @author Alex Robin
 * @date Oct 18, 2019
 */
public class STAServlet extends ServletV1P0
{
    private static final long serialVersionUID = 6257719486841697633L;
    
    STAService service;
    STASecurity securityHandler;
    
    
    STAServlet(STAService service)
    {
        this.securityHandler = service.getSecurityHandler();
    }
    
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // set current authentified user
        String userID = ISecurityManager.ANONYMOUS_USER;
        if (request.getRemoteUser() != null)
            userID = request.getRemoteUser();
        
        securityHandler.setCurrentUser(userID);
        super.service(request, response);
        
        Exception authError = securityHandler.getPermissionError();
        if (authError != null)
        {
            response.reset();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().print(authError.getMessage());
        }
    }
        
}
