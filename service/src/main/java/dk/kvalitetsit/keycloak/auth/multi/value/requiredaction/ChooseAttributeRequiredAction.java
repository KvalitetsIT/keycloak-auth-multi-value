package dk.kvalitetsit.keycloak.auth.multi.value.requiredaction;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChooseAttributeRequiredAction implements RequiredActionProvider {

    private static final Logger logger = LoggerFactory.getLogger(ChooseAttributeRequiredAction.class);

    public static final String FORM_PARAMETER_ATTRIBUTE = "attribute";
    private final String attributeName;


    public ChooseAttributeRequiredAction(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        logger.debug("Evaluating triggers.");
        List<String> attributeList = getAttributeList(context);

        if (attributeList.isEmpty()) {
            logger.debug("No attributes bound to this user, hence no need for choosing one.");

        } else if (attributeList.size() == 1){
            logger.debug("Only one attribute bound to user, hence no need for choosing one.");
            // Add attribute to user
            setAttributeUser(context, attributeList.get(0));

        } else {
            logger.debug("More than one attribute bound to user. Adding required action.");

            // Make user choose attribute
            addChooseAttributeAction(context);
        }

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        LoginFormsProvider form = context.form();

        form.setAttribute("attribute_list", getAttributeList(context));

        form.setAttribute("attribute_name", attributeName);

        Response challenge = form.createForm("attribute.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        logger.debug("Processing action.");

        String attribute = getAttributeFromForm(context);

        setAttributeUser(context, attribute);

        context.success();
    }

    @Override
    public void close() {

    }

    private List<String> getAttributeList(RequiredActionContext context) {
        return context.getUser().getAttributeStream(attributeName).collect(Collectors.toList());
    }

    private String getAttributeFromForm(RequiredActionContext context) {
        return context.getHttpRequest().getDecodedFormParameters().getFirst(FORM_PARAMETER_ATTRIBUTE);
    }

    private void setAttributeUser(RequiredActionContext context, String attribute) {
        context.getUser().setAttribute(attributeName, Collections.singletonList(attribute));
    }

    private void addChooseAttributeAction(RequiredActionContext context) {
        context.getUser().addRequiredAction(ChooseAttributeRequiredActionFactory.PROVIDER_ID);
    }
}
