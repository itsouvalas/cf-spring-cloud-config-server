package com.starkandwayne.springcloudconfigserver.credhub;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.json.JsonCredential;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile({"!dev"})
@ConditionalOnProperty(
   name = {"scs.credhub.enabled"},
   matchIfMissing = true
)
public class ScsCredhubEnvironmentRepository implements EnvironmentRepository {
   private static final Logger log = LoggerFactory.getLogger(ScsCredhubEnvironmentRepository.class);
   private final CredHubOperations credHubOperations;
   private final String credHubPath;

   public ScsCredhubEnvironmentRepository(CredHubOperations credHubOperations, @Value("${scs.credhub.path}") String credHubPath) {
      this.credHubOperations = credHubOperations;
      if (credHubPath.startsWith("/")) {
         credHubPath = credHubPath.substring(1);
      }
      log.info("Loadeding CredHub Environment Path {} {}", new Object[]{credHubPath, credHubOperations.info().toString()});
      this.credHubPath = credHubPath;
   }

   public Environment findOne(String application, String profilesList, String label) {
    log.info("loading environment");
      if (StringUtils.isEmpty(profilesList)) {
         profilesList = "default";
      }

      if (StringUtils.isEmpty(label)) {
         label = "master";
      }

      String[] profiles = StringUtils.commaDelimitedListToStringArray(profilesList);
      Environment environment = new Environment(application, profiles, label, (String)null, (String)null);
      String[] var6 = profiles;
      int var7 = profiles.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         String profile = var6[var8];
         Map<String, Object> properties = this.findProperties(application, profile, label);
         environment.add(new PropertySource("credhub-" + application + "-" + profile + "-" + label, properties));
         log.info("Loaded {} properties for {}/{}/{}", new Object[]{properties.size(), application, profile, label});
         if (!"application".equals(application)) {
            this.addDefaultPropertySource(environment, "application", profile, label);
         }
      }

      if (!Arrays.asList(profiles).contains("default")) {
         this.addDefaultPropertySource(environment, application, "default", label);
      }

      if (!Arrays.asList(profiles).contains("default") && !"application".equals(application)) {
         this.addDefaultPropertySource(environment, "application", "default", label);
      }

      return environment;
   }

   private void addDefaultPropertySource(Environment environment, String application, String profile, String label) {
      Map<String, Object> properties = this.findProperties(application, profile, label);
      if (!properties.isEmpty()) {
         log.info("Loaded {} properties for {}/{}/{}", new Object[]{properties.size(), application, profile, label});
         PropertySource propertySource = new PropertySource("credhub-" + application + "-" + profile + "-" + label, properties);
         environment.add(propertySource);
      }

   }

   private Map<String, Object> findProperties(String application, String profile, String label) {
      String path = "/" + this.credHubPath + "/" + application + "/" + profile + "/" + label;
      return (Map<String, Object>)this.credHubOperations.credentials().findByPath(path).stream().map((credentialSummary) -> {
         return credentialSummary.getName().getName();
      }).map((name) -> {
         return this.credHubOperations.credentials().getByName(new SimpleCredentialName(new String[]{name}), JsonCredential.class);
      }).map(CredentialDetails::getValue).flatMap((jsonCredential) -> {
         return jsonCredential.entrySet().stream();
      }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
         return b;
      }));
   }
}