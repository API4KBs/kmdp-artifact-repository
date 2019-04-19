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

import static edu.mayo.kmdp.id.helper.DatatypeHelper.uri;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.SERVER_HOST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.repository.artifact.jcr.JcrDao;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omg.spec.api4kp._1_0.PlatformComponentHelper;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class KnowledgeArtifactRepositoryTest {

  @TempDir
  static Path tempDir;

  private static JackrabbitRepository jack;
  private static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository repo;

  private static KnowledgeArtifactRepositoryServerConfig cfg =
      new KnowledgeArtifactRepositoryServerConfig()
          .with(DEFAULT_REPOSITORY_NAME, "TestRepository")
          .with(DEFAULT_REPOSITORY_ID, "TestRepo");

  @BeforeAll
  static void repo() throws ConfigurationException {
    RepositoryConfig jcrConfig = RepositoryConfig.create(
        KnowledgeArtifactRepositoryTest.class.getResourceAsStream("/test-repository.xml"),
        tempDir.toString());

    jack = new TransientRepository(jcrConfig);
    JcrDao dao = new JcrDao(jack, Collections
        .singletonList(JcrKnowledgeArtifactRepository.JcrTypes.KNOWLEDGE_ARTIFACT.name()), cfg);

    repo = new JcrKnowledgeArtifactRepository(dao, cfg);
  }

  @AfterAll
  static void cleanup() {
    TransientRepository jackrabbit = (TransientRepository) jack;
    jackrabbit.shutdown();
  }

  @Test
  void testListRepositoryWithDefault() {
    ResponseEntity<List<KnowledgeArtifactRepository>> ans = repo
        .listKnowledgeArtifactRepositories();
    assertEquals(HttpStatus.OK, ans.getStatusCode());
    assertEquals(1, ans.getBody().size());

    KnowledgeArtifactRepository descr = ans.getBody().get(0);
    assertEquals(URI.create(cfg.getTyped(BASE_NAMESPACE) +
            "/repos/" +
            cfg.getTyped(DEFAULT_REPOSITORY_ID)),
        descr.getId().getUri());
    assertEquals(URI.create(cfg.getTyped(SERVER_HOST) +
            "/repos/" +
            cfg.getTyped(DEFAULT_REPOSITORY_ID)),
        descr.getHref());
    assertEquals(cfg.getTyped(DEFAULT_REPOSITORY_NAME),
        descr.getName());
    assertTrue(descr.getAlias() != null && descr.getAlias()
        .contains(uri("uri:uuid:" + cfg.getTyped(DEFAULT_REPOSITORY_ID))));
    assertNotNull(descr.getInstanceId());
  }

  @Test
  void testGetDefaultRepositoryDescriptor() {
    ResponseEntity<KnowledgeArtifactRepository> ans = repo
        .getKnowledgeArtifactRepository(cfg.getTyped(DEFAULT_REPOSITORY_ID));
    assertEquals(HttpStatus.OK, ans.getStatusCode());
    KnowledgeArtifactRepository descr = ans.getBody();
    assertNotNull(descr);
  }

  @Test
  void testExistsDefaultRepository() {
    ResponseEntity<Void> ans = repo
        .isKnowledgeArtifactRepository(cfg.getTyped(DEFAULT_REPOSITORY_ID));
    assertEquals(HttpStatus.NO_CONTENT, ans.getStatusCode());
  }

  @Test
  void testCantDeleteDefaultRepository() {
    ResponseEntity<Void> ans = repo
        .deleteKnowledgeArtifactRepository(cfg.getTyped(DEFAULT_REPOSITORY_ID));
    assertEquals(HttpStatus.FORBIDDEN, ans.getStatusCode());
  }


  @Test
  void testNonExistingRepository() {
    ResponseEntity<Void> ans = repo
        .isKnowledgeArtifactRepository("non-existing-"+Math.random());
    assertEquals(HttpStatus.NOT_FOUND, ans.getStatusCode());
  }

  @Test
  void testGetNonExistingRepositoryDescriptor() {
    ResponseEntity<KnowledgeArtifactRepository> ans = repo
        .getKnowledgeArtifactRepository("non-existing-"+Math.random());
    assertEquals(HttpStatus.NOT_FOUND, ans.getStatusCode());
  }

  @Test
  void testInitRepository() {
    UUID id = UUID.randomUUID();
    KnowledgeArtifactRepository descr = PlatformComponentHelper
        .repositoryDescr(cfg.getTyped(BASE_NAMESPACE),
            id.toString(), "non-standard repository", cfg.getTyped(SERVER_HOST))
        .orElse(null);
    assertNotNull(descr);
    ResponseEntity<Void> ans = repo.initKnowledgeArtifactRepository(descr);
    assertEquals(HttpStatus.CREATED, ans.getStatusCode());

    ResponseEntity<Void> ans2 = repo.deleteKnowledgeArtifactRepository(descr.getId().getTag());
    assertEquals(HttpStatus.OK, ans2.getStatusCode());
  }

  @Test
  void testInitRepositoryWithoutLegalId() {
    UUID id = UUID.randomUUID();
    KnowledgeArtifactRepository descr = PlatformComponentHelper
        .repositoryDescr(cfg.getTyped(BASE_NAMESPACE),
            id.toString(), "non-standard repository", cfg.getTyped(SERVER_HOST))
        .orElse(null);
    assertNotNull(descr);

    descr.setId(null);
    ResponseEntity<Void> ans = repo.initKnowledgeArtifactRepository(descr);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,ans.getStatusCode());
  }

  @Test
  void testGetCustomRepository() {
    UUID id = UUID.randomUUID();
    KnowledgeArtifactRepository init = PlatformComponentHelper
        .repositoryDescr(cfg.getTyped(BASE_NAMESPACE),
            id.toString(), "non-standard repository", cfg.getTyped(SERVER_HOST))
        .orElse(null);
    assertNotNull(init);
    ResponseEntity<Void> ans = repo.initKnowledgeArtifactRepository(init);
    assertEquals(HttpStatus.CREATED, ans.getStatusCode());

    ResponseEntity<KnowledgeArtifactRepository> ans2 = repo.getKnowledgeArtifactRepository(id.toString());

    KnowledgeArtifactRepository descr = ans2.getBody();
    assertEquals(URI.create(cfg.getTyped(BASE_NAMESPACE) +
            "/repos/" +
            id.toString()),
        descr.getId().getUri());
    assertEquals(URI.create(cfg.getTyped(SERVER_HOST) +
            "/repos/" +
            id.toString()),
        descr.getHref());
    assertEquals("non-standard repository",
        descr.getName());
    assertTrue(descr.getAlias() != null && descr.getAlias()
        .contains(uri("uri:uuid:" + id.toString())));
    assertNotNull(descr.getInstanceId());

    ResponseEntity<Void> ans3 = repo.deleteKnowledgeArtifactRepository(descr.getId().getTag());
    assertEquals(HttpStatus.OK, ans3.getStatusCode());
  }

  @Test
  void testCustomRepository() {
    UUID id = UUID.randomUUID();
    KnowledgeArtifactRepository init = PlatformComponentHelper
        .repositoryDescr(cfg.getTyped(BASE_NAMESPACE),
            id.toString(), "non-standard repository", cfg.getTyped(SERVER_HOST))
        .orElse(null);
    assertNotNull(init);
    ResponseEntity<Void> ans = repo.initKnowledgeArtifactRepository(init);
    assertEquals(HttpStatus.CREATED, ans.getStatusCode());

    ResponseEntity<List<KnowledgeArtifactRepository>> ans2 = repo.listKnowledgeArtifactRepositories();
    assertEquals(HttpStatus.OK, ans2.getStatusCode());
    assertEquals(2, ans2.getBody().size());

    ans2.getBody().forEach((descr) -> {
      ResponseEntity<Void> ansx = repo.isKnowledgeArtifactRepository(descr.getId().getTag());
      assertEquals(HttpStatus.NO_CONTENT, ansx.getStatusCode());
    });

    ans2.getBody().stream()
        .filter((descr) -> !cfg.getTyped(DEFAULT_REPOSITORY_ID).equals(descr.getId().getTag()))
        .forEach((nonDefaultDescr) -> {
          ResponseEntity<Void> ansy = repo
              .deleteKnowledgeArtifactRepository(nonDefaultDescr.getId().getTag());
          assertEquals(HttpStatus.OK, ansy.getStatusCode());
        });

    ResponseEntity<List<KnowledgeArtifactRepository>> ans4 = repo.listKnowledgeArtifactRepositories();
    assertEquals(HttpStatus.OK, ans4.getStatusCode());
    assertEquals(1, ans4.getBody().size());

  }



  // TODO: fail deleting default repository




//  ResponseEntity<Void> setKnowledgeArtifactRepository(String repositoryId,
//      org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository repositoryDescr);

}
