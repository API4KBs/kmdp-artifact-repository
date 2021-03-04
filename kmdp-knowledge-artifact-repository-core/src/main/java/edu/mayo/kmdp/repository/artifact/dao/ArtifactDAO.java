package edu.mayo.kmdp.repository.artifact.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ArtifactDAO {

  void shutdown();

  DaoResult<List<Artifact>> listResources(String repositoryId, Boolean deleted, Map<String, String> config);

  default DaoResult<List<Artifact>> listResources(String repositoryId, Boolean deleted) {
    return listResources(repositoryId, deleted, Collections.emptyMap());
  }

  DaoResult<ArtifactVersion> getResourceVersion(String repositoryId, UUID artifactId, String versionTag, Boolean deleted);

  DaoResult<Artifact> getResourceSeries(String repositoryId, UUID artifactId);

  DaoResult<List<ArtifactVersion>> getResourceVersions(String repositoryId, UUID artifactId, Boolean deleted);

  DaoResult<ArtifactVersion> getLatestResourceVersion(String repositoryId, UUID artifactId, Boolean deleted);

  void clear();

  void deleteResourceVersion(String repositoryId, UUID artifactId, String versionTag);

  void deleteResourceSeries(String repositoryId, UUID artifactId);

  void enableResourceVersion(String repositoryId, UUID artifactId, String versionTag);

  void enableResourceSeries(String repositoryId, UUID artifactId);

  DaoResult<ArtifactVersion> saveResource(
      String repositoryId,
      UUID artifactId, String versionTag,
      byte[] document,
      Map<String,String> config);

  default DaoResult<ArtifactVersion> saveResource(
      String repositoryId,
      UUID artifactId, String versionTag,
      byte[] document) {
    return saveResource(repositoryId, artifactId, versionTag, document, Collections.emptyMap());
  }

  DaoResult<Artifact> saveResource(String repositoryId, UUID artifactId);

  byte[] getData(String repositoryId, ArtifactVersion version);
}
