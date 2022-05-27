package com.starkandwayne.springcloudconfigserver.env;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.credhub.configuration.CredHubTemplateFactory;
import org.springframework.credhub.core.CredHubException;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.core.CredHubProperties;
import org.springframework.credhub.support.ClientOptions;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.json.JsonCredential;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;


public class CredHubPropertiesPostProcessor implements EnvironmentPostProcessor {
   private CredHubOperations credHubTemplate;

   void setCredHubTemplate(CredHubOperations credHubTemplate) {
      this.credHubTemplate = credHubTemplate;
      System.out.println("Loaded Credhub PostProcessor");
   }

   public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
      String runtimeCredHubUrl = environment.getProperty("credentials.credhub-url");
      if (StringUtils.isEmpty(runtimeCredHubUrl)) {
         System.out.println("credentials.credhub-url is not set, skipping CredHub properties loading");
      } else {
         String credhubRef = environment.getProperty("credentials.credhub-ref");
         if (StringUtils.isEmpty(credhubRef)) {
            System.out.println("credentials.credhub-ref is not set, skipping CredHub properties loading");
         } else {
            CredHubOperations credHubOperations = this.credHubTemplate == null ? this.buildMtlsCredHubOperations(runtimeCredHubUrl) : this.credHubTemplate;
            System.out.println(String.format("Loading properties from CredHub %s, using credentials-ref %s", runtimeCredHubUrl, credhubRef));
            credhubRef = credhubRef.replace("((", "").replace("))", "");

            JsonCredential value;
            try {
               value = (JsonCredential)credHubOperations.credentials().getByName(new SimpleCredentialName(new String[]{credhubRef}), JsonCredential.class).getValue();
            } catch (CredHubException var8) {
               if (var8.getStatusCode() == HttpStatus.NOT_FOUND) {
                  System.err.println(String.format("%s credential not found in CredHub", credhubRef));
                  return;
               }

               throw var8;
            } catch (ResourceAccessException var9) {
               System.err.println("Can't get credentials from CredHub");
               var9.printStackTrace(System.err);
               return;
            }

            MapPropertySource credhubPropertySource = new MapPropertySource("credhub", value);
            environment.getPropertySources().addLast(credhubPropertySource);
            System.out.println(String.format("Registered %s properties from %s", value.size(), credhubRef));
         }
      }
   }

   private CredHubOperations buildMtlsCredHubOperations(String runtimeCredHubUrl) {
      CredHubProperties credHubProperties = new CredHubProperties();
      credHubProperties.setUrl(runtimeCredHubUrl);
      CredHubTemplateFactory credHubTemplateFactory = new CredHubTemplateFactory();
      return credHubTemplateFactory.credHubTemplate(credHubProperties, new ClientOptions());
   }
}
