package org.sensorhub.impl.service.federation.environment;

import java.util.Base64;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Port of environment.AuthData. Also serves as an admin-UI config object: this
 * is the {@code auth} block that lived in broker-env2.json.
 */
public class AuthData
{
    @DisplayInfo(label = "Type", desc = "Authentication type (basic or oauth)")
    public String type = "basic";

    @DisplayInfo(label = "Username", desc = "Username for the node")
    public String username;

    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    @DisplayInfo(label = "Password", desc = "Password for the node")
    public String password;

    public String base64;
    public String secret;

    public void encode()
    {
        if ("basic".equals(type))
        {
            String userPass = username + ":" + password;
            this.base64 = Base64.getEncoder().encodeToString(userPass.getBytes());
        }
    }
}
