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
package edu.mayo.kmdp.repository.artifact;

import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.mayo.kmdp.repository.artifact.jcr.JcrDao;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import java.nio.file.Path;
import java.util.List;
import javax.jcr.Repository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class KnowledgeArtifactRepositoryTest {

  private static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository repo;

  private static KnowledgeArtifactRepositoryServerConfig cfg =
      new KnowledgeArtifactRepositoryServerConfig()
          .with(DEFAULT_REPOSITORY_NAME, "TestRepository")
          .with(DEFAULT_REPOSITORY_ID, "TestRepo");

  @BeforeAll
  static void repo() {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr);
    repo = new JcrKnowledgeArtifactRepository(dao, cfg);
  }

  @AfterAll
  static void cleanup() {
    JcrKnowledgeArtifactRepository jackrabbit = (JcrKnowledgeArtifactRepository) repo;
    jackrabbit.shutdown();
  }

  @Test
  void testListRepositoryWithDefault() {
    ResponseEntity<List<KnowledgeArtifactRepository>> ans = repo
        .listKnowledgeArtifactRepositories();
    assertEquals(HttpStatus.NOT_IMPLEMENTED, ans.getStatusCode());
  }

  @Test
  void testSetRepositoryWithDefault() {
    ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> ans = repo
        .setKnowledgeArtifactRepository("repository", new KnowledgeArtifactRepository());
    assertEquals(HttpStatus.NOT_IMPLEMENTED, ans.getStatusCode());
  }

  @Test
  void testInitRepositoryWithDefault() {
    ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> ans = repo
        .initKnowledgeArtifactRepository();
    assertEquals(HttpStatus.NOT_IMPLEMENTED, ans.getStatusCode());
  }

  @Test
  void testIsRepositoryWithDefault() {
    ResponseEntity<Void> ans = repo
        .isKnowledgeArtifactRepository("repository");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, ans.getStatusCode());
  }

  @Test
  void testGetRepositoryWithNonDefault() {
    ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> ans = repo
        .getKnowledgeArtifactRepository("repository");
    assertEquals(HttpStatus.NOT_FOUND, ans.getStatusCode());
  }

  @Test
  void testGetRepositoryWithDefault() {
    ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> ans = repo
        .getKnowledgeArtifactRepository("TestRepo");
    assertEquals(HttpStatus.OK, ans.getStatusCode());
  }

  @Test
  void testDisableRepositoryWithDefault() {
    ResponseEntity<Void> ans = repo
        .disableKnowledgeArtifactRepository("repository");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, ans.getStatusCode());
  }

}
