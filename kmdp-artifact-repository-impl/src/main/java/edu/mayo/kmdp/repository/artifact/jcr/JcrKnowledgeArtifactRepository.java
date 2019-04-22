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

import static edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository.JcrTypes.KNOWLEDGE_ARTIFACT;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.repository.artifact.HrefBuilder;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.jcr.version.Version;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.omg.spec.api4kp._1_0.PlatformComponentHelper;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
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

  public enum JcrTypes {
    KNOWLEDGE_ARTIFACT(byte[].class);

    private final Class type;

    JcrTypes(final Class type) {
      this.type = type;
    }

    public Class getType() {
      return type;
    }
  }

  public JcrDao dao;

  private Map<String, KnowledgeArtifactRepository> descriptors = new ConcurrentHashMap<>();

  //*********************************************************************************************/
  //* Constructors */
  //*********************************************************************************************/

  public static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository transientRepository(
      String configFile, String folderPath, KnowledgeArtifactRepositoryServerConfig cfg)
      throws ConfigurationException {

    TransientRepository transientRepo = new TransientRepository(RepositoryConfig.create(
        JcrKnowledgeArtifactRepository.class.getResourceAsStream(configFile),
        folderPath));

    return new JcrKnowledgeArtifactRepository(
        new JcrDao(transientRepo,
            transientRepo::shutdown,
            Collections.singletonList(KNOWLEDGE_ARTIFACT.name()),
            cfg),
        cfg);
  }

  public JcrKnowledgeArtifactRepository(javax.jcr.Repository delegate,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this(delegate, null, cfg);
  }

  // used for testing purposes
  public JcrKnowledgeArtifactRepository(JcrDao dao, KnowledgeArtifactRepositoryServerConfig cfg) {
    this.cfg = cfg;
    hrefBuilder = new HrefBuilder(cfg);
    this.dao = dao;
    init();
  }

  public JcrKnowledgeArtifactRepository(javax.jcr.Repository delegate, Runnable cleanup,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this(new JcrDao(delegate,
        cleanup,
        Arrays.stream(JcrTypes.values()).map(Enum::name).collect(Collectors.toList()),
        cfg), cfg);
  }

  private void init() {
    String defaultRepositoryId = cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    String defaultRepositoryName = cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME);
    createRepositoryDescriptor(defaultRepositoryId, defaultRepositoryName).ifPresent((d) ->
        descriptors.put(defaultRepositoryId, d));
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
    return new ResponseEntity<>(new ArrayList<>(descriptors.values()),HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> initKnowledgeArtifactRepository(KnowledgeArtifactRepository descr) {
    VersionIdentifier vid = DatatypeHelper.toVersionIdentifier(descr.getId());
    return enableRepository(vid != null ? vid.getTag() : null, descr);
  }

  @Override
  public ResponseEntity<Void> setKnowledgeArtifactRepository(String repositoryId,
      KnowledgeArtifactRepository repositoryDescr) {
    return enableRepository(repositoryId, repositoryDescr);
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactRepository(String repositoryId) {
    return isKnowledgeArtifactRepositoryEnabled(repositoryId)
        ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
        : new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @Override
  public ResponseEntity<KnowledgeArtifactRepository> getKnowledgeArtifactRepository(
      String repositoryId) {
    return isKnowledgeArtifactRepositoryEnabled(repositoryId)
        ? new ResponseEntity<>(descriptors.get(repositoryId), HttpStatus.OK)
        : new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactRepository(String repositoryId) {
    if (isKnowledgeArtifactRepositoryEnabled(repositoryId)) {
      if (! isDefaultRepository(repositoryId) && dao.isEmpty(repositoryId)) {
        return disableRepository(repositoryId);
      } else {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
      }
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private boolean isDefaultRepository(String repositoryId) {
    return cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID)
        .equals(repositoryId);
  }


  protected boolean isKnowledgeArtifactRepositoryEnabled(String repositoryId) {
    return descriptors.containsKey(repositoryId);
  }

  protected ResponseEntity<Void> disableRepository(String repositoryId) {
    descriptors.remove(repositoryId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  protected ResponseEntity<Void> enableRepository(String repositoryId,
      KnowledgeArtifactRepository repositoryDescr) {
    if (Util.isEmpty(repositoryId) || repositoryDescr == null || repositoryDescr.getId() == null) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (isKnowledgeArtifactRepositoryEnabled(repositoryId)) {
      descriptors.put(repositoryId, repositoryDescr);
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      descriptors.put(repositoryId, repositoryDescr);
      dao.init(repositoryId);
      return new ResponseEntity<>(HttpStatus.CREATED);
    }
  }




  /*********************************************************************************************/

  @Override
  public ResponseEntity<Void> addKnowledgeArtifactVersion(String repositoryId, String artifactId,
      byte[] document) {
    Map<String, String> params = new HashMap<>();

    String versionId = UUID.randomUUID().toString();

    try (JcrDao.DaoResult<Version> ignored = this.dao
        .saveResource(KNOWLEDGE_ARTIFACT.name(), repositoryId, artifactId, versionId, document, params)) {
      return new ResponseEntity<>(HttpStatus.OK);
    }
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactSeries(String repositoryId,
      String artifactId) {
    this.dao.deleteResource(KNOWLEDGE_ARTIFACT.name(), repositoryId, artifactId);

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactVersion(String repositoryId, String artifactId,
      String versionTag) {
    this.dao.deleteResource(KNOWLEDGE_ARTIFACT.name(), repositoryId, artifactId, versionTag);

    return new ResponseEntity<>(HttpStatus.OK);
  }


  @Override
  public ResponseEntity<byte[]> getLatestKnowledgeArtifact(String repositoryId, String artifactId) {
    return null;
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeArtifactSeries(String repositoryId,
      String artifactId, Integer offset, Integer limit, String beforeTag, String afterTag,
      String sort) {
    try (JcrDao.DaoResult<List<Version>> result = this.dao
        .getResourceVersions(KNOWLEDGE_ARTIFACT.name(), repositoryId, artifactId)) {
      List<Version> versions = result.getValue();

      return versions.isEmpty()
          ? ResponseEntity.notFound().build()
          : this.wrap(versions.stream()
              .map(this::versionToPointer)
              .collect(Collectors.toList()));
    }
  }

  @Override
  public ResponseEntity<Void> setKnowledgeArtifactSeries(String repositoryId, String artifactId) {
    return null;
  }

  @Override
  public ResponseEntity<byte[]> getKnowledgeArtifactVersion(String repositoryId, String artifactId,
      String versionTag) {
    try (JcrDao.DaoResult<Version> result = this.dao
        .getResource(KNOWLEDGE_ARTIFACT.name(), repositoryId, artifactId, versionTag)) {
      Version version = result.getValue();
      byte[] bytes = this.getDataFromNode(version);

      // TODO: set mime type
      HttpHeaders headers = new HttpHeaders();

      return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
  }


  @Override
  public ResponseEntity<Void> initKnowledgeArtifactSeries(String repositoryId) {
    return null;
  }


  @Override
  public ResponseEntity<Void> isKnowledgeArtifactSeries(String repositoryId, String artifactId) {
    return null;
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactVersion(String repositoryId, String artifactId,
      String versionTag) {
    return null;
  }


  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeArtifacts(String repositoryId, Integer offset,
      Integer limit) {
    try (JcrDao.DaoResult<List<Version>> result = this.dao
        .getResources(KNOWLEDGE_ARTIFACT.name(), repositoryId, new HashMap<>())) {
      List<Version> versions = result.getValue();

      List<Pointer> pointers = versions.stream()
          .map(this::versionToPointer)
          .collect(Collectors.toList());

      return this.wrap(pointers);
    }
  }


  @Override
  public ResponseEntity<Void> setKnowledgeArtifactVersion(String repositoryId, String artifactId,
      String versionTag, byte[] document) {
    Map<String, String> params = new HashMap<>();

    try (JcrDao.DaoResult<Version> ignored = this.dao
        .saveResource(KNOWLEDGE_ARTIFACT.name(), repositoryId, artifactId, versionTag, document, params)) {

      return new ResponseEntity<>(HttpStatus.OK);
    }
  }


  @Override
  /**
   * Destructor
   * Release all resources
   */
  public void destroy() {
    this.dao.shutdown();
  }


  private Pointer versionToPointer(Version version) {
    try {
      String artifactId = version.getFrozenNode().getProperty("jcr:id").getString();
      String label = version.getContainingHistory().getVersionLabels(version)[0];

      Pointer pointer = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
      pointer.setHref(this.hrefBuilder.getArtifactHref(artifactId, label));

      URIIdentifier id = DatatypeHelper
          .uri(cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE), artifactId, label);
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
