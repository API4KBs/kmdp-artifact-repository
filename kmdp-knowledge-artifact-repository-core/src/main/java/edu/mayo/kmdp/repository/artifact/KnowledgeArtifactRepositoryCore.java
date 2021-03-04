/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.artifact;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.unsupported;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.dao.DaoResult;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceIdentificationException;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.PlatformComponentHelper;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.springframework.beans.factory.DisposableBean;


public abstract class KnowledgeArtifactRepositoryCore implements DisposableBean,
    ClearableKnowledgeArtifactRepositoryService {

  protected KnowledgeArtifactRepositoryServerConfig cfg;
  protected HrefBuilder hrefBuilder;

  protected String defaultRepositoryId;
  protected String defaultRepositoryName;

  protected ArtifactDAO dao;

  //*********************************************************************************************/
  //* Constructors */
  //*********************************************************************************************/


  public KnowledgeArtifactRepositoryCore(ArtifactDAO dao,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this.cfg = cfg;
    hrefBuilder = new HrefBuilder(cfg);
    this.dao = dao;
    this.defaultRepositoryId = cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    this.defaultRepositoryName = cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME);
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
    return createRepositoryDescriptor(defaultRepositoryId, defaultRepositoryName)
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
    try (DaoResult<List<Artifact>> result = dao
        .listResources(repositoryId, deleted)) {
      List<Artifact> nodes = result.getValue();

      List<Pointer> pointers = nodes.stream()
          .map(node -> artifactToPointer(node, repositoryId))
          .collect(Collectors.toList());

      return Answer.of(pointers);
    }
  }

  @Override
  public Answer<UUID> initKnowledgeArtifact(String repositoryId) {
    UUID artifactId = UUID.randomUUID();

    try (DaoResult<Artifact> ignored = dao
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
    try (DaoResult<ArtifactVersion> result = dao
        .getLatestResourceVersion(repositoryId, artifactId, deleted)) {
      ArtifactVersion version = result.getValue();
      return Answer.of(getData(repositoryId, version));
    }
  }

  @Override
  public Answer<Void> isKnowledgeArtifactSeries(String repositoryId, UUID artifactId,
      Boolean deleted) {
    try (DaoResult<List<ArtifactVersion>> ignored = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      return Answer.of();
    }
  }

  @Override
  public Answer<Void> enableKnowledgeArtifact(String repositoryId,
      UUID artifactId) {
    dao.enableResourceSeries(repositoryId, artifactId);
    return Answer.of(ResponseCodeSeries.Created);
  }

  @Override
  public Answer<Void> deleteKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    if ((Boolean.TRUE.equals(deleted))) {
      return unsupported();
    }
    dao.deleteResourceSeries(repositoryId, artifactId);
    return Answer.of(ResponseCodeSeries.NoContent);
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeArtifactSeries(String repositoryId,
      UUID artifactId, Boolean deleted, Integer offset, Integer limit,
      String beforeTag, String afterTag, String sort) {
    try (DaoResult<List<ArtifactVersion>> result = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      List<ArtifactVersion> versions = result.getValue();

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
    String versionId = UUID.randomUUID().toString();

    try (DaoResult<ArtifactVersion> result = dao
        .saveResource(repositoryId, artifactId, versionId, document, emptyMap())) {
      URI location = versionToPointer(result.getValue(), repositoryId).getHref();
      return Answer.referTo(location, true);
    }
  }

  //*********************************************************************************************/
  //* Knowledge Artifact - Management APIs */
  //*********************************************************************************************/

  @Override
  public Answer<byte[]> getKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (DaoResult<ArtifactVersion> result = dao
        .getResourceVersion(repositoryId, artifactId, versionTag, deleted)) {
      ArtifactVersion version = result.getValue();

      return Answer.of(getData(repositoryId, version));
    }
  }

  @Override
  public Answer<Void> isKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (DaoResult<ArtifactVersion> ignored = dao
        .getResourceVersion(repositoryId, artifactId, versionTag, deleted)) {
      return Answer.of(ResponseCodeSeries.OK);
    }
  }

  public Answer<Void> enableKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    dao.enableResourceVersion(repositoryId, artifactId, versionTag);
    return Answer.of(ResponseCodeSeries.NoContent);
  }

  @Override
  public Answer<Void> setKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, byte[] document) {
    try (DaoResult<ArtifactVersion> ignored = dao
        .saveResource(repositoryId, artifactId, versionTag, document, emptyMap())) {

      return Answer.of(ResponseCodeSeries.NoContent);
    }
  }

  @Override
  public Answer<Void> deleteKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    if (Boolean.TRUE.equals(deleted)) {
      return unsupported();
    }
    dao.deleteResourceVersion(repositoryId, artifactId, versionTag);

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


  private Pointer versionToPointer(ArtifactVersion version, String repositoryId) {
    try {
      ResourceIdentifier resId = version.getResourceIdentifier();
      String artifactId = resId.getTag();
      String versionTag = resId.getVersionTag();

      return SemanticIdentifier.newIdAsPointer(
          URI.create(cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE)),
          artifactId, "",
          versionTag, hrefBuilder.getArtifactHref(artifactId, versionTag, repositoryId));
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }

  protected Pointer artifactToPointer(Artifact node, String repositoryId) {
    try {
      String artifactId = node.getArtifactTag();
      Pointer pointer = SemanticIdentifier.newIdAsPointer(artifactId);
      pointer.withHref(hrefBuilder.getSeriesHref(artifactId, repositoryId));

      return pointer;
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }

  protected byte[] getData(String repositoryId, ArtifactVersion version) {
    ResourceIdentifier vid = version.getResourceIdentifier();
    return dao.getData(repositoryId, version);
  }

  @Override
  public void clear() {
    this.dao.clear();
  }
}
