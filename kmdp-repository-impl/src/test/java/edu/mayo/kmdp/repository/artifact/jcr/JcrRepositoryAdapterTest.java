package edu.mayo.kmdp.repository.artifact.jcr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.Repository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class JcrRepositoryAdapterTest {

  private String TYPE_NAME = JcrRepositoryAdapter.JcrTypes.ARTIFACT.name();

  @Test
  void testLoadAndGetArtifacts() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("1", 0, -1).getBody();

    assertEquals(1, result.size());
  }

  @Test
  void testLoadAndGetArtifactsUUID() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "urn:oid:2.16.840.1.113883.3.117.1.7.1.202", "LATEST",
        "hi!".getBytes());

    ResponseEntity<byte[]> result = adapter
        .getKnowledgeArtifactVersion("1", "urn:oid:2.16.840.1.113883.3.117.1.7.1.202", "LATEST");

    assertEquals("hi!", new String(result.getBody()));
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHref()
      throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("1",0, -1).getBody();

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/1/artifacts/1/versions/new",
        result.get(0).getHref().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultiple()
      throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("1",0, -1).getBody();

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/1/artifacts/1/versions/new2",
        result.get(0).getHref().toString());
  }

  @Test
  void testLoadAndDelete() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi!".getBytes());

    adapter.deleteKnowledgeArtifactVersion("1", "1", "new");

    List<Pointer> result = adapter.getKnowledgeArtifactSeries("1", "1",0, -1, null, null, null ).getBody();

    assertEquals(1, result.size());
  }

  @Test
  void testLoadAndDeleteSeries() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi!".getBytes());

    adapter.deleteKnowledgeArtifactSeries("1", "1");

    HttpStatus result = adapter.getKnowledgeArtifactSeries("1", "1",0, -1, null, null, null).getStatusCode();

    assertEquals(404, result.value());
  }

  @Test
  void testLoadTwice() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());

    ResponseEntity<List<Pointer>> result = adapter.getKnowledgeArtifactSeries("1", "1",0, -1, null, null, null);

    assertEquals(1, result.getBody().size());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultipleArtifacts()
      throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    JcrRepositoryAdapter adapter = new JcrRepositoryAdapter(dao);

    dao.saveResource(TYPE_NAME, "1", "new", "hi!".getBytes());
    dao.saveResource(TYPE_NAME, "1", "new2", "hi!".getBytes());

    dao.saveResource(TYPE_NAME, "2", "new", "hi!".getBytes());
    dao.saveResource(TYPE_NAME, "2", "new2", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("1", 0, -1 ).getBody();

    assertEquals(2, result.size());

    Set<String> resultSet = result.stream().map(it -> it.getHref().toString())
        .collect(Collectors.toSet());

    assertTrue(resultSet.contains("http://localhost:8080/repos/1/artifacts/2/versions/new2"));
    assertTrue(resultSet.contains("http://localhost:8080/repos/1/artifacts/1/versions/new2"));
  }

}
