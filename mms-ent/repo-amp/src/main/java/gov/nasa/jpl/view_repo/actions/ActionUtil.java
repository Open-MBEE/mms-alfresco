/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.springframework.extensions.webscripts.Status;

/**
 * Static class of Action utilities for saving log, sending email, etc.
 * @author cinyoung
 *
 */
public class ActionUtil {

    // defeat instantiation
    private ActionUtil() {
        // do nothing
    }

    /**
     * Send off an email to the modifier of the node
     * @param node      Node whose modifier should be sent an email
     * @param msg       Message to send modifier
     * @param subject   Subjecto of message
     * @param services
     */
    public static void sendEmailToModifier(EmsScriptNode node, String msg, String subject, ServiceRegistry services) {
        String username = (String)node.getProperty("cm:modifier", false);
        EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username), services, new StringBuffer());
        String recipient = (String) user.getProperty("cm:email");

        String sender = NodeUtil.getHostname() + "@jpl.nasa.gov";
        sendEmailTo(sender, recipient, msg, subject, services);
    }

    /**
     * Send email to recipient
     * @param sender
     * @param recipient
     * @param msg
     * @param subject
     * @param services
     */
    public static void sendEmailTo(String sender, String recipient, String msg, String subject, ServiceRegistry services) {
        try {
            Action mailAction = services.getActionService().createAction(MailActionExecuter.NAME);
            mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
            mailAction.setParameterValue(MailActionExecuter.PARAM_TO, recipient);
            // strip out origin from message and sender as necessary, since url won't match
            sender = sender.replace("-origin", "");
            mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, sender);
            msg = msg.replace("-origin", "");
            mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, msg);
            mailAction.setExecuteAsynchronously( true );
            services.getActionService().executeAction(mailAction, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
