package edu.mayo.kmdp.repository.artifact.jpa.entities;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Embeddable;

@Embeddable
public class KeyId implements Serializable {

  private String repositoryId;
  private UUID artifactId;
  private String versionTag;

  public KeyId() {
    // empty constructor
  }

  public KeyId(String repositoryId, UUID artifactId, String versionTag) {
    this.repositoryId = repositoryId;
    this.artifactId = artifactId;
    this.versionTag = versionTag;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public UUID getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(UUID uuid) {
    this.artifactId = uuid;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }


  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other instanceof KeyId) {
      KeyId otherkey = (KeyId) other;
      return this.getRepositoryId().equals(otherkey.getRepositoryId())
          && this.getArtifactId().equals(otherkey.getArtifactId())
          && this.getVersionTag().equals(otherkey.getVersionTag());
    }
    return false;
  }

  public int hashCode() {
    int result = 31 + getArtifactId().hashCode();
    result = 31 * result + getRepositoryId().hashCode();
    return 31 * result + getVersionTag().hashCode();
  }

  @Override
  public String toString() {
    return "##" + artifactId + ":" + versionTag;
  }
}
