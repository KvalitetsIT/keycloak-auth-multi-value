# keycloak-auth-multi-value

This is a Keycloak Plugin that adds a required action during login. The required action is activated if a user has more
than one attribute of the configured attribute name, and the user is then required to choose under which attribute value 
they wish to log in.

## Installation
The plugin is build as a Docker container containing only a single jar file named `/module/keycloak-auth-multi-value.jar`. 
To install the plugin copy the file to `/opt/keycloak/providers/keycloak-auth-multi-value.jar` in your Keycloak installation.

## Configuration
After installation a new required action `REQUIRED_ACTION_CHOOSE` is installed in Keycloak. This should be enabled in 
the Keycloak realm under Authentication.

Further, the Keycloak installation should be given the environment variable
* `required_action_choose_attribute_attribute_name`: The name of the attribute the user should choose from. This should 
match the attribute name received from the IdP.

## Releasing
To support various Keycloak versions the version of the plugin tells which version the plugin have been tested against 
and the version of the plugin. For instance version `1.0.0-20.0.5` says that the version have been tested against Keycloak 
version 20.0.5 and the plugin is version `1.0.0`. Version tags are prefixed with `v`.