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
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.ResourceNoContentException;
import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import edu.mayo.kmdp.util.FileUtil;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class JcrKnowledgeArtifactRepositoryTest {

  private JcrKnowledgeArtifactRepository adapter;
  private JcrDao dao;

  private UUID artifactID;
  private UUID artifactID2;

  @BeforeEach
  void repo() {
    KnowledgeArtifactRepositoryServerConfig cfg =
        new KnowledgeArtifactRepositoryServerConfig().with(
            KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, "1");

    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    dao = new JcrDao(jcr);
    adapter = new JcrKnowledgeArtifactRepository(dao, cfg);
    artifactID = UUID.randomUUID();
    artifactID2 = UUID.randomUUID();
  }

  @AfterEach
  void cleanup() {
    adapter.shutdown();
  }

  @Test
  void testLoadTwice() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());

    ResponseEntity<List<Pointer>> result = adapter
        .getKnowledgeArtifactSeries("1", artifactID, false, 0, -1, null, null, null);

    assertEquals(1, result.getBody().size());
  }

  //"List stored knowledge artifacts"

  @Test
  void testListArtifactsRepoUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        RepositoryNotFoundException.class,
        () -> adapter
            .listKnowledgeArtifacts("none", null, null, false));
  }

  @Test
  void testListArtifactsAllAvailableDeletedFalse() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, false)
        .getBody();

    assertEquals(2, pointers.size());

  }

  @Test
  void testListArtifactsOnlyGivenRepo() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository2", artifactID2, "LATEST",
        "hi!".getBytes());

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, false)
        .getBody();

    assertEquals(1, pointers.size());
  }

  @Test
  void testListArtifactsOnlyAvailableDeletedFalse() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResource("repository", artifactID);

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, false)
        .getBody();

    assertEquals(1, pointers.size());
  }

  @Test
  void testListArtifactsAllUnavailableDeletedFalse() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResource("repository", artifactID);
    dao.deleteResource("repository", artifactID2);

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, false)
        .getBody();

    assertEquals(0, pointers.size());
  }

  @Test
  void testListArtifactsAllReturnedDeletedTrue() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResource("repository", artifactID);

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, true)
        .getBody();

    assertEquals(2, pointers.size());
  }

  @Test
  void testListArtifactsEmptySeries() {
    dao.saveResource("repository", artifactID);
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResource("repository", artifactID);

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, true)
        .getBody();

    assertEquals(2, pointers.size());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHref() {
    dao.saveResource("default", artifactID, "new", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("default", 0, -1, false).getBody();

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/default/artifacts/" + artifactID,
        result.get(0).getHref().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerUri() {
    dao.saveResource("default", artifactID, "new", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("default", 0, -1, false).getBody();

    assertEquals(1, result.size());

    assertEquals("UUID:" + artifactID,
        result.get(0).getEntityRef().getUri().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultipleArtifacts() {
    dao.saveResource("hey", artifactID, "new", "hi!".getBytes());
    dao.saveResource("hey", artifactID, "new2", "hi!".getBytes());

    dao.saveResource("hey", artifactID2, "new", "hi!".getBytes());
    dao.saveResource("hey", artifactID2, "new2", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("hey", 0, -1, false).getBody();

    assertEquals(2, result.size());

    Set<String> resultSet = result.stream().map(it -> it.getHref().toString())
        .collect(Collectors.toSet());

    assertTrue(resultSet.contains("http://localhost:8080/repos/hey/artifacts/" + artifactID));
    assertTrue(resultSet.contains("http://localhost:8080/repos/hey/artifacts/" + artifactID2));
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultiple() {
    dao.saveResource("repo", artifactID, "new", "hi!".getBytes());
    dao.saveResource("repo", artifactID, "new2", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("repo", 0, -1, false).getBody();

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/repo/artifacts/" + artifactID,
        result.get(0).getHref().toString());
  }

  //"Initialize new artifact series"
  @Test
  void testInitializeNewArtifactSeries() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    ResponseEntity<UUID> response = adapter.initKnowledgeArtifact("repository");
    UUID newArtifact = response.getBody();
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repository", null, null, false)
        .getBody();
    List<Pointer> versions = adapter
        .getKnowledgeArtifactSeries("repository", newArtifact, false, null, null, null, null, null)
        .getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(2, artifacts.size());
    assertEquals(0, versions.size());
  }

  @Test
  void testInitializeNewArtifactSeriesNewRepo() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    ResponseEntity<UUID> response = adapter.initKnowledgeArtifact("repository2");
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repository2", null, null, false)
        .getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1, artifacts.size());
  }

  //"Retrieves the LATEST version of a knowledge artifact"
  @Test
  void testGetLatestRepoUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("none", artifactID, false));
  }

  @Test
  void testGetLatestArtifactUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID2, false));
  }

  @Test
  void testGetLatestArtifactUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID, false));
  }

  @Test
  void testGetLatestArtifactUnavailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new",
        "hi!".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "newest".getBytes());
    dao.deleteResource("1", artifactID);
    ResponseEntity<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID, true);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("newest", new String(response.getBody()));
  }

  @Test
  void testGetLatestArtifactAvailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new",
        "hi!".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "newest".getBytes());
    ResponseEntity<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID, true);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("newest", new String(response.getBody()));
  }

  @Test
  void testGetLatestIgnoreUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new",
        "first".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResource("1", artifactID, "LATEST");
    ResponseEntity<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID, false);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("first", new String(response.getBody()));
  }

  @Test
  void testGetLatestIncludeUnavailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new",
        "first".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResource("1", artifactID, "LATEST");
    ResponseEntity<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID, true);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("second", new String(response.getBody()));
  }

  @Test
  void testGetLatestAllUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new",
        "first".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResource("1", artifactID, "new");
    dao.deleteResource("1", artifactID, "LATEST");

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID, false));
  }

  @Test
  void testGetLatestEmptySeries() {
    dao.saveResource("1", artifactID);

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID, false));
  }

  @Test
  void testGetLatestEmptySeriesDeletedTrue() {
    dao.saveResource("1", artifactID);

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID, true));
  }

  //    "Check Knowledge Artifact"
  @Test
  void checkSeriesAvailableUnknownRepo() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactSeries("repository1", artifactID, false));
  }

  @Test
  void checkSeriesAvailableUnknownArtifact() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactSeries("repository", artifactID2, false));
  }

  @Test
  void checkSeriesAvailableWithAvailableVersions() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    ResponseEntity<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID, false);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void checkSeriesAvailableWithUnavailableVersions() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    dao.deleteResource("repository", artifactID, "new");
    ResponseEntity<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID, false);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void checkSeriesAvailableWithNoVersions() {
    dao.saveResource("repository", artifactID);
    ResponseEntity<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID, false);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void checkSeriesUnavailableDeletedFalse() {
    dao.saveResource("repository", artifactID);
    dao.deleteResource("repository", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.isKnowledgeArtifactSeries("repository", artifactID, false));
  }

  @Test
  void checkSeriesUnavailableDeletedTrue() {
    dao.saveResource("repository", artifactID);
    dao.deleteResource("repository", artifactID);
    ResponseEntity<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID, true);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  //"Enable Knowledge Artifact Series"

  @Test
  void testEnableSeriesRepoUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .enableKnowledgeArtifact("none", artifactID));
  }

  @Test
  void testEnableSeriesArtifactUnknown() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    ResponseEntity<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID2);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repositoryId", null, null, false)
        .getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(2, artifacts.size());
  }

  @Test
  void testEnableSeriesSeriesUnavailable() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    dao.deleteResource("repositoryId", artifactID);
    ResponseEntity<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repositoryId", null, null, false)
        .getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1, artifacts.size());
  }

  @Test
  void testEnableSeriesSeriesAvailable() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    ResponseEntity<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repositoryId", null, null, false)
        .getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1, artifacts.size());
  }

  @Test
  void testEnableSeriesVersionsUnavailable() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repositoryId", artifactID, "LATEST2",
        "hi!".getBytes());
    dao.deleteResource("repositoryId", artifactID, "LATEST");
    ResponseEntity<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID);
    List<Pointer> versions = adapter
        .getKnowledgeArtifactSeries("repositoryId", artifactID, false, null, null, null, null, null)
        .getBody();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(2, versions.size());
  }

  @Test
  void testEnableSeriesEmpty() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repositoryId", artifactID2);
    ResponseEntity<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID2);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  //"Removes a knowledge artifact from the repository"

  @Test
  void testRemoveSeriesRepoUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .deleteKnowledgeArtifact("none", artifactID, false));
  }

  @Test
  void testRemoveSeriesArtifactUnknown() {
    dao.saveResource("1", artifactID2, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .deleteKnowledgeArtifact("1", artifactID, false));
  }

  @Test
  void testRemoveSeriesAvailable() {
    dao.saveResource("1", artifactID2, "LATEST",
        "hi!".getBytes());
    dao.saveResource("1", artifactID2, "LATEST2",
        "hi!".getBytes());
    dao.deleteResource("1", artifactID2, "LATEST");

    ResponseEntity<Void> responseEntity = adapter
        .deleteKnowledgeArtifact("1", artifactID2, false);

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getKnowledgeArtifactSeries("1", artifactID2, false, null, null, null, null, null));

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  void testRemoveSeriesAlreadyUnavailable() {
    dao.saveResource("1", artifactID2, "LATEST",
        "hi!".getBytes());
    dao.saveResource("1", artifactID2, "LATEST2",
        "hi!".getBytes());

    adapter.deleteKnowledgeArtifact("1", artifactID2, false);

    ResponseEntity<Void> responseEntity = adapter
        .deleteKnowledgeArtifact("1", artifactID2, false);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  void testRemoveSeriesEmpty() {
    dao.saveResource("1", artifactID2);

    ResponseEntity<Void> responseEntity = adapter
        .deleteKnowledgeArtifact("1", artifactID2, false);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  void testRemoveSeriesDeleteParameterNotImplemented() {
    ResponseEntity<Void> response = adapter.deleteKnowledgeArtifact("1", artifactID, true);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
  }

  //"List versions of a Knowledge Artifact"

  @Test
  void testListVersionsAllAvailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, false, 0, -1, null, null, null).getBody();

    assertEquals(2, results.size());
  }

  @Test
  void testListVersionsUnknownRepo() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactSeries("2", artifactID, false, 0, -1, null, null, null));
  }

  @Test
  void testListVersionsUnknownArtifactId() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactSeries("1", artifactID2, false, 0, -1, null, null, null));
  }

  @Test
  void testListVersionsSomeUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, false, 0, -1, null, null, null).getBody();

    assertEquals(1, results.size());
  }

  @Test
  void testListVersionsSomeUnavailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, true, 0, -1, null, null, null).getBody();

    assertEquals(2, results.size());
  }

  @Test
  void testListVersionsAllUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");
    dao.deleteResource("1", artifactID, "new");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, false, 0, -1, null, null, null).getBody();

    assertEquals(0, results.size());
  }

  @Test
  void testListVersionsSeriesUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");
    dao.deleteResource("1", artifactID, "new");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, false, 0, -1, null, null, null).getBody();

    assertEquals(0, results.size());
  }

  @Test
  void testListVersionsUnavailableSeries() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.getKnowledgeArtifactSeries("1", artifactID, false, 0, -1, null, null, null));
  }

  @Test
  void testListVersionsUnavailableSeriesParameterTrue() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, true, 0, -1, null, null, null).getBody();

    assertEquals(1, results.size());
  }

  @Test
  void testListVersionsEmptySeries() {
    dao.saveResource("1", artifactID);
    ResponseEntity<List<Pointer>> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, true, 0, -1, null, null, null);

    assertEquals(0, results.getBody().size());
    assertEquals(HttpStatus.OK, results.getStatusCode());
  }

  @Test
  void testGetVersionsRightPointerHref() {
    dao.saveResource("default", artifactID, "new", "hi!".getBytes());

    List<Pointer> result = adapter
        .getKnowledgeArtifactSeries("default", artifactID, false, null, null, null, null, null)
        .getBody();

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/default/artifacts/" + artifactID + "/versions/new",
        result.get(0).getHref().toString());
  }

  //“Add a (new version) of a Knowledge Artifact.”
  @Test
  void testAddArtifact() {
    ResponseEntity<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }

  @Test
  void testAddArtifactSeriesAvailable() {
    dao.saveResource("default", artifactID2, "1", "hi!".getBytes());
    ResponseEntity<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }

  @Test
  void testAddArtifactSeriesUnavailable() {
    dao.saveResource("default", artifactID2, "1", "hi!".getBytes());
    dao.deleteResource("default", artifactID2);
    ResponseEntity<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }

  @Test
  void testAddArtifactSeriesEmpty() {
    dao.saveResource("default", artifactID2);
    ResponseEntity<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }


  @Test
  void testAddArtifactSeriesReturnsLocation() {
    ResponseEntity<Void> response = adapter
      .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    String location = response.getHeaders().getLocation().toString();
    String artifact = StringUtils.substringBetween(location, "artifacts/", "/versions");
    String repo = StringUtils.substringBetween(location, "repos/", "/artifact");

    assertEquals(artifactID2.toString(), artifact);
    assertEquals("default", repo);


  }


  //“Retrieve a specific version of a Knowledge Artifact”
  @Test
  void testGetVersionVersionAvailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    ResponseEntity<byte[]> response = adapter
        .getKnowledgeArtifactVersion("1", artifactID, "new", false);
    assertEquals("hi!", new String(response.getBody()));
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testGetVersionVersionUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new");

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new", false));
  }

  @Test
  void testGetVersionDeletedParameterTrue() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new");

    ResponseEntity<byte[]> response = adapter
        .getKnowledgeArtifactVersion("1", artifactID, "new", true);
    assertEquals("hi!", new String(response.getBody()));
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testGetVersionRepoNotKnown() {
    dao.saveResource("differentRepo", artifactID, "differentVersion", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new", false));
  }

  @Test
  void testGetVersionSeriesNotKnown() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID2, "new", false));
  }

  @Test
  void testGetVersionVersionNotKnown() {
    dao.saveResource("1", artifactID, "differentVersion", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new", false));
  }

  @Test
  void testGetVersionSeriesEmpty() {
    dao.saveResource("1", artifactID);
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new", false));
  }

  //"Check knowledge artifact version"
  @Test
  void testCheckVersionUnknownRepo() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo2", artifactID, "new", false));
  }

  @Test
  void testCheckVersionUnknownArtifact() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID2, "new", false));
  }

  @Test
  void testCheckVersionUnknownVersion() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID, "new", false));

  }

  @Test
  void testCheckVersionAvailable() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    ResponseEntity<Void> response = adapter
        .isKnowledgeArtifactVersion("repo1", artifactID, "version", false);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testCheckVersionUnavailableDeletedTrue() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID, "version");
    ResponseEntity<Void> response = adapter
        .isKnowledgeArtifactVersion("repo1", artifactID, "version", true);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testCheckVersionUnavailableDeletedFalse() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID, "version");
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID, "version", false));
  }

  @Test
  void testCheckVersionArtifactUnavailableDeletedFalse() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID, "version", false));
  }

  @Test
  void testCheckVersionArtifactUnavailableDeletedTrue() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID);
    ResponseEntity<Void> response = adapter
        .isKnowledgeArtifactVersion("repo1", artifactID, "version", true);
    assertEquals(HttpStatus.OK, response.getStatusCode());

  }

  //"Ensure a specific version of a Knowledge Artifact is available"

  @Test
  void testEnsureRepositoryUnknown() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.enableKnowledgeArtifactVersion("repository2", artifactID, "version", false));

  }

  @Test
  void testEnsureArtifactUnknown() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.enableKnowledgeArtifactVersion("repository", artifactID2, "version", false));

  }

  @Test
  void testEnsureVersionUnknown() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.enableKnowledgeArtifactVersion("repository", artifactID, "version2", false));

  }

  @Test
  void testEnsureVersionUnavailable() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    dao.deleteResource("repository", artifactID, "version");
    ResponseEntity<Void> response = adapter
        .enableKnowledgeArtifactVersion("repository", artifactID, "version", false);
    ResponseEntity<List<Pointer>> availableVersions = adapter
        .getKnowledgeArtifactSeries("repository", artifactID, false, null, null, null, null, null);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertEquals(1, availableVersions.getBody().size());
  }

  @Test
  void testEnsureSeriesUnavailable() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    dao.deleteResource("repository", artifactID);
    ResponseEntity<Void> response = adapter
        .enableKnowledgeArtifactVersion("repository", artifactID, "version", false);
    ResponseEntity<List<Pointer>> availableVersions = adapter
        .getKnowledgeArtifactSeries("repository", artifactID, false, null, null, null, null, null);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertEquals(1, availableVersions.getBody().size());
  }

  @Test
  void testEnsureVersionIsAlreadyAvailable() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    ResponseEntity<Void> response = adapter
        .enableKnowledgeArtifactVersion("repository", artifactID, "version", false);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

  }

  //"Sets a version of a specific knowledge artifact"
  @Test
  void testPutOnExistingVersion() {
    dao.saveResource("repositoryId", artifactID, "new", "hi!".getBytes());
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "new", false).getBody();

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testPutOnVersionDoesNotExist() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "new", false).getBody();

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testPutOnVersionArtifactDoesNotExist() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID2, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID2, "new", false).getBody();

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testPutOnVersionRepoDoesNotExist() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId2", artifactID2, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId2", artifactID2, "new", false).getBody();

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testPutOnVersionUnavailableVersion() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    dao.deleteResource("repositoryId", artifactID, "first");
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "first", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "first", false).getBody();

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testPutOnVersionUnavailableArtifact() {
    dao.saveResource("repositoryId", artifactID, "first", "payload".getBytes());
    dao.deleteResource("repositoryId", artifactID);
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "first", "payload".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "first", false).getBody();

    assertEquals("payload", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testPutOnVersionEmptyArtifact() {
    adapter.initKnowledgeArtifact("repositoryId");
    ResponseEntity<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "first", "payload".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "first", false).getBody();

    assertEquals("payload", new String(replacedArtifact));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  // “Remove a specific version of a Knowledge Artifact”

  @Test
  void testDeleteVersionAvailable() throws Exception {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    ResponseEntity<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new", false);
    Node deletedVersion = dao.getResource("1", artifactID, "new", true).getValue().getFrozenNode();
    assertEquals("unavailable", deletedVersion.getProperty("status").getString());
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testDeleteVersionNoArtifact() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.deleteKnowledgeArtifactVersion("1", artifactID2, "new", false));
  }

  @Test
  void testDeleteVersionNoVersion() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.deleteKnowledgeArtifactVersion("1", artifactID, "none", false));
  }

  @Test
  void testDeleteVersionNoRepo() {
    dao.saveResource("different", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.deleteKnowledgeArtifactVersion("1", artifactID, "none", false));
  }

  @Test
  void testDeleteVersionAlreadyUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new");
    ResponseEntity<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new", false);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testDeleteVersionSeriesUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    ResponseEntity<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new", false);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void testDeleteParameterNotImplemented() {
    ResponseEntity<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new", true);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
  }

  String getPayload(Version v) {
    try {
      return FileUtil.read(v.getFrozenNode().getProperty("jcr:data").getBinary().getStream())
          .orElse("");
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  String getStatus(Version v) {
    try {
      return v.getFrozenNode().getProperty("status").getString();
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

}



