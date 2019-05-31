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

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.repository.artifact.HrefBuilder;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.version.Version;
import org.apache.commons.io.IOUtils;
import org.omg.spec.api4kp._1_0.PlatformComponentHelper;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class JcrKnowledgeArtifactRepository implements DisposableBean,
    edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository {

  private static final String JCR_DATA = "jcr:data";

  private KnowledgeArtifactRepositoryServerConfig cfg;
  private HrefBuilder hrefBuilder;

  public JcrDao dao;

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
  }

  public JcrKnowledgeArtifactRepository(javax.jcr.Repository delegate, Runnable cleanup,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this(new JcrDao(delegate,
        cleanup,
        cfg), cfg);
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
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> initKnowledgeArtifactRepository() {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> setKnowledgeArtifactRepository(String repositoryId,
      KnowledgeArtifactRepository repositoryDescr) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactRepository(String repositoryId) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<KnowledgeArtifactRepository> getKnowledgeArtifactRepository(
      String repositoryId) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<Void> disableKnowledgeArtifactRepository(String repositoryId) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Series - Management APIs */
  //*********************************************************************************************/

  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeArtifacts(String  repositoryId, Integer  offset, Integer  limit, Boolean  deleted) {
    try (JcrDao.DaoResult<List<Node>> result = dao
            .getResources(repositoryId, deleted, new HashMap<>())) {
      List<Node> nodes = result.getValue();

      List<Pointer> pointers = nodes.stream()
              .map(node -> artifactToPointer(node, repositoryId))
              .collect(Collectors.toList());

      return wrap(pointers);
    }
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeArtifact(String repositoryId) {
    UUID artifactId = UUID.randomUUID();

    try (JcrDao.DaoResult<Node> ignored = dao
            .saveResource(repositoryId, artifactId)) {
      return new ResponseEntity<>(artifactId, HttpStatus.CREATED);
    }
  }

  @Override
  public ResponseEntity<Void> clearKnowledgeRepository( String  repositoryId,
                                                        Boolean  deleted) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<byte[]> getLatestKnowledgeArtifact(String  repositoryId, UUID  artifactId, Boolean  deleted) {
    try (JcrDao.DaoResult<Version> result = dao
            .getLatestResource(repositoryId, artifactId, deleted)) {
      Version version = result.getValue();
      return wrap(getDataFromNode(version));
    }
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactSeries(String  repositoryId, UUID  artifactId, Boolean  deleted) {
    try (JcrDao.DaoResult<List<Version>> ignored = dao
            .getResourceVersions(repositoryId, artifactId, deleted)) {
      return new ResponseEntity<>(HttpStatus.OK);
    }
  }

  @Override
  public ResponseEntity<Void> enableKnowledgeArtifact( String  repositoryId,
                                                        UUID  artifactId) {
    dao.enableResource(repositoryId, artifactId);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifact(String  repositoryId, UUID  artifactId, Boolean  deleted ) {
    if(deleted){
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
    dao.deleteResource(repositoryId, artifactId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeArtifactSeries(String  repositoryId, UUID  artifactId, Boolean  deleted, Integer  offset, Integer  limit,
                                                                  String  beforeTag, String  afterTag, String  sort) {
    try (JcrDao.DaoResult<List<Version>> result = dao
            .getResourceVersions(repositoryId, artifactId, deleted)) {
      List<Version> versions = result.getValue();

      return versions.isEmpty()
              ? wrap(Collections.emptyList())
              : wrap(versions.stream()
              .map(version -> versionToPointer(version, repositoryId))
              .collect(Collectors.toList()));
    }
  }

  @Override
  public ResponseEntity<Void> addKnowledgeArtifactVersion(String repositoryId, UUID artifactId, byte[] document) {
    //TODO success should return location of new version
    Map<String, String> params = new HashMap<>();

    String versionId = UUID.randomUUID().toString();
    try (JcrDao.DaoResult<Version> ignored = dao
        .saveResource(repositoryId, artifactId, versionId, document, params)) {
      return new ResponseEntity<>(HttpStatus.OK);
    }
  }

  //*********************************************************************************************/
  //* Knowledge Artifact - Management APIs */
  //*********************************************************************************************/

  @Override
  public ResponseEntity<byte[]> getKnowledgeArtifactVersion(String  repositoryId, UUID  artifactId, String  versionTag, Boolean  deleted) {
    try (JcrDao.DaoResult<Version> result = dao
            .getResource(repositoryId, artifactId, versionTag, deleted)) {
      Version version = result.getValue();

      return wrap(getDataFromNode(version));

      // TODO: set mime type
    }
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactVersion(String  repositoryId, UUID  artifactId, String  versionTag, Boolean  deleted) {
    try (JcrDao.DaoResult<Version> ignored = dao
            .getResource(repositoryId, artifactId, versionTag, deleted)) {
      return new ResponseEntity<>(HttpStatus.OK);
    }
  }

  public ResponseEntity<Void> enableKnowledgeArtifactVersion( String  repositoryId, UUID  artifactId, String  versionTag, Boolean  deleted) {
    dao.enableResource(repositoryId, artifactId, versionTag);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> setKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
                                                          String versionTag, byte[] document) {
    Map<String, String> params = new HashMap<>();

    try (JcrDao.DaoResult<Version> ignored = dao
        .saveResource(repositoryId, artifactId, versionTag, document, params)) {

      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactVersion(String  repositoryId, UUID  artifactId, String  versionTag, Boolean  deleted) {
    if(deleted){
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
    dao.deleteResource(repositoryId, artifactId, versionTag);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
      throw new RuntimeException(e);
    }
  }
  private Pointer artifactToPointer(Node node, String repositoryId) {
    try {
      String artifactId = node.getProperty("jcr:id").getString();

      Pointer pointer = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
      pointer.setHref(hrefBuilder.getSeriesHref(artifactId, repositoryId));

      URIIdentifier id = DatatypeHelper
              .uri("UUID:"+artifactId);
      pointer.setEntityRef(id);

      return pointer;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] getDataFromNode(Version node) {
    try {
      return IOUtils
          .toByteArray(node.getFrozenNode().getProperty(JCR_DATA).getBinary().getStream());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private <T> ResponseEntity<T> wrap(T resource) {
    return new ResponseEntity<T>(resource, HttpStatus.OK);
  }

}
