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


import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.unsupported;

import edu.mayo.kmdp.repository.artifact.ClearableKnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.HrefBuilder;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceIdentificationException;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
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
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.PlatformComponentHelper;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.repository.resources.KnowledgeArtifactRepository;
import org.springframework.beans.factory.DisposableBean;

@KPServer
public class JcrKnowledgeArtifactRepository implements DisposableBean,
    ClearableKnowledgeArtifactRepositoryService {

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

  private Optional<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> createRepositoryDescriptor(
      String repositoryId, String repositoryName) {
    return PlatformComponentHelper.repositoryDescr(
        cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE),
        repositoryId,
        repositoryName,
        cfg.getTyped(KnowledgeArtifactRepositoryOptions.SERVER_HOST)
    ).map(descr ->
        descr.withDefaultRepository(defaultRepositoryId.equals(repositoryId)));
  }

  public void shutdown() {
    dao.shutdown();
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Repository - Management APIs */
  //*********************************************************************************************/

  @Override
  public Answer<List<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository>> listKnowledgeArtifactRepositories() {
    return createRepositoryDescriptor(defaultRepositoryId,defaultRepositoryName)
        .map(descr -> Answer.of(singletonList(descr)))
        .orElseGet(Answer::notFound);
  }

  @Override
  public Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> initKnowledgeArtifactRepository() {
    return unsupported();
  }

  @Override
  public Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> setKnowledgeArtifactRepository(
      String repositoryId,
      org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository repositoryDescr) {
    return unsupported();
  }

  @Override
  public Answer<Void> isKnowledgeArtifactRepository(String repositoryId) {
    return unsupported();
  }

  @Override
  public Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> getKnowledgeArtifactRepository(
      String repositoryId) {
    return Answer.of(defaultRepositoryId.equals(repositoryId)
        ? createRepositoryDescriptor(repositoryId, defaultRepositoryName)
        : Optional.empty());
  }

  @Override
  public Answer<Void> disableKnowledgeArtifactRepository(String repositoryId) {
    return unsupported();
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Series - Management APIs */
  //*********************************************************************************************/

  @Override
  public Answer<List<Pointer>> listKnowledgeArtifacts(String repositoryId, Integer offset,
      Integer limit, Boolean deleted) {
    try (JcrDao.DaoResult<List<Node>> result = dao
        .getResources(repositoryId, deleted, new HashMap<>())) {
      List<Node> nodes = result.getValue();

      List<Pointer> pointers = nodes.stream()
          .map(node -> artifactToPointer(node, repositoryId))
          .collect(Collectors.toList());

      return Answer.of(pointers);
    }
  }

  @Override
  public Answer<UUID> initKnowledgeArtifact(String repositoryId) {
    UUID artifactId = UUID.randomUUID();

    try (JcrDao.DaoResult<Node> ignored = dao
        .saveResource(repositoryId, artifactId)) {
      return Answer.of(ResponseCodeSeries.Created, artifactId);
    }
  }

  @Override
  public Answer<Void> clearKnowledgeRepository(String repositoryId,
      Boolean deleted) {
    return unsupported();
  }

  @Override
  public Answer<byte[]> getLatestKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    try (JcrDao.DaoResult<Version> result = dao
        .getLatestResource(repositoryId, artifactId, deleted)) {
      Version version = result.getValue();
      return Answer.of(getDataFromNode(version));
    }
  }

  @Override
  public Answer<Void> isKnowledgeArtifactSeries(String repositoryId, UUID artifactId,
      Boolean deleted) {
    try (JcrDao.DaoResult<List<Version>> ignored = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      return Answer.of();
    }
  }

  @Override
  public Answer<Void> enableKnowledgeArtifact(String repositoryId,
      UUID artifactId) {
    dao.enableResource(repositoryId, artifactId);
    return Answer.of(ResponseCodeSeries.Created);
  }

  @Override
  public Answer<Void> deleteKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    if ((Boolean.TRUE.equals(deleted))) {
      return unsupported();
    }
    dao.deleteResource(repositoryId, artifactId);
    return Answer.of(ResponseCodeSeries.NoContent);
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeArtifactSeries(String repositoryId,
      UUID artifactId, Boolean deleted, Integer offset, Integer limit,
      String beforeTag, String afterTag, String sort) {
    try (JcrDao.DaoResult<List<Version>> result = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      List<Version> versions = result.getValue();

      return versions.isEmpty()
          ? Answer.of(Collections.emptyList())
          : Answer.of(versions.stream()
              .map(version -> versionToPointer(version, repositoryId))
              .collect(Collectors.toList()));
    }
  }

  @Override
  public Answer<Void> addKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      byte[] document) {
    Map<String, String> params = new HashMap<>();

    String versionId = UUID.randomUUID().toString();
    try (JcrDao.DaoResult<Version> result = dao
        .saveResource(repositoryId, artifactId, versionId, document, params)) {
      URI location = versionToPointer(result.getValue(), repositoryId).getHref();
      return Answer.referTo(location,true);
    }
  }

  //*********************************************************************************************/
  //* Knowledge Artifact - Management APIs */
  //*********************************************************************************************/

  @Override
  public Answer<byte[]> getKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (JcrDao.DaoResult<Version> result = dao
        .getResource(repositoryId, artifactId, versionTag, deleted)) {
      Version version = result.getValue();

      return Answer.of(getDataFromNode(version));
    }
  }

  @Override
  public Answer<Void> isKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (JcrDao.DaoResult<Version> ignored = dao
        .getResource(repositoryId, artifactId, versionTag, deleted)) {
      return Answer.of(ResponseCodeSeries.OK);
    }
  }

  public Answer<Void> enableKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    dao.enableResource(repositoryId, artifactId, versionTag);
    return Answer.of(ResponseCodeSeries.NoContent);
  }

  @Override
  public Answer<Void> setKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, byte[] document) {
    Map<String, String> params = new HashMap<>();

    try (JcrDao.DaoResult<Version> ignored = dao
        .saveResource(repositoryId, artifactId, versionTag, document, params)) {

      return Answer.of(ResponseCodeSeries.NoContent);
    }
  }

  @Override
  public Answer<Void> deleteKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    if (Boolean.TRUE.equals(deleted)) {
      return unsupported();
    }
    dao.deleteResource(repositoryId, artifactId, versionTag);

    return Answer.of(ResponseCodeSeries.NoContent);
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
      String versionTag = version.getContainingHistory().getVersionLabels(version)[0];

      return (Pointer) SemanticIdentifier.newIdAsPointer(
          URI.create(cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE)),
          artifactId, "",
          versionTag, hrefBuilder.getArtifactHref(artifactId, versionTag, repositoryId));
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }

  private Pointer artifactToPointer(Node node, String repositoryId) {
    try {
      String artifactId = node.getProperty("jcr:id").getString();
      Pointer pointer = (Pointer) SemanticIdentifier.newIdAsPointer(artifactId);
      pointer.withHref(hrefBuilder.getSeriesHref(artifactId, repositoryId));

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

  @Override
  public void clear() {
    this.dao.clear();
  }
}
