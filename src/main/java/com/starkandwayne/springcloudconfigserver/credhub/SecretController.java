package com.starkandwayne.springcloudconfigserver.credhub;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialRequest;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.json.JsonCredentialRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(
   name = {"scs.credhub.enabled"},
   matchIfMissing = true
)
public class SecretController {
   private static final Logger log = LoggerFactory.getLogger(SecretController.class);
   private final CredHubOperations credHubOperations;
   private final String credHubPath;

   SecretController(CredHubOperations credHubOperations, @Value("${scs.credhub.path}") String credHubPath) {
      this.credHubOperations = credHubOperations;
      if (credHubPath.startsWith("/")) {
         credHubPath = credHubPath.substring(1);
      }

      this.credHubPath = credHubPath;
   }

   @PutMapping({"/secrets/{app}/{profile}/{label}/{id}"})
   public void putSecret(@PathVariable String app, @PathVariable String profile, @PathVariable String label, @PathVariable String id, @RequestBody Map secret) {
      log.info(String.format("Creating a secret at: /%s/%s/%s/%s", app, profile, label, id));
      this.credHubOperations.credentials().write((CredentialRequest)((JsonCredentialRequest.JsonCredentialRequestBuilder)JsonCredentialRequest.builder().name(new SimpleCredentialName(new String[]{this.credHubPath, app, profile, label, id}))).value(secret).build());
   }

   @DeleteMapping({"/secrets/{app}/{profile}/{label}/{id}"})
   public void deleteSecret(@PathVariable String app, @PathVariable String profile, @PathVariable String label, @PathVariable String id) {
      log.info(String.format("Deleting a secret at: /%s/%s/%s/%s", app, profile, label, id));
      this.credHubOperations.credentials().deleteByName(new SimpleCredentialName(new String[]{this.credHubPath, app, profile, label, id}));
   }
}