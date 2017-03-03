package hex.genmodel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MojoReaderBackendFactory {

  public enum CachingStrategy { MEMORY, DISK }

  public static MojoReaderBackend createReaderBackend(String filename) throws IOException {
    return createReaderBackend(new File(filename));
  }

  public static MojoReaderBackend createReaderBackend(File file) throws IOException {
    if (file.isFile())
      return new ZipfileMojoReaderBackend(file.getPath());
    else if (file.isDirectory())
      return new FolderMojoReaderBackend(file.getPath());
    else
      throw new IOException("Invalid file specification: " + file);
  }

  public static MojoReaderBackend createReaderBackend(URL url, CachingStrategy cachingStrategy) throws IOException {
    try (InputStream is = url.openStream()) {
      return createReaderBackend(is, cachingStrategy);
    }
  }

  public static MojoReaderBackend createReaderBackend(InputStream inputStream, CachingStrategy cachingStrategy) throws IOException {
    switch (cachingStrategy) {
      case MEMORY:
        return createInMemoryReaderBackend(inputStream);
      case DISK:
        return createTempFileReaderBackend(inputStream);
    }
    throw new IllegalStateException("Unexpected caching strategy: " + cachingStrategy);
  }

  private static MojoReaderBackend createInMemoryReaderBackend(InputStream inputStream) throws IOException {
    ZipInputStream zis = new ZipInputStream(inputStream);
    HashMap<String, byte[]> content = new HashMap<>();
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      if (entry.getSize() > Integer.MAX_VALUE)
        throw new IOException("File too large: " + entry.getName());
      int off = 0;
      int len = (int) entry.getSize();
      byte[] data = new byte[len];
      while (off < len) {
        int available = Math.min(zis.available(), len - off);
        off += zis.read(data, off, available);
      }
      content.put(entry.getName(), data);
    }
    return new InMemoryMojoReaderBackend(content);
  }

  private static MojoReaderBackend createTempFileReaderBackend(InputStream inputStream) throws IOException {
    Path tmp = Files.createTempFile("h2o-mojo", ".zip");
    Files.copy(inputStream, tmp, StandardCopyOption.REPLACE_EXISTING);
    File tmpFile = tmp.toFile();
    tmpFile.deleteOnExit(); // register delete on exit hook (in case tmp reader doesn't do the job)
    return new TmpMojoReaderBackend(tmpFile);
  }

}
