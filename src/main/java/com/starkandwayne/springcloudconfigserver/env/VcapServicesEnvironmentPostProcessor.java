package com.starkandwayne.springcloudconfigserver.env;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.util.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

public class VcapServicesEnvironmentPostProcessor implements EnvironmentPostProcessor {
   private static final Logger log = LoggerFactory.getLogger(VcapServicesEnvironmentPostProcessor.class);
   private static final String VCAP_MIRROR_CREDENTIALS_PREFIX = "vcap.services.mirror-svc.credentials";
   private static final String CONFIG_SERVER_PREFIX = "spring.cloud.config.server";
   private static final String PROPERTY_SOURCE_NAME = "SCS3";
   private static final String BASE_DIR = "/home/vcap/app";

   public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

      MutablePropertySources propertySources = environment.getPropertySources();
      System.out.println("Loaded VCAP PostProcessor");
      System.out.println(propertySources.toString());
      String vcapKey = "vcap";
      if (!propertySources.contains("vcap")) {
         log.warn("VCAP environment not found, is service instance a cloud foundry application?");
      } else {

        org.springframework.core.env.PropertySource<?> confPropSource = propertySources.get("vcap");
        Map confMap = Map.class.cast(confPropSource.getSource());
        Map<String, Object> configMap = (Map<String, Object>) confMap.entrySet().stream().filter( e -> 
             ((String)Entry.class.cast(e).getKey()).startsWith("vcap.services.mirror-svc.credentials")
        ).collect(Collectors.groupingBy(Map.Entry<String, Object>::getKey, Collectors.mapping(Map.Entry<String,Object>::getValue, Collectors.toList())));

         if (this.isStaticGitConfiguration(configMap)) {
            log.info("Adding active profile: git");
            environment.addActiveProfile("git");
            this.addGitPropertiesForStaticGitConfiguration(configMap);
         }

         if (this.isStaticVaultConfiguration(configMap)) {
            log.info("Adding active profile: vault");
            environment.addActiveProfile("vault");
         }

         if (this.isCompositeConfiguration(configMap)) {
            log.info("Adding active profile: composite");
            environment.addActiveProfile("composite");
            this.addGitPropertiesForAllCompositeRepos(configMap);
            configMap.remove("spring.cloud.config.server.composite");
         }

         if (this.isEncryptionEnabled(configMap)) {
            configMap.put("encrypt.key", configMap.get("spring.cloud.config.server.encrypt.key"));
            configMap.remove("spring.cloud.config.server.encrypt.key");
         }

         if (this.emptyConfig(configMap)) {
            log.info("Adding active profile: native");
            environment.addActiveProfile("native");
         }

         log.info("Adding property source with keys: " + configMap.keySet().toString());
         propertySources.addFirst(new MapPropertySource("SCS3", configMap));
      }
   }

   private boolean isEncryptionEnabled(Map configMap) {
      return configMap.containsKey("spring.cloud.config.server.encrypt.key");
   }

   private boolean emptyConfig(Map configMap) {
      return !this.isCompositeConfiguration(configMap) && !this.isStaticGitConfiguration(configMap) && !this.isStaticVaultConfiguration(configMap);
   }

   private void addGitPropertiesForStaticGitConfiguration(Map configMap) {
      this.addGitProperties(configMap, "git", "default");
   }

   private void addGitPropertiesForAllCompositeRepos(Map configMap) {
      long compositeCount = configMap.keySet().stream().filter((key) -> {
         return ((String) key).endsWith("type");
      }).count();

      for(int i = 0; (long)i < compositeCount; ++i) {
         String compositeEntry = String.format("composite[%d]", i);
         if ("git".equals(configMap.get(String.format("%s.%s.type", "spring.cloud.config.server", compositeEntry)))) {
            this.addGitProperties(configMap, compositeEntry, i + "-default");
         }
      }

   }

   private void addGitProperties(Map configMap, String path, String baseDirSuffix) {
      configMap.put(String.format("%s.%s.ignoreLocalSshSettings", "spring.cloud.config.server", path), true);
      configMap.put(String.format("%s.%s.baseDir", "spring.cloud.config.server", path), "/home/vcap/app/config-repo-" + baseDirSuffix);
      String labelKey = String.format("%s.%s.label", "spring.cloud.config.server", path);
      if (configMap.containsKey(labelKey)) {
         configMap.put(String.format("%s.%s.defaultLabel", "spring.cloud.config.server", path), configMap.get(labelKey));
      }

      String periodicKey = String.format("%s.%s.periodic", "spring.cloud.config.server", path);
      if (configMap.containsKey(periodicKey)) {
         configMap.put(periodicKey, Boolean.valueOf(configMap.get(periodicKey).toString()));
      }

   }

   private boolean isStaticVaultConfiguration(Map configMap) {
      return configMap.containsKey("spring.cloud.config.server.vault.host");
   }

   private boolean isCompositeConfiguration(Map configMap) {
      return configMap.containsKey("spring.cloud.config.server.composite[0].type");
   }

   private boolean isStaticGitConfiguration(Map configMap) {
      return configMap.containsKey("spring.cloud.config.server.git.uri");
   }
}