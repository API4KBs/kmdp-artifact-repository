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

import static javax.jcr.nodetype.NodeType.MIX_VERSIONABLE;

import com.google.common.collect.Sets;
import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import edu.mayo.kmdp.repository.artifact.exceptions.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNoContentException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrDao {

  Logger logger = LoggerFactory.getLogger(JcrDao.class);

  protected static final String JCR_ID = "jcr:id";
  protected static final String JCR_DATA = "jcr:data";
  protected static final String JCR_STATUS = "status";
  protected static final String JCR_SERIES_STATUS = "seriesStatus";
  protected static final String STATUS_AVAILABLE = "available";
  protected static final String STATUS_UNAVAILABLE = "unavailable";

  private final Set<String> nodesNotToDelete = Sets.newHashSet("rep:security", "jcr:system", "oak:index");

  private javax.jcr.Repository delegate;

  private Runnable cleanup;

  public JcrDao(javax.jcr.Repository delegate) {
    this(delegate, null);
  }


  public JcrDao(javax.jcr.Repository delegate, Runnable cleanup) {
    this.delegate = delegate;
    this.cleanup = cleanup;
  }

  public <T> DaoResult<T> execute(Function<Session, T> f) {
    try {
      Session session = delegate.login(
          new SimpleCredentials("admin", "admin".toCharArray())
      );
      T result = f.apply(session);
      session.save();
      return new DaoResult<>(result, session);
    } catch (RepositoryException e) {
      throw new DaoRuntimeException(e);
    }
  }

  public DaoResult<List<Version>> getResourceVersions(String repositoryId,
      UUID uuid, Boolean deleted) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    return execute((Session session) -> {
      try {
        Node rootNode = session.getRootNode();
        if (artifactSeriesExists(rootNode, encodedRepositoryId, id)) {

          Node resource = session.getRootNode().getNode(encodedRepositoryId).getNode(id);
          if ((Boolean.FALSE.equals(deleted)) && resource.getProperty(JCR_SERIES_STATUS).getString()
              .equals(STATUS_UNAVAILABLE)) {
            throw new ResourceNoContentException("Artifact known, but not available.");
          }
          return getArtifactVersions(session, resource, deleted);
        } else {
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException | ResourceNoContentException e) {
        session.logout();
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
    });
  }

  private List<Version> getArtifactVersions(Session session, Node resource,
      Boolean deleted) throws RepositoryException {
    VersionHistory history = session.getWorkspace().getVersionManager()
        .getVersionHistory(resource.getPath());
    String[] versions = history.getVersionLabels();

    return Arrays.stream(versions)
        .map(label -> {
          try {
            return history.getVersionByLabel(label);
          } catch (RepositoryException e) {
            throw new DaoRuntimeException(e);
          }
        })
        .filter(version -> {
          //If deleted != true, filter out unavailable versions
          try {
            return deleted || !versionIsUnavailable(version);
          } catch (RepositoryException e) {
            throw new DaoRuntimeException(e);
          }
        })

        .collect(Collectors.toList());
  }

  public DaoResult<Version> getLatestResource(String repositoryId, UUID id, Boolean deleted) {
    DaoResult<List<Version>> result = getResourceVersions(repositoryId, id, deleted);
    List<Version> versions = result.getValue();
    if (versions.isEmpty()) {
      result.close();
      throw new ResourceNoContentException(
          "Artifact known but either not available, or no versions are available");
    }
    versions.sort((Version v1, Version v2) -> {
      try {
        return v2.getCreated().compareTo(v1.getCreated());
      } catch (RepositoryException e) {
        result.close();
        throw new DaoRuntimeException(e);
      }
    });
    return new DaoResult<>(versions.get(0), result.getSession());
  }

  public DaoResult<Version> getResource(String repositoryId, UUID uuid,
      String version, boolean getUnavailable) {
    String encodedRepositoryId = this.encode(repositoryId);

    String id = this.encode(uuid.toString());

    return execute((Session session) -> {
      try {
        Node rootNode = session.getRootNode();
        if (!artifactSeriesExists(rootNode, encodedRepositoryId, id)) {
          throw new ResourceNotFoundException();
        }
        Node resource = session.getRootNode().getNode(encodedRepositoryId).getNode(id);
        VersionHistory history = session.getWorkspace().getVersionManager()
            .getVersionHistory(resource.getPath());
        if (!history.hasVersionLabel(version)) {
          throw new ResourceNotFoundException();
        }
        if (!getUnavailable && versionIsUnavailable(history.getVersionByLabel(version))) {
          throw new ResourceNoContentException("The version is known but currently unavailable.");
        }
        return history.getVersionByLabel(version);
      } catch (ResourceNoContentException | ResourceNotFoundException e) {
        session.logout();
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
    });
  }

  public DaoResult<List<Node>> getResources(String repositoryId,
      Boolean deleted, Map<String, String> query) {
    String encodedRepositoryId = this.encode(repositoryId);

    return execute((Session session) -> {
      try {
        if (!session.getRootNode().hasNode(encodedRepositoryId)) {
          throw new RepositoryNotFoundException(encodedRepositoryId);
        }
        String queryString;
        if (query.isEmpty()) {
          queryString = "";
        } else {
          String q = query.entrySet().stream()
              .map(entry -> String.format("jcr:%s='%s'",
                  ISO9075.encode(this.encode(entry.getKey())), this.encode(ISO9075.encode(entry.getValue()))))
              .collect(Collectors.joining(" AND "));
          queryString = "[" + q + "]";
        }
        QueryResult queryResult = session.getWorkspace().getQueryManager()
            .createQuery(String.format("//%s/*%s", ISO9075.encode(encodedRepositoryId), queryString), "xpath")
            .execute();

        NodeIterator nodes = queryResult.getNodes();
        List<Node> result = new ArrayList<>();
        while (nodes.hasNext()) {
          Node node = nodes.nextNode();
          if (Boolean.FALSE.equals(deleted) && node.hasProperty(JCR_SERIES_STATUS) && node
              .getProperty(JCR_SERIES_STATUS)
              .getString().equals(STATUS_UNAVAILABLE)) {
            continue;
          }
          result.add(node);
        }
        return result;
      } catch (RepositoryNotFoundException e) {
        session.logout();
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
    });

  }

  private String encode(String id) {
    return Text.escapeIllegalJcrChars(id);
  }

  public void deleteResource(String repositoryId, UUID uuid) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    try (DaoResult<?> ignored = execute((Session session) -> {
      try {
        Node rootNode = session.getRootNode();
        if (artifactSeriesExists(rootNode, encodedRepositoryId, id)) {
          Node resource = session.getRootNode().getNode(encodedRepositoryId).getNode(id);
          VersionManager versionManager = session.getWorkspace().getVersionManager();
          VersionHistory history = session.getWorkspace().getVersionManager()
              .getVersionHistory(resource.getPath());
          String[] labels = history.getVersionLabels();
          versionManager.checkout(resource.getPath());
          resource.setProperty(JCR_SERIES_STATUS, STATUS_UNAVAILABLE);
          session.save();
          for (String label : labels) {
            //Assign each version as 'unavailable'
            versionManager.checkout(resource.getPath());
            resource.setProperty(JCR_STATUS, STATUS_UNAVAILABLE);
            session.save();
            Version newNode = versionManager.checkin(resource.getPath());
            versionManager.getVersionHistory(resource.getPath())
                .addVersionLabel(newNode.getName(), label, true);
          }
        } else {
          session.logout();
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }

      return null;
    })) {
      //nothing to do here
    }
  }

  public void deleteResource(String repositoryId, UUID uuid, String version) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    try (DaoResult<?> ignored = execute((Session session) -> {
      try {
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        Node rootNode = session.getRootNode();
        if (artifactSeriesExists(rootNode, encodedRepositoryId, id)) {
          Node node = session.getRootNode().getNode(encodedRepositoryId).getNode(id);
          if (versionManager.getVersionHistory(node.getPath()).hasVersionLabel(version)) {
            versionManager.checkout(node.getPath());
            node.setProperty(JCR_STATUS, STATUS_UNAVAILABLE);
            session.save();
            Version newNode = versionManager.checkin(node.getPath());
            versionManager.getVersionHistory(node.getPath())
                .addVersionLabel(newNode.getName(), version, true);
            return null;
          } else {
            throw new ResourceNotFoundException();
          }
        } else {
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e) {
        session.logout();
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
    })) {
      // nothing to do
    }
  }

  public void saveResource(String repositoryId, UUID id, String version,
      byte[] payload) {
    try (DaoResult<?> ignored = saveResource(repositoryId, id, version, payload, new HashMap<>())) {
      // nothing to do
    }

  }

  public DaoResult<Version> saveResource(String repositoryId, UUID uuid,
      String version,
      byte[] payload, Map<String, String> metadata) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    return execute((Session session) -> {
      try {
        long t0;
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        // check if repository node exists
        if (!session.getRootNode().hasNode(encodedRepositoryId)) {
          session.getRootNode().addNode(encodedRepositoryId);
        }

        Node assetNode = session.getRootNode().getNode(encodedRepositoryId);

        Node node;
        if (assetNode.hasNode(id)) {
          node = assetNode.getNode(id);
          versionManager.checkout(node.getPath());
        } else {
          node = assetNode.addNode(id);
          node.addMixin(MIX_VERSIONABLE);
          node.setProperty("jcr:id", id);
        }

        node.setProperty(JCR_DATA,
            session.getValueFactory().createBinary(new ByteArrayInputStream(payload)));
        node.setProperty(JCR_STATUS, STATUS_AVAILABLE);
        node.setProperty(JCR_SERIES_STATUS, STATUS_AVAILABLE);
        if (metadata != null) {
          metadata.forEach((key, value) -> {
            try {
              node.setProperty("jcr:" + key, value);
            } catch (RepositoryException e) {
              throw new DaoRuntimeException(e);
            }
          });
        }
        session.save();
        Version newNode = versionManager.checkin(node.getPath());

        versionManager.getVersionHistory(node.getPath())
            .addVersionLabel(newNode.getName(), version, true);
        return newNode;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
    });
  }

  public DaoResult<Node> saveResource(String repositoryId, UUID uuid) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    return execute((Session session) -> {
      try {
        // check if repository node exists, if not, add it
        if (!session.getRootNode().hasNode(encodedRepositoryId)) {
          session.getRootNode().addNode(encodedRepositoryId);
        }
        Node assetNode = session.getRootNode().getNode(encodedRepositoryId);
        Node node = assetNode.addNode(id);
        node.addMixin(MIX_VERSIONABLE);
        node.setProperty("jcr:id", id);
        node.setProperty(JCR_SERIES_STATUS, STATUS_AVAILABLE);
        session.save();

        return node;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
    });
  }

  public void enableResource(String repositoryId, UUID uuid) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    try (DaoResult<?> ignored = execute((Session session) -> {
      try {
        if (session.getRootNode().hasNode(encodedRepositoryId)) {
          if (session.getRootNode().getNode(encodedRepositoryId).hasNode(id)) {
            Node node = session.getRootNode().getNode(encodedRepositoryId).getNode(id);
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            VersionHistory history = versionManager.getVersionHistory(node.getPath());
            versionManager.checkout(node.getPath());
            node.setProperty(JCR_SERIES_STATUS, STATUS_AVAILABLE);
            session.save();
            String[] labels = history.getVersionLabels();
            for (String label : labels) {
              //Assign each version as 'avaiable'
              versionManager.checkout(node.getPath());
              node.setProperty(JCR_STATUS, STATUS_AVAILABLE);
              session.save();
              Version updatedNode = versionManager.checkin(node.getPath());
              versionManager.getVersionHistory(node.getPath())
                  .addVersionLabel(updatedNode.getName(), label, true);
            }
          } else {
            //If artifact series doesn't exist, create it.
            saveResource(encodedRepositoryId, uuid);
          }
        } else {
          session.logout();
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }
      return null;
    })) {
      // nothing to do
    }
  }

  public void enableResource(String repositoryId, UUID uuid, String versionId) {
    String id = this.encode(uuid.toString());

    String encodedRepositoryId = this.encode(repositoryId);

    try (DaoResult<?> ignored = execute((Session session) -> {
      try {
        Node rootNode = session.getRootNode();
        if (artifactSeriesExists(rootNode, encodedRepositoryId, id)) {
          Node node = session.getRootNode().getNode(encodedRepositoryId).getNode(id);
          VersionManager versionManager = session.getWorkspace().getVersionManager();
          if (!versionManager.getVersionHistory(node.getPath()).hasVersionLabel(versionId)) {
            throw new ResourceNotFoundException();
          }
          session.save();
          versionManager.checkout(node.getPath());
          node.setProperty(JCR_SERIES_STATUS, STATUS_AVAILABLE);
          node.setProperty(JCR_STATUS, STATUS_AVAILABLE);
          session.save();
          Version updatedNode = versionManager.checkin(node.getPath());
          versionManager.getVersionHistory(node.getPath())
              .addVersionLabel(updatedNode.getName(), versionId, true);

        } else {
          session.logout();
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (Exception e) {
        session.logout();
        throw new DaoRuntimeException(e);
      }

      return null;
    })) {
      // nothing to do
    }
  }

  private boolean versionIsUnavailable(Version version) throws RepositoryException {
    return version.getFrozenNode().getProperty(JCR_STATUS).getString().equals(STATUS_UNAVAILABLE);
  }

  private boolean artifactSeriesExists(Node rootNode, String encodedRepositoryId, String artifactId)
      throws RepositoryException {
    return rootNode.hasNode(encodedRepositoryId) && rootNode.getNode(encodedRepositoryId).hasNode(artifactId);
  }

  protected void shutdown() {
    if (cleanup != null) {
      cleanup.run();
    }
  }

  public void clear() {
    try (DaoResult<?> ignored = this.execute(session -> {
      try {
        NodeIterator itr = session.getRootNode().getNodes();

        while (itr.hasNext()) {
          Node node = itr.nextNode();

          if (! this.nodesNotToDelete.contains(node.getName())) {
            node.remove();
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    })) {
      // nothing to do
    }
  }

}
