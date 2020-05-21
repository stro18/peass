package de.peass.measurement.rca;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.PeASSFilter;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.DurationFilter;
import de.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.monitoring.writer.filesystem.AggregatedDataReader;
import kieker.monitoring.writer.filesystem.aggregateddata.AggregatedData;
import kieker.monitoring.writer.filesystem.aggregateddata.AggregatedDataNode;
import kieker.tools.trace.analysis.filter.executionRecordTransformation.ExecutionRecordTransformationFilter;

public class KiekerResultReader {

   private static final Logger LOG = LogManager.getLogger(KiekerResultReader.class);

   final boolean useAggregation;
   final Set<CallTreeNode> includedNodes;
   final String version;
   final File versionResultFolder;
   final TestCase testcase;
   final boolean otherVersion;

   boolean considerNodePosition = false;

   public KiekerResultReader(final boolean useAggregation, final Set<CallTreeNode> includedNodes, final String version,
         final File versionResultFolder, final TestCase testcase, final boolean otherVersion) {
      this.useAggregation = useAggregation;
      this.includedNodes = includedNodes;
      this.version = version;
      this.versionResultFolder = versionResultFolder;
      this.testcase = testcase;
      this.otherVersion = otherVersion;
   }

   public void setConsiderNodePosition(final boolean considerNodePosition) {
      this.considerNodePosition = considerNodePosition;
   }

   public void readResults() {
      try {
         LOG.info("Reading kieker results");
         for (final File kiekerResultFolder : versionResultFolder.listFiles((FilenameFilter) new RegexFileFilter("[0-9]*"))) {
            final File kiekerTraceFile = KiekerFolderUtil.getKiekerTraceFolder(kiekerResultFolder, testcase);
            if (useAggregation) {
               readAggregatedData(kiekerTraceFile);
            } else {
               readNonAggregated(kiekerTraceFile);
            }
         }
      } catch (IllegalStateException | AnalysisConfigurationException | IOException e) {
         e.printStackTrace();
      }
   }

   public void readAggregatedData(final File kiekerTraceFolder) throws JsonParseException, JsonMappingException, IOException {
      final Map<AggregatedDataNode, AggregatedData> fullDataMap = AggregatedDataReader.getFullDataMap(kiekerTraceFolder);
      for (final CallTreeNode node : includedNodes) {
         boolean nodeFound = false;
         final CallTreeNode examinedNode = otherVersion ? node.getOtherVersionNode() : node;
         final String nodeCall = KiekerPatternConverter.fixParameters(examinedNode.getKiekerPattern());
         final List<StatisticalSummary> values = new LinkedList<>();
         // final SummaryStatistics statistics = new SummaryStatistics();
         for (final Entry<AggregatedDataNode, AggregatedData> entry : fullDataMap.entrySet()) {
            if (isSameNode(examinedNode, nodeCall, entry.getKey())) {
               for (final StatisticalSummary dataSlice : entry.getValue().getStatistic().values()) {
                  values.add(dataSlice);
               }
               nodeFound = true;
            }
         }

         if (nodeFound) {
            node.setMeasurement(version, values);
         } else {
            LOG.warn("Node {} ({}) did not find measurement values", nodeCall, node.getOtherVersionNode());
         }
      }
   }

   private boolean isSameNode(final CallTreeNode node, final String nodeCall, final AggregatedDataNode measuredNode) {
      final String kiekerCall = KiekerPatternConverter.getKiekerPattern(measuredNode.getCall());
      if (nodeCall.equals(kiekerCall)) {
         System.out.println("Test: " + kiekerCall + " " + nodeCall + " " + node.getEoi() + " " + measuredNode.getEoi());
         if (considerNodePosition) {
            final int eoi = node.getEoi();
            if (measuredNode.getEoi() == eoi) {
               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public void readNonAggregated(final File kiekerTraceFile) throws AnalysisConfigurationException {
      final KiekerReader reader = new KiekerReader(kiekerTraceFile);
      reader.initBasic();
      final AnalysisController analysisController = reader.getAnalysisController();

      final DurationFilter kopemeFilter = new DurationFilter(includedNodes, analysisController, version);
      analysisController.connect(reader.getExecutionFilter(), ExecutionRecordTransformationFilter.OUTPUT_PORT_NAME_EXECUTIONS,
            kopemeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

      analysisController.run();
   }
}
