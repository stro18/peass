package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.utils.StreamGobbler;
import de.dagere.peass.vcs.VersionControlSystem;

public enum ContinuousFolderUtil {
   ;

   private static final Logger LOG = LogManager.getLogger(ContinuousFolderUtil.class);

   public static File getLocalFolder(final File projectFolder) {
      return new File(projectFolder, ".." + File.separator + projectFolder.getName() + "_fullPeass");
   }

   public static String getSubFolderPath(final File projectFolder) throws IOException {
      File vcsFolder = VersionControlSystem.findVCSFolder(projectFolder);
      if (vcsFolder != null) {
         String projectCanonicalPath = projectFolder.getCanonicalPath();
         String vcsCanonicalPath = vcsFolder.getCanonicalPath();
         if (projectCanonicalPath.length() > vcsCanonicalPath.length()) {
            String localSuffix = projectCanonicalPath.substring(vcsCanonicalPath.length() + 1);
            return vcsFolder.getName() + File.separator + localSuffix;
         } else {
            return vcsFolder.getName();
         }
      } else {
         return null;
      }
   }

   public static void cloneProject(final File cloneProjectFolder, final File localFolder) throws InterruptedException, IOException {
      localFolder.mkdirs();
      File originalVcsFolder = VersionControlSystem.findVCSFolder(cloneProjectFolder);
      if (originalVcsFolder != null && originalVcsFolder.exists()) {
         LOG.info("Cloning using git clone");
         final ProcessBuilder builder = new ProcessBuilder("git", "clone", originalVcsFolder.getAbsolutePath());
         builder.directory(localFolder);
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
         assureProcessFinished(process);
         LOG.debug("Exit code: {}", process.exitValue());
      } else {
         throw new RuntimeException("No git folder in " + cloneProjectFolder.getAbsolutePath() + " (or parent) present - "
               + "currently, only git projects are supported");
      }
   }

   private static void assureProcessFinished(Process process) throws InterruptedException {
      while (process.isAlive()) {
         LOG.debug("Process is still alive, while it should be finished");
         Thread.sleep(10);
      }
   }

}
