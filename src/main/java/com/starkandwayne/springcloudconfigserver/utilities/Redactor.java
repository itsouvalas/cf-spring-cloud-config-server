package com.starkandwayne.springcloudconfigserver.utilities;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Redactor {
   private static final Set<String> keysToRedact = new HashSet<String>();

   public static Object redact(Object object) {
      if (object instanceof Map) {
         Map<String, Object> mapObject = (Map<String, Object>)object;
         Stream mapstream = mapObject.entrySet().stream();

         return (mapstream
         .collect(Collectors.toMap(Map.Entry<Object, Object>::getKey, (e) -> {
            return keysToRedact.contains(e.getKey()) ? "||REDACTED||" : redact(e.getValue());
         })));
      } else {
         return object instanceof List ? ((List)object).stream().map(Redactor::redact).collect(Collectors.toList()) : object;
      }
   }

   static {
      keysToRedact.add("privateKey");
      keysToRedact.add("sourcePrivateKey");
      keysToRedact.add("username");
      keysToRedact.add("sourceUsername");
      keysToRedact.add("password");
      keysToRedact.add("sourcePassword");
      keysToRedact.add("defaultKey");
      keysToRedact.add("client_secret");
      keysToRedact.add("key");
   }
}
