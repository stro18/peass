package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.RequiredDependency;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class GradleBuildfileEditor {

   private static final Logger LOG = LogManager.getLogger(GradleBuildfileEditor.class);

   private final JUnitTestTransformer testTransformer;
   private final File buildfile;
   private final ProjectModules modules;

   public GradleBuildfileEditor(final JUnitTestTransformer testTransformer, final File buildfile, final ProjectModules modules) {
      this.testTransformer = testTransformer;
      this.buildfile = buildfile;
      this.modules = modules;
   }

   public GradleBuildfileVisitor addDependencies(final File tempFolder) {
      GradleBuildfileVisitor visitor = null;
      try {
         LOG.debug("Editing buildfile: {}", buildfile.getAbsolutePath());
         visitor = GradleParseUtil.parseBuildfile(buildfile, testTransformer.getConfig().getExecutionConfig());
         if (visitor.isUseJava() == true) {
            editGradlefileContents(tempFolder, visitor);
         } else {
            LOG.debug("Buildfile itself does not contain Java plugin, checking parent projects");
            boolean isUseJava = isParentUseJava(buildfile, modules);
            if (isUseJava) {
               editGradlefileContents(tempFolder, visitor);
            } else {
               LOG.info("Parent buildfile did not contain java; not changing buildfile");
            }
         }

         LOG.debug("Writing changed buildfile: {}", buildfile.getAbsolutePath());
         Files.write(buildfile.toPath(), visitor.getLines(), StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   private boolean isParentUseJava(final File buildfile, final ProjectModules modules) throws IOException, FileNotFoundException {
      List<File> parentProjects = modules.getParents(buildfile.getParentFile());
      boolean isUseJava = false;
      for (File parentProject : parentProjects) {
         File parentBuildfile = GradleParseHelper.findGradleFile(parentProject);
         LOG.debug("Reading " + parentBuildfile);
         GradleBuildfileVisitor parentVisitor = GradleParseUtil.parseBuildfile(parentBuildfile, testTransformer.getConfig().getExecutionConfig());
         if (parentVisitor.isSubprojectJava()) {
            isUseJava = true;
         }
      }
      return isUseJava;
   }

   private void editGradlefileContents(final File tempFolder, final GradleBuildfileVisitor visitor) {
      if (visitor.getBuildTools() != -1) {
         GradleParseUtil.updateBuildTools(visitor);
      }

      if (visitor.getBuildToolsVersion() != -1) {
         GradleParseUtil.updateBuildToolsVersion(visitor);
      }

      if (visitor.isUseSpringBoot()) {
         LOG.info("Adding spring boot ext");
         GradleParseUtil.addJUnitVersionSpringBoot(visitor);
      } else {
         LOG.info("Did not find spring boot");
      }

      GradleParseUtil.removeExclusions(visitor);

      addDependencies(visitor);

      addKiekerLine(tempFolder, visitor);
   }

   private void addDependencies(final GradleBuildfileVisitor visitor) {
      boolean isAddJunit3 = testTransformer.isJUnit3();
      boolean isExcludeLog4j = testTransformer.getConfig().getExecutionConfig().isExcludeLog4jSlf4jImpl();
      if (visitor.getDependencyLine() != -1) {
         for (RequiredDependency dependency : RequiredDependency.getAll(isAddJunit3)) {
            final String dependencyGradle;
            if (isExcludeLog4j && dependency.getMavenDependency().getArtifactId().contains("kopeme")) {
               String excludeString = "{ exclude group: '" + MavenPomUtil.LOG4J_GROUPID + "', module: '" + MavenPomUtil.LOG4J_SLF4J_IMPL_ARTIFACTID + "' }";
               dependencyGradle = "implementation ('" + dependency.getGradleDependency() + "') " + excludeString;
            } else {
               dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            }
            visitor.addLine(visitor.getDependencyLine() - 1, dependencyGradle);
         }
      } else {
         visitor.getLines().add("dependencies { ");
         for (RequiredDependency dependency : RequiredDependency.getAll(isAddJunit3)) {
            final String dependencyGradle = "implementation '" + dependency.getGradleDependency() + "'";
            visitor.getLines().add(dependencyGradle);
         }
         visitor.getLines().add("}");
      }

      if (testTransformer.getConfig().getExecutionConfig().isExcludeLog4jToSlf4j() && visitor.getAllConfigurationsLine() != -1) {
         String excludeString = "exclude group: '" + MavenPomUtil.LOG4J_GROUPID + "', module: '" + MavenPomUtil.LOG4J_TO_SLF4J_ARTIFACTID + "'";
         visitor.addLine(visitor.getAllConfigurationsLine() + 1, excludeString);
      }
   }

   public void addKiekerLine(final File tempFolder, final GradleBuildfileVisitor visitor) {
      ArgLineBuilder argLineBuilder = new ArgLineBuilder(testTransformer, buildfile.getParentFile());
      addArgLine(visitor, argLineBuilder, tempFolder);
   }

   private void addArgLine(final GradleBuildfileVisitor visitor, final ArgLineBuilder argLineBuilder, File tempFolder) {
      if (visitor.getAndroidLine() != -1) {
         if (visitor.getUnitTestsAll() != -1) {
            visitor.addLine(visitor.getUnitTestsAll() - 1, argLineBuilder.buildArglineGradle(tempFolder));
         } else if (visitor.getTestOptionsAndroid() != -1) {
            visitor.addLine(visitor.getTestOptionsAndroid() - 1, "unitTests.all{" + argLineBuilder.buildArglineGradle(tempFolder) + "}");
         } else {
            visitor.addLine(visitor.getAndroidLine() - 1, "testOptions{ unitTests.all{" + argLineBuilder.buildArglineGradle(tempFolder) + "} }");
         }
      } else {
         if (visitor.getTestLine() != -1) {
            if (visitor.getTestSystemPropertiesLine() == -1) {
               visitor.addLine(visitor.getTestLine() - 1, argLineBuilder.buildArglineGradle(tempFolder));
            } else {
               for (Map.Entry<String, String> entry : argLineBuilder.getGradleSystemProperties(tempFolder).entrySet()) {
                  visitor.addLine(visitor.getTestSystemPropertiesLine(), "  '" + entry.getKey() + "'             : '" + entry.getValue() + "',");
               }
               if (argLineBuilder.getJVMArgs() != null) {
                  visitor.addLine(visitor.getIntegrationTestLine(), argLineBuilder.getJVMArgs());
               }

            }

         } else {
            visitor.getLines().add("test { " + argLineBuilder.buildArglineGradle(tempFolder) + "}");
         }
      }
      if (visitor.getIntegrationTestLine() != -1) {
         if (visitor.getIntegrationTestSystemPropertiesLine() == -1) {
            visitor.addLine(visitor.getIntegrationTestLine() - 1, argLineBuilder.buildArglineGradle(tempFolder));
         } else {
            for (Map.Entry<String, String> entry : argLineBuilder.getGradleSystemProperties(tempFolder).entrySet()) {
               visitor.addLine(visitor.getIntegrationTestSystemPropertiesLine(), "  '" + entry.getKey() + "'             : '" + entry.getValue() + "',");
            }
            if (argLineBuilder.getJVMArgs() != null) {
               visitor.addLine(visitor.getIntegrationTestLine(), argLineBuilder.getJVMArgs());
            }
         }

      }
   }
}
