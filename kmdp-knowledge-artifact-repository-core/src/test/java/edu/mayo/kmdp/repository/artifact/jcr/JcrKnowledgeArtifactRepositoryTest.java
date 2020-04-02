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

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.exceptions.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNoContentException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.springframework.http.HttpHeaders;

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

    List<Pointer> result = adapter.getKnowledgeArtifactSeries("1", artifactID)
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());
  }

  //"List stored knowledge artifacts"

  @Test
  void testListArtifactsRepoUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        RepositoryNotFoundException.class,
        () -> adapter
            .listKnowledgeArtifacts("none"));
  }

  @Test
  void testListArtifactsAllAvailableDeletedFalse() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository")
        .orElse(Collections.emptyList());

    assertEquals(2, pointers.size());

  }

  @Test
  void testListArtifactsOnlyGivenRepo() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository2", artifactID2, "LATEST",
        "hi!".getBytes());

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository")
        .orElse(Collections.emptyList());
    
    assertEquals(1, pointers.size());
  }

  @Test
  void testListArtifactsOnlyAvailableDeletedFalse() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResource("repository", artifactID);

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository")
        .orElse(Collections.emptyList());

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

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository")
        .orElse(Collections.emptyList());

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
        .orElse(Collections.emptyList());

    assertEquals(2, pointers.size());
  }

  @Test
  void testListArtifactsEmptySeries() {
    dao.saveResource("repository", artifactID);
    dao.saveResource("repository", artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResource("repository", artifactID);

    List<Pointer> pointers = adapter.listKnowledgeArtifacts("repository", null, null, true)
        .orElse(Collections.emptyList());

    assertEquals(2, pointers.size());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHref() {
    dao.saveResource("default", artifactID, "new", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("default")
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/default/artifacts/" + artifactID,
        result.get(0).getHref().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerUri() {
    dao.saveResource("default", artifactID, "new", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("default")
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());

    assertEquals(BASE_UUID_URN + artifactID,
        result.get(0).getResourceId().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultipleArtifacts() {
    dao.saveResource("hey", artifactID, "new", "hi!".getBytes());
    dao.saveResource("hey", artifactID, "new2", "hi!".getBytes());

    dao.saveResource("hey", artifactID2, "new", "hi!".getBytes());
    dao.saveResource("hey", artifactID2, "new2", "hi!".getBytes());

    List<Pointer> result = adapter.listKnowledgeArtifacts("hey")
        .orElse(Collections.emptyList());
    
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

    List<Pointer> result = adapter.listKnowledgeArtifacts("repo")
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/repo/artifacts/" + artifactID,
        result.get(0).getHref().toString());
  }

  //"Initialize new artifact series"
  @Test
  void testInitializeNewArtifactSeries() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    Answer<UUID> response = adapter.initKnowledgeArtifact("repository");
    UUID newArtifact = response.orElse(null);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repository")
        .orElse(Collections.emptyList());
    List<Pointer> versions = adapter
        .getKnowledgeArtifactSeries("repository", newArtifact)
        .orElse(Collections.emptyList());

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals(2, artifacts.size());
    assertEquals(0, versions.size());
  }

  @Test
  void testInitializeNewArtifactSeriesNewRepo() {
    dao.saveResource("repository", artifactID, "LATEST",
        "hi!".getBytes());
    Answer<UUID> response = adapter.initKnowledgeArtifact("repository2");
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repository2")
        .orElse(Collections.emptyList());
    
    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
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
            .getLatestKnowledgeArtifact("none", artifactID));
  }

  @Test
  void testGetLatestArtifactUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID2));
  }

  @Test
  void testGetLatestArtifactUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID));
  }

  @Test
  void testGetLatestArtifactUnavailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new",
        "hi!".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "newest".getBytes());
    dao.deleteResource("1", artifactID);
    Answer<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID, true);

    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
    assertEquals("newest", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestArtifactAvailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new",
        "hi!".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "newest".getBytes());
    Answer<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID);

    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
    assertEquals("newest", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestIgnoreUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new",
        "first".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResource("1", artifactID, "LATEST");
    Answer<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID);

    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
    assertEquals("first", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestIncludeUnavailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new",
        "first".getBytes());
    dao.saveResource("1", artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResource("1", artifactID, "LATEST");
    Answer<byte[]> response = adapter.getLatestKnowledgeArtifact("1", artifactID, true);

    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
    assertEquals("second", new String(response.orElse(new byte[0])));
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
            .getLatestKnowledgeArtifact("1", artifactID));
  }

  @Test
  void testGetLatestEmptySeries() {
    dao.saveResource("1", artifactID);

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID));
  }

  @Test
  void testGetLatestEmptySeriesDeletedTrue() {
    dao.saveResource("1", artifactID);

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getLatestKnowledgeArtifact("1", artifactID));
  }

  //    "Check Knowledge Artifact"
  @Test
  void checkSeriesAvailableUnknownRepo() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactSeries("repository1", artifactID));
  }

  @Test
  void checkSeriesAvailableUnknownArtifact() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactSeries("repository", artifactID2));
  }

  @Test
  void checkSeriesAvailableWithAvailableVersions() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    Answer<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID);
    assertEquals(ResponseCodeSeries.OK, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesAvailableWithUnavailableVersions() {
    dao.saveResource("repository", artifactID, "new", "document".getBytes());
    dao.deleteResource("repository", artifactID, "new");
    Answer<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID);
    assertEquals(ResponseCodeSeries.OK, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesAvailableWithNoVersions() {
    dao.saveResource("repository", artifactID);
    Answer<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID);
    assertEquals(ResponseCodeSeries.OK, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesUnavailableDeletedFalse() {
    dao.saveResource("repository", artifactID);
    dao.deleteResource("repository", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.isKnowledgeArtifactSeries("repository", artifactID));
  }

  @Test
  void checkSeriesUnavailableDeletedTrue() {
    dao.saveResource("repository", artifactID);
    dao.deleteResource("repository", artifactID);
    Answer<Void> responseEntity = adapter
        .isKnowledgeArtifactSeries("repository", artifactID, true);
    assertEquals(ResponseCodeSeries.OK, responseEntity.getOutcomeType());
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
    Answer<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID2);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repositoryId")
        .orElse(Collections.emptyList());

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals(2, artifacts.size());
  }

  @Test
  void testEnableSeriesSeriesUnavailable() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    dao.deleteResource("repositoryId", artifactID);
    Answer<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repositoryId")
        .orElse(Collections.emptyList());

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals(1, artifacts.size());
  }

  @Test
  void testEnableSeriesSeriesAvailable() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    Answer<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID);
    List<Pointer> artifacts = adapter.listKnowledgeArtifacts("repositoryId")
        .orElse(Collections.emptyList());

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals(1, artifacts.size());
  }

  @Test
  void testEnableSeriesVersionsUnavailable() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repositoryId", artifactID, "LATEST2",
        "hi!".getBytes());
    dao.deleteResource("repositoryId", artifactID, "LATEST");
    Answer<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID);
    List<Pointer> versions = adapter
        .getKnowledgeArtifactSeries("repositoryId", artifactID)
        .orElse(Collections.emptyList());
    
    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals(2, versions.size());
  }

  @Test
  void testEnableSeriesEmpty() {
    dao.saveResource("repositoryId", artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repositoryId", artifactID2);
    Answer<Void> response = adapter.enableKnowledgeArtifact("repositoryId", artifactID2);
    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
  }

  //"Removes a knowledge artifact from the repository"

  @Test
  void testRemoveSeriesRepoUnknown() {
    dao.saveResource("1", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .deleteKnowledgeArtifact("none", artifactID));
  }

  @Test
  void testRemoveSeriesArtifactUnknown() {
    dao.saveResource("1", artifactID2, "LATEST",
        "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter
            .deleteKnowledgeArtifact("1", artifactID));
  }

  @Test
  void testRemoveSeriesAvailable() {
    dao.saveResource("1", artifactID2, "LATEST",
        "hi!".getBytes());
    dao.saveResource("1", artifactID2, "LATEST2",
        "hi!".getBytes());
    dao.deleteResource("1", artifactID2, "LATEST");

    Answer<Void> responseEntity = adapter
        .deleteKnowledgeArtifact("1", artifactID2);

    assertThrows(
        ResourceNoContentException.class,
        () -> adapter
            .getKnowledgeArtifactSeries("1", artifactID2));

    assertEquals(ResponseCodeSeries.NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void testRemoveSeriesAlreadyUnavailable() {
    dao.saveResource("1", artifactID2, "LATEST",
        "hi!".getBytes());
    dao.saveResource("1", artifactID2, "LATEST2",
        "hi!".getBytes());

    adapter.deleteKnowledgeArtifact("1", artifactID2);

    Answer<Void> responseEntity = adapter
        .deleteKnowledgeArtifact("1", artifactID2);

    assertEquals(ResponseCodeSeries.NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void testRemoveSeriesEmpty() {
    dao.saveResource("1", artifactID2);

    Answer<Void> responseEntity = adapter
        .deleteKnowledgeArtifact("1", artifactID2);

    assertEquals(ResponseCodeSeries.NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void testRemoveSeriesDeleteParameterNotImplemented() {
    Answer<Void> response = adapter.deleteKnowledgeArtifact("1", artifactID, true);
    assertEquals(ResponseCodeSeries.NotImplemented, response.getOutcomeType());
  }

  //"List versions of a Knowledge Artifact"

  @Test
  void testListVersionsAllAvailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID)
            .orElse(Collections.emptyList());;

    assertEquals(2, results.size());
  }

  @Test
  void testListVersionsUnknownRepo() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactSeries("2", artifactID));
  }

  @Test
  void testListVersionsUnknownArtifactId() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactSeries("1", artifactID2));
  }

  @Test
  void testListVersionsSomeUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID)
        .orElse(Collections.emptyList());

    assertEquals(1, results.size());
  }

  @Test
  void testListVersionsSomeUnavailableDeletedTrue() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, true, 0, -1, null, null, null)
        .orElse(Collections.emptyList());

    assertEquals(2, results.size());
  }

  @Test
  void testListVersionsAllUnavailableDeletedFalse() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");
    dao.deleteResource("1", artifactID, "new");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID)
        .orElse(Collections.emptyList());

    assertEquals(0, results.size());
  }

  @Test
  void testListVersionsSeriesUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.saveResource("1", artifactID, "new2", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new2");
    dao.deleteResource("1", artifactID, "new");

    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID)
        .orElse(Collections.emptyList());

    assertEquals(0, results.size());
  }

  @Test
  void testListVersionsUnavailableSeries() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.getKnowledgeArtifactSeries("1", artifactID));
  }

  @Test
  void testListVersionsUnavailableSeriesParameterTrue() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    List<Pointer> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID, true, 0, -1, null, null, null)
        .orElse(Collections.emptyList());

    assertEquals(1, results.size());
  }

  @Test
  void testListVersionsEmptySeries() {
    dao.saveResource("1", artifactID);
    Answer<List<Pointer>> results = adapter
        .getKnowledgeArtifactSeries("1", artifactID);

    assertEquals(0, results.orElse(null).size());
    assertEquals(ResponseCodeSeries.OK, results.getOutcomeType());
  }

  @Test
  void testGetVersionsRightPointerHref() {
    dao.saveResource("default", artifactID, "new", "hi!".getBytes());

    List<Pointer> result = adapter
        .getKnowledgeArtifactSeries("default", artifactID)
        .orElse(null);

    assertEquals(1, result.size());

    assertEquals("http://localhost:8080/repos/default/artifacts/" + artifactID + "/versions/new",
        result.get(0).getHref().toString());
  }

  //“Add a (new version) of a Knowledge Artifact.”
  @Test
  void testAddArtifact() {
    Answer<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }

  @Test
  void testAddArtifactSeriesAvailable() {
    dao.saveResource("default", artifactID2, "1", "hi!".getBytes());
    Answer<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }

  @Test
  void testAddArtifactSeriesUnavailable() {
    dao.saveResource("default", artifactID2, "1", "hi!".getBytes());
    dao.deleteResource("default", artifactID2);
    Answer<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }

  @Test
  void testAddArtifactSeriesEmpty() {
    dao.saveResource("default", artifactID2);
    Answer<Void> response = adapter
        .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    Version version = dao.getLatestResource("default", artifactID2, false).getValue();

    assertEquals(ResponseCodeSeries.Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertEquals("available", getStatus(version));
  }


  @Test
  void testAddArtifactSeriesReturnsLocation() {
    Answer<Void> response = adapter
      .addKnowledgeArtifactVersion("default", artifactID2, "hi!".getBytes());
    String location = response.getMeta(HttpHeaders.LOCATION).orElse("").toString();
    String artifact = StringUtils.substringBetween(location, "artifacts/", "/versions");
    String repo = StringUtils.substringBetween(location, "repos/", "/artifact");

    assertEquals(artifactID2.toString(), artifact);
    assertEquals("default", repo);


  }


  //“Retrieve a specific version of a Knowledge Artifact”
  @Test
  void testGetVersionVersionAvailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    Answer<byte[]> response = adapter
        .getKnowledgeArtifactVersion("1", artifactID, "new", false);
    assertEquals("hi!", new String(response.orElse(new byte[0])));
    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
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

    Answer<byte[]> response = adapter
        .getKnowledgeArtifactVersion("1", artifactID, "new", true);
    assertEquals("hi!", new String(response.orElse(new byte[0])));
    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
  }

  @Test
  void testGetVersionRepoNotKnown() {
    dao.saveResource("differentRepo", artifactID, "differentVersion", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new"));
  }

  @Test
  void testGetVersionSeriesNotKnown() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID2, "new"));
  }

  @Test
  void testGetVersionVersionNotKnown() {
    dao.saveResource("1", artifactID, "differentVersion", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new"));
  }

  @Test
  void testGetVersionSeriesEmpty() {
    dao.saveResource("1", artifactID);
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.getKnowledgeArtifactVersion("1", artifactID, "new"));
  }

  //"Check knowledge artifact version"
  @Test
  void testCheckVersionUnknownRepo() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo2", artifactID, "new"));
  }

  @Test
  void testCheckVersionUnknownArtifact() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID2, "new"));
  }

  @Test
  void testCheckVersionUnknownVersion() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID, "new"));

  }

  @Test
  void testCheckVersionAvailable() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    Answer<Void> response = adapter
        .isKnowledgeArtifactVersion("repo1", artifactID, "version");
    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
  }

  @Test
  void testCheckVersionUnavailableDeletedTrue() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID, "version");
    Answer<Void> response = adapter
        .isKnowledgeArtifactVersion("repo1", artifactID, "version", true);
    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());
  }

  @Test
  void testCheckVersionUnavailableDeletedFalse() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID, "version");
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID, "version"));
  }

  @Test
  void testCheckVersionArtifactUnavailableDeletedFalse() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID);
    assertThrows(
        ResourceNoContentException.class,
        () -> adapter.isKnowledgeArtifactVersion("repo1", artifactID, "version"));
  }

  @Test
  void testCheckVersionArtifactUnavailableDeletedTrue() {
    dao.saveResource("repo1", artifactID, "version", "hello".getBytes());
    dao.deleteResource("repo1", artifactID);
    Answer<Void> response = adapter
        .isKnowledgeArtifactVersion("repo1", artifactID, "version", true);
    assertEquals(ResponseCodeSeries.OK, response.getOutcomeType());

  }

  //"Ensure a specific version of a Knowledge Artifact is available"

  @Test
  void testEnsureRepositoryUnknown() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.enableKnowledgeArtifactVersion("repository2", artifactID, "version"));

  }

  @Test
  void testEnsureArtifactUnknown() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.enableKnowledgeArtifactVersion("repository", artifactID2, "version"));

  }

  @Test
  void testEnsureVersionUnknown() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.enableKnowledgeArtifactVersion("repository", artifactID, "version2"));

  }

  @Test
  void testEnsureVersionUnavailable() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    dao.deleteResource("repository", artifactID, "version");
    Answer<Void> response = adapter
        .enableKnowledgeArtifactVersion("repository", artifactID, "version");
    Answer<List<Pointer>> availableVersions = adapter
        .getKnowledgeArtifactSeries("repository", artifactID);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
    assertEquals(1, availableVersions.orElse(null).size());
  }

  @Test
  void testEnsureSeriesUnavailable() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    dao.deleteResource("repository", artifactID);
    Answer<Void> response = adapter
        .enableKnowledgeArtifactVersion("repository", artifactID, "version", false);
    Answer<List<Pointer>> availableVersions = adapter
        .getKnowledgeArtifactSeries("repository", artifactID);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
    assertEquals(1, availableVersions.orElse(null).size());
  }

  @Test
  void testEnsureVersionIsAlreadyAvailable() {
    dao.saveResource("repository", artifactID, "version", "thisExists".getBytes());
    Answer<Void> response = adapter
        .enableKnowledgeArtifactVersion("repository", artifactID, "version", false);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());

  }

  //"Sets a version of a specific knowledge artifact"
  @Test
  void testPutOnExistingVersion() {
    dao.saveResource("repositoryId", artifactID, "new", "hi!".getBytes());
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "new")
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionDoesNotExist() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "new", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionArtifactDoesNotExist() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID2, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID2, "new", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionRepoDoesNotExist() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId2", artifactID2, "new", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId2", artifactID2, "new", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionUnavailableVersion() {
    dao.saveResource("repositoryId", artifactID, "first", "hi!".getBytes());
    dao.deleteResource("repositoryId", artifactID, "first");
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "first", "replaced".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "first", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionUnavailableArtifact() {
    dao.saveResource("repositoryId", artifactID, "first", "payload".getBytes());
    dao.deleteResource("repositoryId", artifactID);
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "first", "payload".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "first", false)
        .orElse(new byte[0]);

    assertEquals("payload", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionEmptyArtifact() {
    adapter.initKnowledgeArtifact("repositoryId");
    Answer<Void> response = adapter
        .setKnowledgeArtifactVersion("repositoryId", artifactID, "first", "payload".getBytes());
    byte[] replacedArtifact = adapter
        .getKnowledgeArtifactVersion("repositoryId", artifactID, "first", false)
        .orElse(new byte[0]);

    assertEquals("payload", new String(replacedArtifact));
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  // “Remove a specific version of a Knowledge Artifact”

  @Test
  void testDeleteVersionAvailable() throws Exception {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    Answer<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new", false);
    Node deletedVersion = dao.getResource("1", artifactID, "new", true).getValue().getFrozenNode();
    assertEquals("unavailable", deletedVersion.getProperty("status").getString());
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
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
        () -> adapter.deleteKnowledgeArtifactVersion("1", artifactID, "none"));
  }

  @Test
  void testDeleteVersionNoRepo() {
    dao.saveResource("different", artifactID, "new", "hi!".getBytes());
    assertThrows(
        ResourceNotFoundException.class,
        () -> adapter.deleteKnowledgeArtifactVersion("1", artifactID, "none"));
  }

  @Test
  void testDeleteVersionAlreadyUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID, "new");
    Answer<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new");
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testDeleteVersionSeriesUnavailable() {
    dao.saveResource("1", artifactID, "new", "hi!".getBytes());
    dao.deleteResource("1", artifactID);
    Answer<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new");
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testDeleteParameterNotImplemented() {
    Answer<Void> response = adapter
        .deleteKnowledgeArtifactVersion("1", artifactID, "new", true);
    assertEquals(ResponseCodeSeries.NotImplemented, response.getOutcomeType());
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



