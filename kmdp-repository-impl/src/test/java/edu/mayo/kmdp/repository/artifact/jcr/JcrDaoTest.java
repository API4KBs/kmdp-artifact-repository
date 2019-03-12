package edu.mayo.kmdp.repository.artifact.jcr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.Test;

class JcrDaoTest {

  String TYPE_NAME = JcrRepositoryAdapter.JcrTypes.ARTIFACT.name();

  protected String d(Version v) {
    try {
      return IOUtils.toString(v.getFrozenNode().getProperty("jcr:data").getBinary().getStream());
    } catch (IOException | RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testLoadAndGet() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());

    Version result = dao.getResource(TYPE_NAME, "1", "new").getValue();

    assertEquals("hi!", d(result));
  }

  @Test
  void testLoadAndGetVersion() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi2".getBytes());

    Version result1 = dao.getResource(TYPE_NAME, "1", "new1").getValue();
    Version result2 = dao.getResource(TYPE_NAME, "1", "new2").getValue();

    assertEquals("hi1", d(result1));
    assertEquals("hi2", d(result2));
  }

  @Test
  void testLoadAndGetVersions() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi2".getBytes());

    Optional<List<Version>> versions = dao.getResourceVersions(TYPE_NAME, "1").getValue();

    assertEquals(2, versions.get().size());
  }

  @Test
  void testLoadAndGetLatestVersion() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi2".getBytes());

    Version version = dao.getLatestResource(TYPE_NAME, "1").getValue();

    assertEquals("hi2", d(version));
  }

  @Test
  void testLoadAndGetLatestVersionNone() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi2".getBytes());

    assertThrows(
        ResourceNotFoundException.class,
        () -> dao.getLatestResource(TYPE_NAME, "12345").getValue());
  }

  @Test
  void testDelete() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi2".getBytes());

    dao.deleteResource(TYPE_NAME, "1", "new1");

    Optional<List<Version>> versions = dao.getResourceVersions(TYPE_NAME, "1").getValue();

    assertEquals(1, versions.get().size());
  }

  @Test
  void testQuery() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    dao.saveResource(TYPE_NAME, "1", "new1", "hi1".getBytes(), m("type", "foobar"));
    dao.saveResource(TYPE_NAME, "1", "new1.1", "hi1.1".getBytes(), m("type", "foo"));

    dao.saveResource(TYPE_NAME, "2", "new2", "hi2".getBytes(), m("type", "foobar"));
    dao.saveResource(TYPE_NAME, "2", "new2.1", "hi2.1".getBytes(), m("type", "foo"));

    Map<String, String> query = new HashMap<>();
    query.put("type", "foo");

    List<Version> resources = dao.getResources(TYPE_NAME, query).getValue();

    assertEquals(2, resources.size());
  }

  private Map<String, String> m(String k, String v) {
    Map<String, String> m = new HashMap<>();
    m.put(k, v);
    return m;
  }

}
