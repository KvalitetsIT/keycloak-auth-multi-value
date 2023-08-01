package dk.kvalitetsit.keycloak.auth.multi.value;

import dk.kvalitetsit.keycloak.auth.multi.value.requiredaction.ChooseAttributeRequiredAction;
import dk.kvalitetsit.keycloak.auth.multi.value.requiredaction.ChooseAttributeRequiredActionFactory;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;

public class ChooseAttributeRequiredActionTest {

    private ChooseAttributeRequiredAction chooseAttributeRequiredAction;

    private final String attributeName = "attribute";

    @Before
    public void setup() {
        chooseAttributeRequiredAction = new ChooseAttributeRequiredAction(attributeName);
    }

    @Test
    public void testEvaluateTriggers() {
        RequiredActionContext context = Mockito.mock(RequiredActionContext.class);
        UserModel userModel = Mockito.mock(UserModel.class);
        KeycloakSession keycloakSession = Mockito.mock(KeycloakSession.class);
        List<String> attributeList = new ArrayList<>();
        attributeList.add("attribute 1");
        attributeList.add("attribute 2");
        attributeList.add("attribute 3");

        Mockito.when(context.getUser()).thenReturn(userModel);
        Mockito.when(userModel.getId()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(userModel.getAttributeStream(attributeName)).thenReturn(attributeList.stream());
        Mockito.when(context.getSession()).thenReturn(keycloakSession);

        chooseAttributeRequiredAction.evaluateTriggers(context);

        Mockito.verify(context, times(2)).getUser();
        Mockito.verify(userModel, times(1)).getAttributeStream(attributeName);
        Mockito.verify(userModel, times(1)).addRequiredAction(ChooseAttributeRequiredActionFactory.PROVIDER_ID);
    }

    @Test
    public void testEvaluateTriggersOneAttribute() {
        RequiredActionContext context = Mockito.mock(RequiredActionContext.class);
        UserModel userModel = Mockito.mock(UserModel.class);
        KeycloakSession keycloakSession = Mockito.mock(KeycloakSession.class);
        List<String> attributeList = new ArrayList<>();
        attributeList.add("attribute 1");

        Mockito.when(context.getUser()).thenReturn(userModel);
        Mockito.when(userModel.getId()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(userModel.getAttributeStream(attributeName)).thenReturn(attributeList.stream());
        Mockito.when(context.getSession()).thenReturn(keycloakSession);

        chooseAttributeRequiredAction.evaluateTriggers(context);

        Mockito.verify(context, times(2)).getUser();
        Mockito.verify(userModel, times(1)).getAttributeStream(attributeName);
        Mockito.verify(userModel, times(0)).addRequiredAction(ChooseAttributeRequiredActionFactory.PROVIDER_ID);
        Mockito.verify(userModel, times(1)).setAttribute(attributeName, Collections.singletonList("attribute 1"));
    }

    @Test
    public void testEvaluateTriggersNoAttributes() {
        RequiredActionContext context = Mockito.mock(RequiredActionContext.class);
        UserModel userModel = Mockito.mock(UserModel.class);
        KeycloakSession keycloakSession = Mockito.mock(KeycloakSession.class);
        List<String> attributeList = new ArrayList<>();

        Mockito.when(context.getUser()).thenReturn(userModel);
        Mockito.when(userModel.getAttributeStream(attributeName)).thenReturn(attributeList.stream());
        Mockito.when(context.getSession()).thenReturn(keycloakSession);

        chooseAttributeRequiredAction.evaluateTriggers(context);

        Mockito.verify(context, times(1)).getUser();
        Mockito.verify(userModel, times(1)).getAttributeStream(attributeName);
        Mockito.verify(userModel, times(0)).addRequiredAction(ChooseAttributeRequiredActionFactory.PROVIDER_ID);
    }
}
