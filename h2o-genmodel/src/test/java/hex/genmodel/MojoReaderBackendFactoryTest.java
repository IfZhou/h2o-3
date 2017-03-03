package hex.genmodel;

import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;
import static hex.genmodel.MojoReaderBackendFactory.*;
public class MojoReaderBackendFactoryTest {

  @Test
  public void testCreateReaderBackend_URL_Memory() throws Exception {
    URL dumjo = MojoReaderBackendFactoryTest.class.getResource("dumjo.zip");
    assertNotNull(dumjo);
    MojoReaderBackend r = createReaderBackend(dumjo, CachingStrategy.MEMORY);
    assertTrue(r instanceof InMemoryMojoReaderBackend);
    try {
      assertTrue(r.exists("binary-file"));
      assertTrue(r.exists("text-file"));
      assertEquals("line1", r.getTextFile("text-file").readLine());
    } finally {
      ((Closeable) r).close();
    }
  }

  @Test
  public void testCreateReaderBackend_URL_Disk() throws Exception {
    URL dumjo = MojoReaderBackendFactoryTest.class.getResource("dumjo.zip");
    assertNotNull(dumjo);
    MojoReaderBackend r = createReaderBackend(dumjo, CachingStrategy.DISK);
    assertTrue(r instanceof TmpMojoReaderBackend);
    File tempFile = ((TmpMojoReaderBackend) r)._tempZipFile;
    assertTrue(tempFile.exists());
    try {
      assertTrue(r.exists("binary-file"));
      assertTrue(r.exists("text-file"));
      assertEquals("line1", r.getTextFile("text-file").readLine());
    } finally {
      ((Closeable) r).close();
    }
    assertFalse(tempFile.exists());
  }

}