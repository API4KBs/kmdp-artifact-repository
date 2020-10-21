/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.repository.artifact.jcr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.repository.artifact.jcr.JcrDao.DaoResult;
import edu.mayo.kmdp.util.FileUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JcrDaoTest {

  private JcrDao dao;

  private UUID artifactUUID;
  private UUID artifactUUID2;

  @BeforeEach
  void repo() {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    dao = new JcrDao(jcr);
    artifactUUID = UUID.randomUUID();
    artifactUUID2 = UUID.randomUUID();
  }

  @AfterEach
  void cleanup() {
    dao.shutdown();
  }

  @Test
  void testClear() {
    String repoId = UUID.fromString("4cb66719-4440-4b98-8966-79a0de213475").toString();

    dao.saveResource(repoId, artifactUUID, "new", "hi!".getBytes());

    DaoResult<List<Node>> nodes = dao.getResources(repoId, false, new HashMap<>());

    assertEquals(1, nodes.getValue().size());

    DaoResult<Version> result = dao.getResource(repoId, artifactUUID, "new", false);

    assertEquals("hi!", d(result.getValue()));

    result.getSession().logout();

    dao.clear();

    UUID randomUuid = UUID.randomUUID();

    dao.saveResource(repoId, randomUuid, "something", "new node".getBytes());

    nodes = dao.getResources(repoId, false, new HashMap<>());

    assertEquals(1, nodes.getValue().size());

    nodes.getSession().logout();

    DaoResult<Version> result2 = dao.getResource(repoId, randomUuid, "something", false);
    assertEquals("new node", d(result2.getValue()));

    result2.getSession().logout();
  }

  @Test
  void testLoadAndGet() {
    dao.saveResource("1", artifactUUID, "new", "hi!".getBytes());

    Version result = dao.getResource("1", artifactUUID, "new", false).getValue();

    assertEquals("hi!", d(result));
  }

  @Test
  void testLoadAndGetHasAvailableStatus() throws Exception {
    dao.saveResource("1", artifactUUID, "new", "hi!".getBytes());

    Node result = dao.getResource("1", artifactUUID, "new", false).getValue().getFrozenNode();

    assertEquals("available", result.getProperty("status").getString());
  }

  @Test
  void testLoadAndGetVersion() {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource("1", artifactUUID, "new2", "hi2".getBytes());

    Version result1 = dao.getResource("1", artifactUUID, "new1", false).getValue();
    Version result2 = dao.getResource("1", artifactUUID, "new2", false).getValue();

    assertEquals("hi1", d(result1));
    assertEquals("hi2", d(result2));
  }

  @Test
  void testLoadAndGetVersions() {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource("1", artifactUUID, "new2", "hi2".getBytes());

    List<Version> versions = dao.getResourceVersions("1", artifactUUID, false).getValue();

    assertEquals(2, versions.size());
  }

  @Test
  void testLoadAndGetLatestVersion() {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource("1", artifactUUID, "new2", "hi2".getBytes());

    Version version = dao.getLatestResource("1", artifactUUID, false).getValue();

    assertEquals("hi2", d(version));
  }

  @Test
  void testLoadAndGetLatestVersionNone() {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource("1", artifactUUID, "new2", "hi2".getBytes());

    assertThrows(
        ResourceNotFoundException.class,
        () -> dao.getLatestResource("1", artifactUUID2, false));
  }

  @Test
  void testDeleteTagsVersionUnavailable() throws Exception {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource("1", artifactUUID, "new2", "hi2".getBytes());

    dao.deleteResource("1", artifactUUID, "new1");

    Node version = dao.getResource("1", artifactUUID, "new1", true).getValue().getFrozenNode();
    Node version2 = dao.getResource("1", artifactUUID, "new2", true).getValue().getFrozenNode();

    assertEquals("unavailable", version.getProperty("status").getString());
    assertEquals("available", version2.getProperty("status").getString());
  }

  @Test
  void testDeleteTagsArtifactsAndVersionsUnavailable() throws Exception {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource("1", artifactUUID, "new2", "hi2".getBytes());
    dao.saveResource("1", artifactUUID, "new3", "hi2".getBytes());
    dao.saveResource("1", artifactUUID, "new4", "hi2".getBytes());

    dao.deleteResource("1", artifactUUID);

    Node version = dao.getResource("1", artifactUUID, "new1", true).getValue().getFrozenNode();
    Node version2 = dao.getResource("1", artifactUUID, "new2", true).getValue().getFrozenNode();

    assertEquals("unavailable", version.getProperty("status").getString());
    assertEquals("unavailable", version2.getProperty("status").getString());
  }

  @Test
  void testQueryAll() {
    dao.saveResource("default", UUID.randomUUID(), "new1", "hi1".getBytes(), m("type", "foobar"));
    dao.saveResource("default", UUID.randomUUID(), "new1.1", "hi1.1".getBytes(), m("type", "foo"));

    dao.saveResource("default", UUID.randomUUID(), "new2", "hi2".getBytes(), m("type", "foobar"));
    dao.saveResource("default", UUID.randomUUID(), "new2.1", "hi2.1".getBytes(), m("type", "foo"));

    Map<String, String> query = new HashMap<>();

    List<Node> resources = dao.getResources("default", false, query).getValue();

    assertEquals(4, resources.size());
  }

  @Test
  void testQueryProperty() throws Exception{
    dao.saveResource("default", UUID.randomUUID(), "new1", "hi1".getBytes(), m("type", "a"));
    dao.saveResource("default", UUID.randomUUID(), "new1.1", "hi1.1".getBytes(), m("type", "b"));

    UUID id = UUID.randomUUID();
    dao.saveResource("default", id, "new2", "hi2".getBytes(), m("type", "c"));
    dao.saveResource("default", UUID.randomUUID(), "new2.1", "hi2.1".getBytes(), m("type", "d"));

    Map<String, String> query = new HashMap<>();
    query.put("type", "c");

    List<Node> resources = dao.getResources("default", false, query).getValue();

    assertEquals(1, resources.size());

    assertEquals(id.toString(), resources.get(0).getName());
  }


  @Test
  void testQuery() {
    dao.saveResource("default", artifactUUID, "new1", "hi1".getBytes(), m("type", "foobar"));
    dao.saveResource("default", artifactUUID, "new1.1", "hi1.1".getBytes(), m("type", "foo"));

    dao.saveResource("default", artifactUUID2, "new2", "hi2".getBytes(), m("type", "foobar"));
    dao.saveResource("default", artifactUUID2, "new2.1", "hi2.1".getBytes(), m("type", "foo"));

    Map<String, String> query = new HashMap<>();
    query.put("type", "foo");

    List<Node> resources = dao.getResources("default", false, query).getValue();

    assertEquals(2, resources.size());
  }

  @Test
  void testQueryWithNumbers() {
    dao.saveResource("1", artifactUUID, "new1", "hi1".getBytes(), m("type", "foobar"));
    dao.saveResource("1", artifactUUID, "new1.1", "hi1.1".getBytes(), m("type", "foo"));

    dao.saveResource("1", artifactUUID2, "new2", "hi2".getBytes(), m("type", "foobar"));
    dao.saveResource("1", artifactUUID2, "new2.1", "hi2.1".getBytes(), m("type", "foo"));

    Map<String, String> query = new HashMap<>();
    query.put("type", "foo");

    List<Node> resources = dao.getResources("1", false, query).getValue();

    assertEquals(2, resources.size());
  }


  private Map<String, String> m(String k, String v) {
    Map<String, String> m = new HashMap<>();
    m.put(k, v);
    return m;
  }

  String d(Version v) {
    try {
      return FileUtil.read(v.getFrozenNode().getProperty("jcr:data").getBinary().getStream())
          .orElse("");
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }


}
