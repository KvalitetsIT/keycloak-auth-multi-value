package dk.kvalitetsit.keycloak.auth.multi.value;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import dk.kvalitetsit.keycloak.auth.multi.value.dto.User;
import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class ChooseAttributeRequiredActionIT extends AbstractIntegrationTest {

    @Test
    public void testRequiredActionNoAttribute() throws IOException, JSONException {
        //Given
        String username = "User1";
        String userId = UUID.randomUUID().toString();
        String password = addUserAttributeRealm(username, new ArrayList<>(), userId);
        WebClient webdriver = getWebDriver();

        //When..Then
        HtmlPage afterLogin = doLoginFlow(webdriver, username, password);
        assertFalse(isAttributeInputPage(afterLogin));

        // User is created and has no attributes
        User userAttributeRealm = getUserAttributeRealm(userId);
        assertNotNull(userAttributeRealm);
        String usernameTestRealm = userAttributeRealm.getAttributes().get("saml.persistent.name.id.for.http://localhost:8080/auth/realms/Test").get(0);
        User user = getUserTestRealm(usernameTestRealm);
        assertNotNull(user);
        assertEquals(usernameTestRealm.toLowerCase(), user.getUsername());
        assertTrue(user.getAttributes().isEmpty());

        //"saml.persistent.name.id.for.http://localhost:8080/auth/realms/Test"
    }

    @Test
    public void testRequiredActionOneAttribute() throws IOException, JSONException {
        //Given
        String username = "User2";
        String userId = UUID.randomUUID().toString();
        List<String> attributeList = new ArrayList<>();
        attributeList.add("attribute1");
        String password = addUserAttributeRealm(username, attributeList, userId);
        WebClient webdriver = getWebDriver();

        //When..Then
        HtmlPage afterLogin = doLoginFlow(webdriver, username, password);
        assertFalse(isAttributeInputPage(afterLogin));

        // User is created and has correct attribute
        User userAttributeRealm = getUserAttributeRealm(userId);
        assertNotNull(userAttributeRealm);
        String usernameTestRealm = userAttributeRealm.getAttributes().get("saml.persistent.name.id.for.http://localhost:8080/auth/realms/Test").get(0);
        User user = getUserTestRealm(usernameTestRealm);
        assertNotNull(user);
        assertEquals(usernameTestRealm.toLowerCase(), user.getUsername());
        assertFalse(user.getAttributes().isEmpty());
        assertEquals(1, user.getAttributes().get("organisation").size());
        assertEquals("attribute1", user.getAttributes().get("organisation").get(0));

    }

    @Test
    public void testRequiredActionManyAttributes() throws IOException, JSONException {
        //Given
        String username = "User3";
        String userId = UUID.randomUUID().toString();
        List<String> attributeList = new ArrayList<>();
        attributeList.add("attribute1");
        attributeList.add("attribute2");
        attributeList.add("attribute3");
        attributeList.add("attribute4");

        String password = addUserAttributeRealm(username, attributeList, userId);
        WebClient webdriver = getWebDriver();

        //When..Then
        HtmlPage afterLogin = doLoginFlow(webdriver, username, password);
        assertTrue(isAttributeInputPage(afterLogin));

        HtmlPage afterAttributeChoice = setAttribute(afterLogin, "attribute2");
        assertFalse(isAttributeInputPage(afterAttributeChoice));

        // User is created and has correct attribute
        User userAttributeRealm = getUserAttributeRealm(userId);
        assertNotNull(userAttributeRealm);
        String usernameTestRealm = userAttributeRealm.getAttributes().get("saml.persistent.name.id.for.http://localhost:8080/auth/realms/Test").get(0);
        User user = getUserTestRealm(usernameTestRealm);
        assertNotNull(user);
        assertEquals(usernameTestRealm.toLowerCase(), user.getUsername());
        assertFalse(user.getAttributes().isEmpty());
        assertEquals(1, user.getAttributes().get("organisation").size());
        assertEquals("attribute2", user.getAttributes().get("organisation").get(0));

        webdriver = getWebDriver();
        // Login again and choose new attribute
        HtmlPage afterSecondLogin = doLoginFlow(webdriver, username, password);
        assertTrue(isAttributeInputPage(afterSecondLogin));

        HtmlPage afterAttributeChoiceSecondLogin = setAttribute(afterSecondLogin, "attribute4");
        assertFalse(isAttributeInputPage(afterAttributeChoiceSecondLogin));

        // User is created and has correct attribute
        User userSecondLogin = getUserTestRealm(usernameTestRealm);
        assertNotNull(userSecondLogin);
        assertEquals(usernameTestRealm.toLowerCase(), userSecondLogin.getUsername());
        assertFalse(userSecondLogin.getAttributes().isEmpty());
        assertEquals(1, userSecondLogin.getAttributes().get("organisation").size());
        assertEquals("attribute4", userSecondLogin.getAttributes().get("organisation").get(0));

    }


    public HtmlPage doLoginFlow(WebClient wc, String username, String password) throws IOException {
        // Access oauth2 proxy. It redirects to keycloak
        HtmlPage mainPage = wc.getPage(appendToProxyHostAndPort("/"));

        // Do login
        HtmlForm loginForm = mainPage.getForms().get(0);
        loginForm.getInputByName("username").setValueAttribute(username);
        loginForm.getInputByName("password").setValueAttribute(password);

        Page loginResult = loginForm.getButtonByName("login").click();

        if (loginResult.isHtmlPage()) {
            return (HtmlPage) loginResult;
        }
        throw new RuntimeException("Ikke en htmlside som resultat: " + loginResult.getWebResponse().getContentAsString());
    }

    public HtmlPage setAttribute(HtmlPage page, String attribute) throws IOException {
        HtmlForm attributeForm = page.getForms().get(0);
        attributeForm.getSelectByName("attribute").setSelectedAttribute(attribute, true);
        Page result = attributeForm.getInputByName("login").click();
        if (result.isHtmlPage()) {
            return (HtmlPage) result;
        }
        throw new RuntimeException("Ikke en htmlside som resultat: " + result.getWebResponse().getContentAsString());
    }

    public boolean isAttributeInputPage(HtmlPage page) {
        return page.getWebResponse().getContentAsString().contains("Hvilken organisation vil du benytte til login?");
    }
}
