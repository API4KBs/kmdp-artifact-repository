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

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import edu.mayo.kmdp.util.FileUtil;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JcrDaoTest {

  @TempDir
  Path tempDir;

  private String TYPE_NAME = JcrKnowledgeArtifactRepository.JcrTypes.KNOWLEDGE_ARTIFACT.name();

  private JcrDao dao;
  private Repository repo;

  @BeforeEach
  void repo() throws ConfigurationException {
    RepositoryConfig jcrConfig = RepositoryConfig.create(
        JcrKnowledgeArtifactRepositoryTest.class.getResourceAsStream("/test-repository.xml"),
        tempDir.toString());

    KnowledgeArtifactRepositoryServerConfig cfg =
        new KnowledgeArtifactRepositoryServerConfig().with(
            KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, "1");

    repo = new TransientRepository(jcrConfig);

    dao = new JcrDao(repo, Collections.singletonList(TYPE_NAME), cfg);
  }

  @AfterEach
  void cleanup() {
    TransientRepository jackrabbit = (TransientRepository) repo;
    jackrabbit.shutdown();
  }

  @Test
  void testLoadAndGet() {
    dao.saveResource(TYPE_NAME, "1", "1", "new", "hi!".getBytes());

    Version result = dao.getResource(TYPE_NAME, "1", "1", "new").getValue();

    assertEquals("hi!", d(result));
  }

  @Test
  void testLoadAndGetVersion() {
    dao.saveResource(TYPE_NAME, "1", "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "1", "new2", "hi2".getBytes());

    Version result1 = dao.getResource(TYPE_NAME, "1", "1", "new1").getValue();
    Version result2 = dao.getResource(TYPE_NAME, "1", "1", "new2").getValue();

    assertEquals("hi1", d(result1));
    assertEquals("hi2", d(result2));
  }

  @Test
  void testLoadAndGetVersions() {
    dao.saveResource(TYPE_NAME, "1", "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "1", "new2", "hi2".getBytes());

    List<Version> versions = dao.getResourceVersions(TYPE_NAME, "1", "1").getValue();

    assertEquals(2, versions.size());
  }

  @Test
  void testLoadAndGetLatestVersion() {
    dao.saveResource(TYPE_NAME, "1", "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "1", "new2", "hi2".getBytes());

    Version version = dao.getLatestResource(TYPE_NAME, "1", "1").getValue();

    assertEquals("hi2", d(version));
  }

  @Test
  void testLoadAndGetLatestVersionNone() {
    dao.saveResource(TYPE_NAME, "1", "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "1", "new2", "hi2".getBytes());

    assertThrows(
        ResourceNotFoundException.class,
        () -> dao.getLatestResource(TYPE_NAME, "1", "12345"));
  }

  @Test
  void testDelete() {
    dao.saveResource(TYPE_NAME, "1", "1", "new1", "hi1".getBytes());
    dao.saveResource(TYPE_NAME, "1", "1", "new2", "hi2".getBytes());

    dao.deleteResource(TYPE_NAME, "1","1", "new1");

    List<Version> versions = dao.getResourceVersions(TYPE_NAME, "1", "1").getValue();

    assertEquals(1, versions.size());
  }

  @Test
  void testQuery() {
    dao.saveResource(TYPE_NAME, "1", "1", "new1", "hi1".getBytes(), m("type", "foobar"));
    dao.saveResource(TYPE_NAME, "1", "1", "new1.1", "hi1.1".getBytes(), m("type", "foo"));

    dao.saveResource(TYPE_NAME, "1", "2", "new2", "hi2".getBytes(), m("type", "foobar"));
    dao.saveResource(TYPE_NAME, "1", "2", "new2.1", "hi2.1".getBytes(), m("type", "foo"));

    Map<String, String> query = new HashMap<>();
    query.put("type", "foo");

    List<Version> resources = dao.getResources(TYPE_NAME, "1", query).getValue();

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
