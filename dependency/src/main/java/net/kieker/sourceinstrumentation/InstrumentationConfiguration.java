package net.kieker.sourceinstrumentation;

import java.util.HashSet;
import java.util.Set;

public class InstrumentationConfiguration {
   private final AllowedKiekerRecord usedRecord;
   private final boolean sample;
   private final int samplingCount;
   private final boolean enableDeactivation;
   private final boolean createDefaultConstructor;
   private final boolean enableAdaptiveMonitoring;
   private final Set<String> includedPatterns;
   private final Set<String> excludedPatterns;
   private final boolean extractMethod;

   /**
    * Simple constructor, setting default values for everything except usedRecord, sample and includedPatterns
    */
   public InstrumentationConfiguration(final AllowedKiekerRecord usedRecord, final boolean sample,
         final Set<String> includedPatterns, final boolean enableAdaptiveMonitoring, final boolean enableDecativation, final int samplingCount, final boolean extractMethod) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.includedPatterns = includedPatterns;
      excludedPatterns = new HashSet<String>();
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
      this.createDefaultConstructor = true;
      this.enableDeactivation = enableDecativation;
      this.samplingCount = samplingCount;
      this.extractMethod = extractMethod;

      check();
   }

   public InstrumentationConfiguration(final AllowedKiekerRecord usedRecord, final boolean sample,
         final boolean createDefaultConstructor, final boolean enableAdaptiveMonitoring,
         final Set<String> includedPatterns, final boolean enableDecativation, final int samplingCount, final boolean extractMethod) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.createDefaultConstructor = createDefaultConstructor;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
      this.includedPatterns = includedPatterns;
      excludedPatterns = new HashSet<String>();
      this.enableDeactivation = enableDecativation;
      this.samplingCount = samplingCount;
      this.extractMethod = extractMethod;

      check();
   }

   public InstrumentationConfiguration(final AllowedKiekerRecord usedRecord, final boolean sample,
         final boolean createDefaultConstructor, final boolean enableAdaptiveMonitoring,
         final Set<String> includedPatterns, final Set<String> excludedPatterns, final boolean enableDecativation, final int samplingCount, final boolean extractMethod) {
      this.usedRecord = usedRecord;
      this.sample = sample;
      this.createDefaultConstructor = createDefaultConstructor;
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
      this.includedPatterns = includedPatterns;
      this.excludedPatterns = excludedPatterns;
      this.enableDeactivation = enableDecativation;
      this.samplingCount = samplingCount;
      this.extractMethod = extractMethod;

      check();
   }

   private void check() {
      if (sample && usedRecord == AllowedKiekerRecord.OPERATIONEXECUTION) {
         throw new RuntimeException("Sampling + OperationExecutionRecord does not make sense, since OperationExecutionRecord contains too complex metadata for sampling");
      }
      if (!enableDeactivation && extractMethod) {
         throw new RuntimeException("Disabling deactivation and extracting methods does not make sense, since it only slows down the process");
      }
   }

   public AllowedKiekerRecord getUsedRecord() {
      return usedRecord;
   }

   public boolean isSample() {
      return sample;
   }

   public int getSamplingCount() {
      return samplingCount;
   }

   public boolean isCreateDefaultConstructor() {
      return createDefaultConstructor;
   }

   public Set<String> getIncludedPatterns() {
      return includedPatterns;
   }

   public Set<String> getExcludedPatterns() {
      return excludedPatterns;
   }

   public boolean isEnableAdaptiveMonitoring() {
      return enableAdaptiveMonitoring;
   }

   public boolean isEnableDeactivation() {
      return enableDeactivation;
   }

}