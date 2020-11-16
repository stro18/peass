package de.peass.measurement.rca;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.peass.dependency.analysis.data.TestCase;
import picocli.CommandLine.Option;

public class CauseSearcherConfigMixin {

   @Option(names = { "-useCalibrationRun", "--useCalibrationRun" }, description = "Use the calibration run for complete measurements")
   private boolean useCalibrationRun = false;

   @Option(names = { "-useNonAggregatedWriter",
         "--useNonAggregatedWriter" }, description = "Whether to save non-aggregated JSON data for measurement results - if true, full kieker record data are stored")
   private boolean useNonAggregatedWriter = false;

   @Option(names = { "-saveKieker", "--saveKieker" }, description = "Save no kieker results in order to use less space - default false")
   private boolean saveNothing = false;

   @Option(names = { "-useEOIs",
         "--useEOIs" }, description = "Use EOIs - nodes will be considered different if their kieker pattern or ess differ (needs space and computation time for big trees)")
   private boolean useEOIs = false;

   @Option(names = { "-notSplitAggregated", "--notSplitAggregated" }, description = "Whether to split the aggregated data (produces aggregated data per time slice)")
   private boolean notSplitAggregated = false;

   @Option(names = { "-outlierFactor", "--outlierFactor" }, description = "Whether outliers should be removed with z-score higher than the given value")
   private double outlierFactor = 5.0;

   @Option(names = { "-minTime",
         "--minTime" }, description = "Minimum node difference time compared to relative standard deviation. "
               + "If a node takes less time, its childs won't be measured (since time measurement isn't below accurate below a certain value).")
   private double minTime = 0.1;

   public boolean isUseCalibrationRun() {
      return useCalibrationRun;
   }

   public boolean isUseNonAggregatedWriter() {
      return useNonAggregatedWriter;
   }

   public boolean isSaveNothing() {
      return saveNothing;
   }

   public boolean isUseEOIs() {
      return useEOIs;
   }

   public boolean isNotSplitAggregated() {
      return notSplitAggregated;
   }

   public double getOutlierFactor() {
      return outlierFactor;
   }

   public double getMinTime() {
      return minTime;
   }

}