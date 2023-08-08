package com.starkandwayne.springcloudconfigserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController

public class PropertiesController {


    @Autowired
    private Environment env;

    @GetMapping("/properties")
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        String[] keys = {
                "server.forward-headers-strategy",
                "server.port",
                "spring.profiles.active",
                "spring.cloud.config.server.credhub.url",
                "spring.cloud.config.server.health.enabled",
                "spring.credhub.url",
                "spring.credhub.path",
                "spring.credhub.oauth2.registration-id",
                "spring.security.oauth2.client.registration.uaa.provider",
                "spring.security.oauth2.client.registration.uaa.client-id",
                "spring.security.oauth2.client.registration.uaa.client-secret",
                "spring.security.oauth2.client.registration.uaa.authorization-grant-type",
                "spring.security.oauth2.client.registration.uaa.redirect-uri",
                "spring.security.oauth2.client.registration.uaa.authentication-method",
                "spring.security.oauth2.client.registration.uaa.scope",
                "spring.security.oauth2.client.provider.uaa.token-uri",
                "spring.security.oauth2.client.provider.uaa.authorization-uri",
                "spring.security.oauth2.client.provider.uaa.jwk-set-uri",
                "spring.security.oauth2.client.provider.uaa.user-info-uri",
                "spring.security.oauth2.client.provider.uaa.user-name-attribute",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                "spring.mvc.pathmatch.use-suffix-pattern",
                "spring.mvc.pathmatch.use-registered-suffix-pattern",
                "uaa.uri",
                "uaa.token-uri",
                "uaa.jwk-set-uri",
                "management.endpoint.health.show-details",
                "management.endpoints.web.exposure.include",
                "scs.credhub.path",
                "scs.config-server.authorization-uri",
                "scs.config-server.service-instance-id"

        };
        Arrays.stream(keys).forEach(key -> properties.put(key, env.getProperty(key)));
        return properties;
    }

}
