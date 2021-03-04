package edu.mayo.kmdp.repository.artifact.jcr;

import static edu.mayo.kmdp.repository.artifact.jcr.JcrDao.JCR_DATA;
import static edu.mayo.kmdp.repository.artifact.jcr.JcrDao.JCR_ID;
import static edu.mayo.kmdp.repository.artifact.jcr.JcrDao.JCR_STATUS;
import static edu.mayo.kmdp.repository.artifact.jcr.JcrDao.STATUS_AVAILABLE;
import static edu.mayo.kmdp.repository.artifact.jcr.JcrDao.STATUS_UNAVAILABLE;

import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.dao.DaoResult;
import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceIdentificationException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import org.apache.commons.compress.utils.IOUtils;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;

public class JcrAdapter implements ArtifactDAO {

  private JcrDao innerDao;

  public JcrAdapter(JcrDao dao) {
    this.innerDao = dao;
  }

  public JcrAdapter(Repository delegate) {
    this(new JcrDao(delegate));
  }

  public JcrAdapter(Repository delegate, Runnable cleanup) {
    this(new JcrDao(delegate, cleanup));
  }

  @Override
  public void shutdown() {
    innerDao.shutdown();
  }

  @Override
  public DaoResult<List<Artifact>> listResources(String repositoryId, Boolean deleted,
      Map<String, String> config) {
    return innerDao.getResources(repositoryId, deleted, config)
        .map(nodes -> mapAll(nodes,this::toArtifact));
  }


  @Override
  public DaoResult<ArtifactVersion> getResourceVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    return innerDao.getResource(repositoryId,artifactId, versionTag, deleted)
        .map(this::toArtifactVersion);
  }

  @Override
  public DaoResult<Artifact> getResourceSeries(String repositoryId, UUID artifactId) {
    throw new UnsupportedOperationException("Unused");
  }

  @Override
  public DaoResult<List<ArtifactVersion>> getResourceVersions(String repositoryId, UUID artifactId,
      Boolean deleted) {
    return innerDao.getResourceVersions(repositoryId, artifactId, deleted)
        .map(nodes -> mapAll(nodes, this::toArtifactVersion));
  }

  @Override
  public DaoResult<ArtifactVersion> getLatestResourceVersion(String repositoryId, UUID artifactId,
      Boolean deleted) {
    return innerDao.getLatestResource(repositoryId, artifactId, deleted)
        .map(this::toArtifactVersion);
  }

  @Override
  public void clear() {
    innerDao.clear();
  }

  @Override
  public void deleteResourceVersion(String repositoryId, UUID artifactId, String versionTag) {
    innerDao.deleteResource(repositoryId, artifactId, versionTag);
  }

  @Override
  public void deleteResourceSeries(String repositoryId, UUID artifactId) {
    innerDao.deleteResource(repositoryId, artifactId);
  }

  @Override
  public void enableResourceVersion(String repositoryId, UUID artifactId, String versionTag) {
    innerDao.enableResource(repositoryId, artifactId, versionTag);
  }

  @Override
  public void enableResourceSeries(String repositoryId, UUID artifactId) {
    innerDao.enableResource(repositoryId,artifactId);
  }

  @Override
  public DaoResult<ArtifactVersion> saveResource(String repositoryId, UUID artifactId,
      String versionTag, byte[] document, Map<String, String> config) {
    return innerDao.saveResource(repositoryId, artifactId, versionTag, document, config)
        .map(this::toArtifactVersion);
  }

  @Override
  public DaoResult<Artifact> saveResource(String repositoryId, UUID artifactId) {
    return innerDao.saveResource(repositoryId, artifactId)
        .map(this::toArtifact);
  }


  @Override
  public byte[] getData(String repositoryId, ArtifactVersion version) {
    try {
      InputStream is = version.getDataStream();
      return IOUtils
          .toByteArray(is);
    } catch (Exception e) {
      throw new ResourceIdentificationException(e);
    }
  }

  private ArtifactVersion toArtifactVersion(Version version) {
    return new VersionAdapter(version);
  }

  private Artifact toArtifact(Node node) {
    return new NodeAdapter(node);
  }

  private <X,T> List<X> mapAll(List<T> source, Function<T,X> mapper) {
    return source.stream()
        .map(mapper)
        .collect(Collectors.toList());
  }

  public static class VersionAdapter implements ArtifactVersion {
    private Version version;

    public VersionAdapter(Version version) {
      this.version = version;
    }

    public Version getJCRVersion() {
      return version;
    }

    @Override
    public ResourceIdentifier getResourceIdentifier() {
      try {
        String artifactId = version.getFrozenNode().getProperty("jcr:id").getString();
        String versionTag = version.getContainingHistory().getVersionLabels(version)[0];
        return SemanticIdentifier.newId(artifactId,versionTag);
      } catch (RepositoryException e) {
        throw new IllegalStateException(e.getMessage(),e);
      }
    }

    @Override
    public boolean isUnavailable() throws DaoRuntimeException {
      try {
        return STATUS_UNAVAILABLE.equals(version.getFrozenNode().getProperty(JCR_STATUS).getString());
      } catch (RepositoryException e) {
        throw new DaoRuntimeException(e.getMessage(),e);
      }
    }

    @Override
    public boolean isAvailable() throws DaoRuntimeException {
      try {
        return STATUS_AVAILABLE.equals(version.getFrozenNode().getProperty(JCR_STATUS).getString());
      } catch (RepositoryException e) {
        throw new DaoRuntimeException(e.getMessage(),e);
      }
    }

    @Override
    public InputStream getDataStream() throws DaoRuntimeException {
      Node node = null;
      try {
        node = getJCRVersion().getFrozenNode();
        return node.getProperty(JCR_DATA).getBinary().getStream();
      } catch (RepositoryException e) {
        throw new DaoRuntimeException(e.getMessage(), e);
      }
    }
  }

  public static class NodeAdapter implements Artifact {
    private Node node;

    public NodeAdapter(Node node) {
      this.node = node;
    }

    public Node getJCRNode() {
      return node;
    }

    @Override
    public String getArtifactTag() {
      try {
        return node.getProperty(JCR_ID).getString();
      } catch (RepositoryException e) {
        throw new IllegalStateException(e.getMessage(),e);
      }
    }

    @Override
    public UUID getArtifactId() {
      return UUID.fromString(getArtifactTag());
    }

    @Override
    public boolean isUnavailable() {
      try {
        return STATUS_UNAVAILABLE.equals(node.getProperty(JCR_STATUS).getString());
      } catch (RepositoryException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public boolean isAvailable() {
      try {
        return STATUS_AVAILABLE.equals(node.getProperty(JCR_STATUS).getString());
      } catch (RepositoryException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
