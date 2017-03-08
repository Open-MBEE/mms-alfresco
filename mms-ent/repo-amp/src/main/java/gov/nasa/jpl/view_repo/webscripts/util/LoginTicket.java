/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.TicketComponent;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;


/**
 * Login Ticket copied from org.alfresco.repo.web.scripts.bean.LoginTicket
 *
 * @author davidc
 */
public class LoginTicket extends DeclarativeWebScript
{
    // dependencies
    private TicketComponent ticketComponent;

    /**
     * @param ticketComponent
     */
    public void setTicketComponent(TicketComponent ticketComponent)
    {
        this.ticketComponent = ticketComponent;
    }


    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.DeclarativeWebScript#executeImpl(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.WebScriptResponse)
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status)
    {
        // retrieve ticket from request and current ticket
        String ticket = req.getExtensionPath();
        if (ticket == null || ticket.length() == 0)
        {
            throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Ticket not specified");
        }

        // construct model for ticket
        Map<String, Object> model = new HashMap<>(1, 1.0f);
        //model.put("ticket",  ticket);

        String username = null;
        JSONObject result = new JSONObject();
        try {
            username = ticketComponent.validateTicket(ticket);
            result.put("username", username);
        } catch (AuthenticationException e) {
            //status.setRedirect(true);
            status.setCode(HttpServletResponse.SC_NOT_FOUND);
            status.setMessage("Ticket not found");
            result.put("message", "Ticket not found");
        }

        model.put( "res", result.toString() );
        return model;
    }

}
