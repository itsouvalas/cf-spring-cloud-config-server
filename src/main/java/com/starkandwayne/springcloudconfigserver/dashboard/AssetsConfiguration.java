package com.starkandwayne.springcloudconfigserver.dashboard;

import java.util.HashMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Order(0)
@Configuration
@Profile({"!test", "dashboardTest"})
public class AssetsConfiguration extends WebSecurityConfigurerAdapter {
   private final ApplicationContext applicationContext;
   private String ASSETS_URL_PREFIX = "/assets/**";

   public AssetsConfiguration(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
   }

   @Bean
   public HandlerMapping staticAssetsHandler() {
      SimpleUrlHandlerMapping resourceHandlerMapping = (SimpleUrlHandlerMapping)this.applicationContext.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
      HashMap<String, Object> mappings = new HashMap<String, Object>();
      resourceHandlerMapping.getUrlMap().forEach((url, handler) -> {
         mappings.put(this.ASSETS_URL_PREFIX, handler);
      });
      SimpleUrlHandlerMapping assetsHandlerMapping = new SimpleUrlHandlerMapping();
      assetsHandlerMapping.setUrlMap(mappings);
      assetsHandlerMapping.setOrder(Integer.MIN_VALUE);
      return assetsHandlerMapping;
   }

   protected void configure(HttpSecurity http) throws Exception {
      ((ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl)((HttpSecurity)http.antMatcher(this.ASSETS_URL_PREFIX).headers().cacheControl().disable().and()).authorizeRequests().anyRequest()).permitAll();
   }
}