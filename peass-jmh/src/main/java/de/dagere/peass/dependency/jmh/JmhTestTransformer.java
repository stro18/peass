package de.dagere.peass.dependency.jmh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.dagere.kopeme.parsing.JUnitParseUtil;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.WorkloadType;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzFinder;
import de.dagere.peass.dependency.changesreading.JavaParserProvider;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.TestTransformer;

public class JmhTestTransformer implements TestTransformer {

   private static final Logger LOG = LogManager.getLogger(JmhTestTransformer.class);

   private final File projectFolder;
   private final MeasurementConfig measurementConfig;
   private boolean ignoreEOIs;

   public JmhTestTransformer(final File projectFolder, final MeasurementConfig measurementConfig) {
      this.projectFolder = projectFolder;
      this.measurementConfig = measurementConfig;
      if (!measurementConfig.getExecutionConfig().getTestExecutor().equals(WorkloadType.JMH.getTestExecutor())) {
         throw new RuntimeException("Test Executor needs to be " + WorkloadType.JMH.getTestExecutor());
      }
   }

   public JmhTestTransformer(final File projectFolder, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig) {
      this.projectFolder = projectFolder;
      measurementConfig = new MeasurementConfig(1, executionConfig, kiekerConfig);
      measurementConfig.setIterations(1);
      measurementConfig.setWarmup(0);
      measurementConfig.setUseKieker(true);
   }

   @Override
   public MeasurementConfig getConfig() {
      return measurementConfig;
   }

   @Override
   public TestSet buildTestMethodSet(final TestSet testsToUpdate, final List<File> modules) {
      final TestSet tests = new TestSet();
      for (final TestCase clazzname : testsToUpdate.getClasses()) {
         final Set<String> currentClazzMethods = testsToUpdate.getMethods(clazzname);
         if (currentClazzMethods == null || currentClazzMethods.isEmpty()) {
            final File moduleFolder = new File(projectFolder, clazzname.getModule());
            final List<TestCase> methods = getTestMethodNames(moduleFolder, clazzname);
            for (final TestCase test : methods) {
               addTestIfIncluded(tests, test);
            }
         } else {
            for (final String method : currentClazzMethods) {
               TestCase test = new TestCase(clazzname.getClazz(), method, clazzname.getModule());
               tests.addTest(test);
            }
         }
      }
      return tests;
   }

   // TODO includedModules is currenctly ignored for jmh!
   @Override
   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final ProjectModules modules) {
      TestSet allBenchmarks = new TestSet();
      try {
         for (File module : modules.getModules()) {
            TestSet moduleTests = findModuleTests(mapping, includedModules, module);
            allBenchmarks.addTestSet(moduleTests);
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException("File was not found, can not handle this error", e);
      }
      return allBenchmarks;
   }

   public TestSet findModuleTests(final ModuleClassMapping mapping, final List<String> includedModules, final File module) throws FileNotFoundException {
      TestSet moduleTests = new TestSet();
      ClazzFileFinder finder = new ClazzFileFinder(measurementConfig.getExecutionConfig());
      for (final String clazz : finder.getClasses(module)) {
         String currentModule = ModuleClassMapping.getModuleName(projectFolder, module);
         final List<TestCase> testMethodNames = getTestMethodNames(module, new TestCase(clazz, null, currentModule));
         for (TestCase test : testMethodNames) {
            if (includedModules == null || includedModules.contains(test.getModule())) {
               addTestIfIncluded(moduleTests, test);
            }
         }
      }
      return moduleTests;
   }

   @Override
   public boolean isJUnit3() {
      return false;
   }

   @Override
   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }

   @Override
   public List<TestCase> getTestMethodNames(final File module, final TestCase clazzname) {
      final List<TestCase> methods = new LinkedList<>();
      ClazzFileFinder finder = new ClazzFileFinder(measurementConfig.getExecutionConfig());
      final File clazzFile = finder.getClazzFile(module, clazzname);
      try {
         // File might be removed or moved
         if (clazzFile != null) {
            LOG.debug("Parsing {} - {}", clazzFile, clazzname);
            final CompilationUnit unit = JavaParserProvider.parse(clazzFile);
            List<ClassOrInterfaceDeclaration> clazzDeclarations = ClazzFinder.getClazzDeclarations(unit);
            for (ClassOrInterfaceDeclaration clazz : clazzDeclarations) {
               String parsedClassName = getFullName(clazz);
               LOG.trace("Clazz: {} - {}", parsedClassName, clazzname.getShortClazz());
               if (parsedClassName.equals(clazzname.getShortClazz())) {
                  List<String> benchmarkMethods = JUnitParseUtil.getAnnotatedMethods(clazz, "org.openjdk.jmh.annotations.Benchmark", "Benchmark");
                  for (String benchmarkMethod : benchmarkMethods) {
                     TestCase foundBenchmark = new TestCase(clazzname.getClazz(), benchmarkMethod, clazzname.getModule());
                     methods.add(foundBenchmark);
                  }
               }
            }
         }
      } catch (FileNotFoundException e) {
         throw new RuntimeException(e);
      }
      return methods;
   }

   private String getFullName(final ClassOrInterfaceDeclaration clazz) {
      String parsedClassName = clazz.getNameAsString();
      boolean hasClassParent = clazz.getParentNode().isPresent() && clazz.getParentNode().get() instanceof ClassOrInterfaceDeclaration;
      ClassOrInterfaceDeclaration parent = hasClassParent ? (ClassOrInterfaceDeclaration) clazz.getParentNode().get() : null;
      while (parent != null) {
         parsedClassName = parent.getNameAsString() + ChangedEntity.CLAZZ_SEPARATOR + parsedClassName;
         hasClassParent = parent.getParentNode().isPresent() && parent.getParentNode().get() instanceof ClassOrInterfaceDeclaration;
         parent = hasClassParent ? (ClassOrInterfaceDeclaration) parent.getParentNode().get() : null;
      }
      return parsedClassName;
   }

   private void addTestIfIncluded(final TestSet moduleTests, final TestCase test) {
      if (NonIncludedTestRemover.isTestIncluded(test, getConfig().getExecutionConfig())) {
         moduleTests.addTest(test);
      }
   }

   @Override
   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   @Override
   public void determineVersions(final List<File> modules) {
      // not required for JmhTestTransformer, since inheritance is not considered and therefore no re-reading of files is required (and therefore no cache)
   }

}
