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

import static edu.mayo.kmdp.util.ws.ResponseHelper.attempt;
import static edu.mayo.kmdp.util.ws.ResponseHelper.notSupported;
import static edu.mayo.kmdp.util.ws.ResponseHelper.succeed;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.repository.artifact.HrefBuilder;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.ResourceIdentificationException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.version.Version;
import org.apache.commons.io.IOUtils;
import org.omg.spec.api4kp._1_0.PlatformComponentHelper;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class JcrKnowledgeArtifactRepository implements DisposableBean,
    edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository {

  private static final String JCR_DATA = "jcr:data";

  private KnowledgeArtifactRepositoryServerConfig cfg;
  private HrefBuilder hrefBuilder;

  private String defaultRepositoryId;
  private String defaultRepositoryName;

  private JcrDao dao;

  //*********************************************************************************************/
  //* Constructors */
  //*********************************************************************************************/


  public JcrKnowledgeArtifactRepository(javax.jcr.Repository delegate,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this(delegate, null, cfg);
  }

  public JcrKnowledgeArtifactRepository(JcrDao dao, KnowledgeArtifactRepositoryServerConfig cfg) {
    this.cfg = cfg;
    hrefBuilder = new HrefBuilder(cfg);
    this.dao = dao;
    this.defaultRepositoryId = cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    this.defaultRepositoryName = cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME);
  }

  public JcrKnowledgeArtifactRepository(javax.jcr.Repository delegate, Runnable cleanup,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this(new JcrDao(delegate, cleanup), cfg);
  }

  private Optional<KnowledgeArtifactRepository> createRepositoryDescriptor(
      String repositoryId, String repositoryName) {
    return PlatformComponentHelper.repositoryDescr(
        cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE),
        repositoryId,
        repositoryName,
        cfg.getTyped(KnowledgeArtifactRepositoryOptions.SERVER_HOST)
    );
  }

  public void shutdown() {
    dao.shutdown();
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Repository - Management APIs */
  //*********************************************************************************************/

  @Override
  public ResponseEntity<List<KnowledgeArtifactRepository>> listKnowledgeArtifactRepositories() {
    return notSupported();
  }

  @Override
  public ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> initKnowledgeArtifactRepository() {
    return notSupported();
  }

  @Override
  public ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> setKnowledgeArtifactRepository(
      String repositoryId,
      KnowledgeArtifactRepository repositoryDescr) {
    return notSupported();
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactRepository(String repositoryId) {
    return notSupported();
  }

  @Override
  public ResponseEntity<KnowledgeArtifactRepository> getKnowledgeArtifactRepository(
      String repositoryId) {
    return attempt(defaultRepositoryId.equals(repositoryId)
        ? createRepositoryDescriptor(repositoryId, defaultRepositoryName)
        : Optional.empty());
  }

  @Override
  public ResponseEntity<Void> disableKnowledgeArtifactRepository(String repositoryId) {
    return notSupported();
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Series - Management APIs */
  //*********************************************************************************************/

  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeArtifacts(String repositoryId, Integer offset,
      Integer limit, Boolean deleted) {
    try (JcrDao.DaoResult<List<Node>> result = dao
        .getResources(repositoryId, deleted, new HashMap<>())) {
      List<Node> nodes = result.getValue();

      List<Pointer> pointers = nodes.stream()
          .map(node -> artifactToPointer(node, repositoryId))
          .collect(Collectors.toList());

      return succeed(pointers);
    }
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeArtifact(String repositoryId) {
    UUID artifactId = UUID.randomUUID();

    try (JcrDao.DaoResult<Node> ignored = dao
        .saveResource(repositoryId, artifactId)) {
      return succeed(artifactId, HttpStatus.CREATED);
    }
  }

  @Override
  public ResponseEntity<Void> clearKnowledgeRepository(String repositoryId,
      Boolean deleted) {
    return notSupported();
  }

  @Override
  public ResponseEntity<byte[]> getLatestKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    try (JcrDao.DaoResult<Version> result = dao
        .getLatestResource(repositoryId, artifactId, deleted)) {
      Version version = result.getValue();
      return succeed(getDataFromNode(version));
    }
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactSeries(String repositoryId, UUID artifactId,
      Boolean deleted) {
    try (JcrDao.DaoResult<List<Version>> ignored = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      return succeed();
    }
  }

  @Override
  public ResponseEntity<Void> enableKnowledgeArtifact(String repositoryId,
      UUID artifactId) {
    dao.enableResource(repositoryId, artifactId);
    return succeed(HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    if ((Boolean.TRUE.equals(deleted))) {
      return notSupported();
    }
    dao.deleteResource(repositoryId, artifactId);
    return succeed(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeArtifactSeries(String repositoryId,
      UUID artifactId, Boolean deleted, Integer offset, Integer limit,
      String beforeTag, String afterTag, String sort) {
    try (JcrDao.DaoResult<List<Version>> result = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      List<Version> versions = result.getValue();

      return versions.isEmpty()
          ? succeed(Collections.emptyList())
          : succeed(versions.stream()
              .map(version -> versionToPointer(version, repositoryId))
              .collect(Collectors.toList()));
    }
  }

  @Override
  public ResponseEntity<Void> addKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      byte[] document) {
    Map<String, String> params = new HashMap<>();

    String versionId = UUID.randomUUID().toString();
    try (JcrDao.DaoResult<Version> result = dao
        .saveResource(repositoryId, artifactId, versionId, document, params)) {
      URI location = versionToPointer(result.getValue(), repositoryId).getHref();
      return succeed(location, HttpStatus.CREATED, HttpHeaders::setLocation);
    }
  }

  //*********************************************************************************************/
  //* Knowledge Artifact - Management APIs */
  //*********************************************************************************************/

  @Override
  public ResponseEntity<byte[]> getKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (JcrDao.DaoResult<Version> result = dao
        .getResource(repositoryId, artifactId, versionTag, deleted)) {
      Version version = result.getValue();

      return succeed(getDataFromNode(version));
    }
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (JcrDao.DaoResult<Version> ignored = dao
        .getResource(repositoryId, artifactId, versionTag, deleted)) {
      return succeed();
    }
  }

  public ResponseEntity<Void> enableKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    dao.enableResource(repositoryId, artifactId, versionTag);
    return succeed(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> setKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, byte[] document) {
    Map<String, String> params = new HashMap<>();

    try (JcrDao.DaoResult<Version> ignored = dao
        .saveResource(repositoryId, artifactId, versionTag, document, params)) {

      return succeed(HttpStatus.NO_CONTENT);
    }
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    if (Boolean.TRUE.equals(deleted)) {
      return notSupported();
    }
    dao.deleteResource(repositoryId, artifactId, versionTag);

    return succeed(HttpStatus.NO_CONTENT);
  }


  @Override
  /**
   * Destructor
   * Release all resources
   */
  public void destroy() {
    dao.shutdown();
  }


  private Pointer versionToPointer(Version version, String repositoryId) {
    try {
      String artifactId = version.getFrozenNode().getProperty("jcr:id").getString();
      String label = version.getContainingHistory().getVersionLabels(version)[0];

      Pointer pointer = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
      pointer.setHref(hrefBuilder.getArtifactHref(artifactId, label, repositoryId));

      URIIdentifier id = DatatypeHelper
          .uri(cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE), artifactId, label);
      pointer.setEntityRef(id);

      return pointer;
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }

  private Pointer artifactToPointer(Node node, String repositoryId) {
    try {
      String artifactId = node.getProperty("jcr:id").getString();

      Pointer pointer = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
      pointer.setHref(hrefBuilder.getSeriesHref(artifactId, repositoryId));

      URIIdentifier id = DatatypeHelper
          .uri("UUID:" + artifactId);
      pointer.setEntityRef(id);

      return pointer;
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }

  private byte[] getDataFromNode(Version node) {
    try {
      return IOUtils
          .toByteArray(node.getFrozenNode().getProperty(JCR_DATA).getBinary().getStream());
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }


}
