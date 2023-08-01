package dk.kvalitetsit.keycloak.auth.multi.value.requiredaction;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChooseAttributeRequiredActionFactory implements RequiredActionFactory {

    private static final Logger logger = LoggerFactory.getLogger(ChooseAttributeRequiredActionFactory.class);

    public static String PROVIDER_ID = "REQUIRED_ACTION_CHOOSE";

    private static final String CONFIG_ATTRIBUTE = "required_action_choose_attribute_attribute_name";



    @Override
    public RequiredActionProvider create(KeycloakSession session) {

        String realmName = session.getContext().getRealm().getName();

        String attribute = System.getenv(CONFIG_ATTRIBUTE);

        logger.debug("Configured ChooseAttribute on realm '" + realmName + "' is configured "+ CONFIG_ATTRIBUTE + ":" + attribute);

        return new ChooseAttributeRequiredAction(attribute);
    }

    @Override
    public void init(Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Valg af attribut til at logge ind ved.";
    }
}
