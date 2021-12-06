package de.dagere.peass.jmh;

import java.io.File;

public class MyJmhTestConstants {

   public static File JMH_EXAMPLE_FOLDER = new File("src/test/resources/jmh-it");
   
   public static File MULTIPARAM_VERSION = new File(MyJmhTestConstants.JMH_EXAMPLE_FOLDER, "multi-param-version");
   public static File MULTIPARAM_VERSION_CHANGE = new File(MyJmhTestConstants.JMH_EXAMPLE_FOLDER, "multi-param-version-change");
}
