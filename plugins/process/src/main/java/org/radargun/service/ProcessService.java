package org.radargun.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.radargun.Service;
import org.radargun.config.Converter;
import org.radargun.config.Property;
import org.radargun.config.TimeConverter;
import org.radargun.logging.Log;
import org.radargun.logging.LogFactory;
import org.radargun.traits.ProvidesTrait;
import org.radargun.utils.Tokenizer;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@Service(doc = "Generic process control")
public class ProcessService {

   @Property(doc = "Command for starting the process")
   protected String command;

   @Property(doc = "Configuration file used as the last argument.")
   protected String file;

   @Property(doc = "Additional arguments. Empty by default.", converter = ArgsConverter.class)
   protected List<String> args = Collections.emptyList();

   @Property(doc = "Environment arguments. Empty by default.", converter = EnvsConverter.class)
   protected Map<String,String> env = Collections.emptyMap();

   @Property(doc = "Current operating system. Default is UNIX.")
   protected String os = "unix";

   @Property(doc = "Timeout to start the server. Default is 1 minute.", converter = TimeConverter.class)
   public long startTimeout = 60000;

   @ProvidesTrait
   public ProcessLifecycle createLifecycle() {
      return new ProcessLifecycle(this);
   }

   protected List<String> getCommand() {
      ArrayList<String> command = new ArrayList<String>(args.size() + 2);
      command.add(this.command);
      command.addAll(args);
      if (file != null) {
         command.add(file);
      }
      return command;
   }

   public Map<String, String> getEnvironment() {
      return env;
   }

   private static class ArgsConverter implements Converter<List<String>> {
      private static Log log = LogFactory.getLog(ArgsConverter.class);

      @Override
      public List<String> convert(String string, Type type) {
         ArrayList<String> list = new ArrayList<String>();
         Tokenizer tokenizer = new Tokenizer(string, new String[] { " ", "\t", "\n", "\r", "\f", "'" }, true, false, 0);
         StringBuilder sb = null;
         while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.charAt(0) == '\'') {
               if (sb == null) { // non-quoted
                  sb = new StringBuilder().append(token);
               } else { // quoted
                  sb.append("'");
                  list.add(sb.toString());
                  sb = null;
               }
            } else if (Character.isWhitespace(token.charAt(0)) && token.length() == 1) {
               if (sb != null) {
                  sb.append(token);
               }
            } else {
               if (sb == null) {
                  list.add(token);
               } else {
                  sb.append(token);
               }
            }
         }
         if (sb != null) {
            log.warn("Args are not closed: " + string);
            sb.append('\'');
            list.add(sb.toString());
         }
         return list;
      }

      @Override
      public String convertToString(List<String> value) {
         StringBuilder sb = new StringBuilder();
         for (String arg : value) {
            sb.append(arg).append(' ');
         }
         return sb.toString();
      }

      @Override
      public String allowedPattern(Type type) {
         return ".*";
      }
   }

   private static class EnvsConverter implements Converter<Map<String, String>> {
      private static Log log = LogFactory.getLog(EnvsConverter.class);

      @Override
      public Map<String, String> convert(String string, Type type) {
         Map<String, String> env = new TreeMap<String, String>();
         String[] lines = string.split("\n");
         for (String line : lines) {
            int eqIndex = line.indexOf('=');
            if (eqIndex < 0) {
               if (line.trim().length() > 0) {
                  log.warn("Cannot parse env " + line);
               }
            } else {
               env.put(line.substring(0, eqIndex).trim(), line.substring(eqIndex + 1).trim());
            }
         }
         return env;
      }

      @Override
      public String convertToString(Map<String, String> value) {
         StringBuilder sb = new StringBuilder();
         for (Map.Entry<String, String> envVar : value.entrySet()) {
            sb.append(envVar.getKey()).append('=').append(envVar.getValue()).append('\n');
         }
         return sb.toString();
      }

      @Override
      public String allowedPattern(Type type) {
         return ".*";
      }
   }
}
