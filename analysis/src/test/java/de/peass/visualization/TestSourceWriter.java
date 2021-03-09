package de.peass.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.peass.analysis.properties.TestMethodChangeReader;

public class TestSourceWriter {

   public static final File methodSourceFolder = new File("target" + File.separator + "current_sources");
   
   @Before
   public void init() throws FileNotFoundException, IOException {
      TestMethodChangeReader.writeConstructor(new File("../dependency/"), TestMethodChangeReader.methodSourceFolder);
      TestMethodChangeReader.writeInit(new File("../dependency/"), TestMethodChangeReader.methodSourceFolder);
   }

   @Test
   public void testInitWriter() throws IOException {
      GraphNode root = new GraphNode("de.peass.analysis.properties.TestMethodChangeReader#init", 
            "public void de.peass.analysis.properties.TestMethodChangeReader.init()",
            "public void de.peass.analysis.properties.TestMethodChangeReader.init()");
      BufferedWriter mockedWriter = Mockito.mock(BufferedWriter.class);
      SourceWriter writer = new SourceWriter(root, mockedWriter, TestMethodChangeReader.methodSourceFolder, TestMethodChangeReader.VERSION);

      writer.writeSources();

      ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

      Mockito.verify(mockedWriter, Mockito.atLeastOnce()).write(argument.capture());

      validateInitSource(argument);
   }

   private void validateInitSource(final ArgumentCaptor<String> argument) {
      String writtenJavascript = StringUtils.join(argument.getAllValues());
      System.out.println(writtenJavascript);
      Assert.assertThat(writtenJavascript, Matchers.not(Matchers.containsString("System.out")));
      Assert.assertThat(writtenJavascript, Matchers.containsString("System.err"));
   }
   
   @Test
   public void testConstructorWriter() throws IOException {
      GraphNode root = new GraphNode("de.peass.analysis.properties.TestMethodChangeReader#<init>", 
            "public new de.peass.analysis.properties.TestMethodChangeReader.<init>()",
            "public new de.peass.analysis.properties.TestMethodChangeReader.<init>()");
      BufferedWriter mockedWriter = Mockito.mock(BufferedWriter.class);
      SourceWriter writer = new SourceWriter(root, mockedWriter, TestMethodChangeReader.methodSourceFolder, TestMethodChangeReader.VERSION);

      writer.writeSources();

      ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

      Mockito.verify(mockedWriter, Mockito.atLeastOnce()).write(argument.capture());

      validateConstructorSource(argument);
   }

   private void validateConstructorSource(final ArgumentCaptor<String> argument) {
      String writtenJavascript = StringUtils.join(argument.getAllValues());
      System.out.println(writtenJavascript);
      Assert.assertThat(writtenJavascript, Matchers.containsString("System.out"));
      Assert.assertThat(writtenJavascript, Matchers.not(Matchers.containsString("System.err")));
   }
}