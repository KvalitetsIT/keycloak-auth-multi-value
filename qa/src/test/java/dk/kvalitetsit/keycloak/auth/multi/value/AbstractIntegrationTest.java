package dk.kvalitetsit.keycloak.auth.multi.value;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import dk.kvalitetsit.keycloak.auth.multi.value.dto.Credential;
import dk.kvalitetsit.keycloak.auth.multi.value.dto.User;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class AbstractIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);
    private static final Logger keycloakLogger = LoggerFactory.getLogger("keycloak-logger");

    public static int keycloakPort;
    private static String keycloakHost;

    private static final RestTemplate restTemplate = new RestTemplate();

    private static String accessToken;
    private static int proxyPort;
    private static String proxyHost;

    @BeforeClass
    public static void setupTestEnvironment() throws JSONException, IOException, XmlPullParserException {
        Network n = Network.newNetwork();

        // Start Keycloak service
        var version = calculateKeycloakVersion();
        setupKeycloakContainer(n, version);
        setupProxyContainer(n);

        accessToken = getAccessToken();
    }

    private static void setupKeycloakContainer(Network n, String tag) {
        logger.info("Starting keycloak container version {}", tag);
        Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(8080), new ExposedPort(8080)));

        var image = new ImageFromDockerfile()
                .withFileFromFile("keycloak-auth-multi-value.jar", new File("../service/target/service.jar"))
                .withDockerfileFromBuilder(builder ->
                        builder.from("quay.io/keycloak/keycloak:" + tag)
                                .copy("keycloak-auth-multi-value.jar", "/opt/keycloak/providers/keycloak-auth-multi-value.jar")
                                .build());

        var keycloakContainer = new GenericContainer<>(image)
                .withClasspathResourceMapping("test-realm.json", "/opt/keycloak/data/import/test-realm.json", BindMode.READ_ONLY)
                .withClasspathResourceMapping("attribute-realm.json", "/opt/keycloak/data/import/attribute-realm.json", BindMode.READ_ONLY)
                .withCommand("start-dev", "--http-relative-path", "/auth", "--import-realm")
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "kit")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "Test1234")
                .withEnv("KEYCLOAK_LOGLEVEL", "DEBUG")
                .withEnv("required_action_choose_attribute_attribute_name", "organisation")

                .withNetwork(n)
                .withNetworkAliases("keycloak")
                .withExposedPorts(8080)
                .withCreateContainerCmdModifier(cmd)
                .waitingFor(Wait.forHttp("/auth").withStartupTimeout(Duration.ofMinutes(3)));

        keycloakContainer.start();

        logContainerOutput(keycloakContainer, keycloakLogger);
        keycloakPort = keycloakContainer.getMappedPort(8080);
        keycloakHost = keycloakContainer.getHost();
    }

    private static String calculateKeycloakVersion() throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("../pom.xml"));
        var properties = model.getProperties();
        return properties.getProperty("keycloak.version");
    }

    private static void setupProxyContainer(Network n) {
        GenericContainer<?> nginx = new GenericContainer<>("nginx")
                .withExposedPorts(80)
                .waitingFor(Wait.forHttp("/"))
                .withNetworkAliases("nginx")
                .withNetwork(n);
        nginx.start();

        GenericContainer<?> oauthProxy = new GenericContainer<>("quay.io/oauth2-proxy/oauth2-proxy:v7.6.0")
                .withNetwork(n)
                .withExposedPorts(4180)
                .withCommand(
                        "--provider=oidc",
                        "--client-id=oauth2-proxy",
                        "--client-secret=4AaKzgtdzIlsUyIQQKDYy6V08aozOpvE",
                        "--http-address=0.0.0.0:4180",
                        "--insecure-oidc-allow-unverified-email=true",
                        "--session-store-type=cookie",
                        "--upstream=http://nginx:80",
                        "--skip-provider-button=true",
                        "--skip-auth-preflight",
                        "--errors-to-info-log",
                        "--cookie-name=test",
                        "--skip-oidc-discovery",
                        "--redeem-url=http://keycloak:8080/auth/realms/Test/protocol/openid-connect/token",
                        "--login-url=http://localhost:8080/auth/realms/Test/protocol/openid-connect/auth",
                        "--oidc-jwks-url=http://keycloak:8080/auth/realms/Test/protocol/openid-connect/certs",
                        "--skip-oidc-discovery=true",
                        "--cookie-secure=false",

                        "--oidc-issuer-url=http://localhost:8080/auth/realms/Test",
                        "--cookie-secret=ThisSecretShouldBeThisLong_12345",
                        "--email-domain=*")
                .waitingFor(Wait.forListeningPort());
        oauthProxy.start();
        proxyPort = oauthProxy.getMappedPort(4180);
        proxyHost = oauthProxy.getHost();
    }

    private static void logContainerOutput(GenericContainer<?> container, Logger logger) {
        logger.info("Attaching logger to container: {}", container.getContainerInfo().getName());
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        container.followOutput(logConsumer);
    }

    public WebClient getWebDriver() {
        WebClient wc = new WebClient();
        WebClientOptions options = wc.getOptions();
        options.setJavaScriptEnabled(true);
        options.setRedirectEnabled(true);
        options.setCssEnabled(false);
        wc.waitForBackgroundJavaScriptStartingBefore(60000);
        return wc;
    }

    public static String addUserAttributeRealm(String userName, List<String> attributeList, String userId) {
        // User to create
        String password = "testtest";
        User user = new User();
        user.setUsername(userName);
        user.setEnabled(true);
        Credential credential = new Credential();
        credential.setType("password");
        credential.setValue(password);
        credential.setTemporary(false);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail(String.format("%s@example.com", UUID.randomUUID()));
        user.getCredentials().add(credential);
        user.getAttributes().put("USER_ID", Collections.singletonList(userId));
        if (!attributeList.isEmpty()) {
            // key skal stemme overens med required_action_choose_attribute_attribute_name
            user.getAttributes().put("organisation", attributeList);
        }

        // Do create user
        HttpHeaders userheaders = getKeycloakApiHeaders(accessToken);
        HttpEntity<User> requestUser = new HttpEntity<>(user, userheaders);
        ResponseEntity<String> result = restTemplate.postForEntity(appendToKeycloakHostAndPort("/auth/admin/realms/Attribute/users"), requestUser, String.class);
        if (HttpStatus.CREATED != result.getStatusCode()) {
            throw new RuntimeException("User not created");
        }

        return password;
    }

    public User getUserTestRealm(String userName) throws JSONException  {

        // Auth
        String accessToken = getAccessToken();

        // Find user
        HttpHeaders userheaders = getKeycloakApiHeaders(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(userheaders);
        ResponseEntity<User[]> result = restTemplate.exchange(appendToKeycloakHostAndPort("/auth/admin/realms/Test/users?username="+userName), HttpMethod.GET, requestEntity, User[].class);
        if (HttpStatus.OK != result.getStatusCode()) {
            throw new RuntimeException("Userquery failed");
        }

        if (result.getBody() != null && result.getBody().length == 1) {
            return result.getBody()[0];
        }

        return null;
    }

    public User getUserAttributeRealm(String username) {
        HttpHeaders userHeaders = getKeycloakApiHeaders(accessToken);
        HttpEntity<Void> headers = new HttpEntity<>(userHeaders);

        ResponseEntity<User[]> result = restTemplate.exchange(appendToKeycloakHostAndPort("/auth/admin/realms/Attribute/users"), HttpMethod.GET, headers, User[].class);
        if (HttpStatus.OK != result.getStatusCode()) {
            throw new RuntimeException("User not created");
        }

        return Arrays.stream(Objects.requireNonNull(result.getBody()))
                .filter(x -> x.getAttributes().get("USER_ID").contains(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public static HttpHeaders getKeycloakApiHeaders(String accessToken) {
        HttpHeaders keycloakHeaders = new HttpHeaders();
        keycloakHeaders.setContentType(MediaType.APPLICATION_JSON);
        keycloakHeaders.set("Authorization", "bearer " + accessToken);

        return keycloakHeaders;
    }

    private static String getAccessToken() throws JSONException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", "kit");
        body.add("password", "Test1234");
        body.add("grant_type", "password");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(appendToKeycloakHostAndPort("/auth/realms/master/protocol/openid-connect/token"), request, String.class);
        String authBody = response.getBody();
        JSONObject authJson = new JSONObject(authBody);

        return authJson.getString("access_token");
    }

    public static String appendToKeycloakHostAndPort(String url) {
        return "http://" + keycloakHost + ":" + keycloakPort + url;
    }

    public static String appendToProxyHostAndPort(String url) {
        return "http://" + proxyHost + ":" + proxyPort + url;
    }
}
