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

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.ResourceNoContentException;
import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import java.io.Closeable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.value.BinaryImpl;

public class JcrDao {

  protected String JCR_DATA = "jcr:data";
  protected String JCR_STATUS = "status";
  protected String JCR_SERIES_STATUS = "seriesStatus";
  protected String STATUS_AVAILABLE = "available";
  protected String STATUS_UNAVAILABLE = "unavailable";

  private javax.jcr.Repository delegate;

  private Runnable cleanup;

  public JcrDao(javax.jcr.Repository delegate) {
    this(delegate, null, new KnowledgeArtifactRepositoryServerConfig());
  }

  public JcrDao(javax.jcr.Repository delegate,
      KnowledgeArtifactRepositoryServerConfig config) {
    this(delegate, null, config);
  }

  public JcrDao(javax.jcr.Repository delegate, Runnable cleanup,
      KnowledgeArtifactRepositoryServerConfig config) {
    this.delegate = delegate;
    this.cleanup = cleanup;
  }

  public <T> DaoResult<T> execute(Function<Session, T> f) {
    try {
      Session session = this.delegate.login(
          new SimpleCredentials("admin", "admin".toCharArray())
      );

      T result = f.apply(session);

      session.save();

      return new DaoResult<T>(result, session);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  public DaoResult<List<Version>> getResourceVersions(String repositoryId,
      UUID id_, Boolean deleted) {
    String id = Text.escapeIllegalJcrChars(id_.toString());

    return this.execute((Session session) -> {
      try {
        if (session.getRootNode().hasNode(repositoryId) && session.getRootNode().getNode(repositoryId).hasNode(id)) {
          Node resource = session.getRootNode().getNode(repositoryId).getNode(id);
          if(!deleted && resource.getProperty(JCR_SERIES_STATUS).getString().equals(STATUS_UNAVAILABLE)){
            throw new ResourceNoContentException("Artifact known, but not available.");
          }
          VersionHistory history = session.getWorkspace().getVersionManager()
                  .getVersionHistory(resource.getPath());
          String[] versions = history.getVersionLabels();

          return Arrays.stream(versions)
            .map(label -> {
            try {
              return history.getVersionByLabel(label);
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          })
                  .filter(version -> {
                    //If deleted != true, filter out unavailable versions
                    try {
                      return deleted || !versionIsUnavailable(version);
                    } catch (RepositoryException e) {
                      throw new RuntimeException(e);
                    }
                  })

                  .collect(Collectors.toList());
        } else {
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException | ResourceNoContentException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<Version> getLatestResource(String repositoryId, UUID id, Boolean deleted) {
    DaoResult<List<Version>> result = this.getResourceVersions(repositoryId, id, deleted);
    List<Version> versions = result.getValue();
    if (versions.isEmpty()) {
      result.close();
      throw new ResourceNoContentException("Artifact known but either not available, or no versions are available");
    }
    versions.sort((Version v1, Version v2) -> {
      try {
        return v2.getCreated().compareTo(v1.getCreated());
      } catch (RepositoryException e) {
        result.close();
        throw new RuntimeException(e);
      }
    });
      return new DaoResult<>(versions.get(0), result.getSession());
  }

  public DaoResult<Version> getResource(String repositoryId, UUID id_,
      String version, boolean getUnavailable) {
    String id = Text.escapeIllegalJcrChars(id_.toString());

    return this.execute((Session session) -> {
      try {
        if (!session.getRootNode().hasNode(repositoryId) || !session.getRootNode().getNode(repositoryId).hasNode(id)) {
          throw new ResourceNotFoundException();
        }
        Node resource = session.getRootNode().getNode(repositoryId).getNode(id);
        VersionHistory history = session.getWorkspace().getVersionManager()
                .getVersionHistory(resource.getPath());
        if(!history.hasVersionLabel(version)) {
          throw new ResourceNotFoundException();
        }
        if (!getUnavailable && versionIsUnavailable(history.getVersionByLabel(version))) {
          throw new ResourceNoContentException("The version is known but currently unavailable.");
        }
        return history.getVersionByLabel(version);
      }catch(ResourceNoContentException | ResourceNotFoundException e){
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<List<Node>> getResources(String repositoryId_,
      Boolean deleted, Map<String, String> query) {
    String repositoryId = ISO9075.encodePath(repositoryId_);
    return this.execute((Session session) -> {
      try {
        if (!session.getRootNode().hasNode(repositoryId)) {
          throw new RepositoryNotFoundException();
        }
        String queryString;
        if (query.isEmpty()) {
          queryString = "";
        } else {
          String q = query.entrySet().stream()
                  .map(entry -> String.format("jcr:%s='%s'", entry.getKey(), entry.getValue()))
                  .collect(Collectors.joining(" AND "));
          queryString = "[" + q + "]";
        }
        QueryResult queryResult = session.getWorkspace().getQueryManager()
                .createQuery(String.format("//%s/*%s", repositoryId, queryString), "xpath")
                .execute();

        NodeIterator nodes = queryResult.getNodes();
        List<Node> result = new ArrayList<>();
        while (nodes.hasNext()) {
          Node node = nodes.nextNode();
          if (!deleted && node.hasProperty(JCR_SERIES_STATUS) && node.getProperty(JCR_SERIES_STATUS).getString().equals(STATUS_UNAVAILABLE)){
            continue;
          }
          result.add(node);
        }
        return result;
      }catch(RepositoryNotFoundException e){
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

  }

  public void deleteResource(String repositoryId, UUID id_) {
    String id = Text.escapeIllegalJcrChars(id_.toString());

    try (DaoResult<?> ignored = this.execute((Session session) -> {
      try {

        if (session.getRootNode().hasNode(repositoryId) && session.getRootNode().getNode(repositoryId).hasNode(id)) {
          Node resource = session.getRootNode().getNode(repositoryId).getNode(id);
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
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e){
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    })) {
    }
  }

  public void deleteResource(String repositoryId, UUID id_, String version) {
    String id = Text.escapeIllegalJcrChars(id_.toString());
    this.execute((Session session) -> {
      try {
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        Node rootNode = session.getRootNode();
        if (rootNode.hasNode(repositoryId) && rootNode.getNode(repositoryId).hasNode(id)) {
          Node node = session.getRootNode().getNode(repositoryId).getNode(id);
          if (versionManager.getVersionHistory(node.getPath()).hasVersionLabel(version)) {
            versionManager.checkout(node.getPath());
            node.setProperty(JCR_STATUS, STATUS_UNAVAILABLE);
            session.save();
            Version newNode = versionManager.checkin(node.getPath());
            versionManager.getVersionHistory(node.getPath())
                    .addVersionLabel(newNode.getName(), version, true);
            return null;
          }
          else {
            throw new ResourceNotFoundException();
          }
        } else {
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public void saveResource(String repositoryId, UUID id, String version,
      byte[] payload) {
    saveResource(repositoryId, id, version, payload, new HashMap<>());
  }

  public DaoResult<Version> saveResource(String repositoryId, UUID id_,
      String version,
      byte[] payload, Map<String, String> metadata) {
    String id = Text.escapeIllegalJcrChars(id_.toString());

    return this.execute((Session session) -> {
      try {
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        // check if repository node exists
        if(! session.getRootNode().hasNode(repositoryId)) {
          session.getRootNode().addNode(repositoryId);
        }
        Node assetNode = session.getRootNode().getNode(repositoryId);

        Node node;
        if (assetNode.hasNode(id)) {
          node = assetNode.getNode(id);
          versionManager.checkout(node.getPath());

        } else {
          node = assetNode.addNode(id);
          node.addMixin(MIX_VERSIONABLE);
          node.setProperty("jcr:id", id);
        }

        node.setProperty(JCR_DATA, new BinaryImpl(payload));
        node.setProperty(JCR_STATUS, STATUS_AVAILABLE);
        node.setProperty(JCR_SERIES_STATUS, STATUS_AVAILABLE);
        if (metadata != null) {
          metadata.forEach((key, value) -> {
            try {
              node.setProperty("jcr:" + key, value);
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          });
        }
        session.save();
        Version newNode = versionManager.checkin(node.getPath());
        versionManager.getVersionHistory(node.getPath())
            .addVersionLabel(newNode.getName(), version, true);

        return newNode;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<Node> saveResource(String repositoryId, UUID id_) {
    String id = Text.escapeIllegalJcrChars(id_.toString());

    return this.execute((Session session) -> {
      try {
        // check if repository node exists, if not, add it
        if(! session.getRootNode().hasNode(repositoryId)) {
          session.getRootNode().addNode(repositoryId);
        }
        Node assetNode = session.getRootNode().getNode(repositoryId);
        Node node = assetNode.addNode(id);
        node.addMixin(MIX_VERSIONABLE);
        node.setProperty("jcr:id", id);
        node.setProperty(JCR_SERIES_STATUS, STATUS_AVAILABLE);
        session.save();

        return node;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public void enableResource(String repositoryId, UUID id_) {
    String id = Text.escapeIllegalJcrChars(id_.toString());
    try (DaoResult<?> ignored = this.execute((Session session) -> {
      try {
        if (session.getRootNode().hasNode(repositoryId)) {
          if(session.getRootNode().getNode(repositoryId).hasNode(id)){
            Node node = session.getRootNode().getNode(repositoryId).getNode(id);
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
          }
          else {
            saveResource(repositoryId, id_);
          }
        } else {
          throw new ResourceNotFoundException();
        }
      } catch (ResourceNotFoundException e){
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    })) {
    }
  }

    public void enableResource(String repositoryId, UUID id_, String versionId) {
        String id = Text.escapeIllegalJcrChars(id_.toString());
        try (DaoResult<?> ignored = this.execute((Session session) -> {
            try {
                if (session.getRootNode().hasNode(repositoryId) && session.getRootNode().getNode(repositoryId).hasNode(id)) {
                    Node node = session.getRootNode().getNode(repositoryId).getNode(id);
                    VersionManager versionManager = session.getWorkspace().getVersionManager();
                    if(!versionManager.getVersionHistory(node.getPath()).hasVersionLabel(versionId)){
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
                    throw new ResourceNotFoundException();
                }
            } catch (ResourceNotFoundException e){
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return null;
        })) {
        }
    }

  private boolean versionIsUnavailable(Version version) throws RepositoryException {
    return version.getFrozenNode().getProperty(JCR_STATUS).getString().equals(STATUS_UNAVAILABLE);
  }

  protected void shutdown() {
    if (this.cleanup !=null) {
      this.cleanup.run();
    }
  }

  public static class DaoResult<T> implements Closeable {

    private T value;
    private Session session;

    DaoResult(T value, Session session) {
      this.value = value;
      this.session = session;
    }

    @Override
    public void close() {
      this.session.logout();
    }

    public T getValue() {
      return value;
    }

    public Session getSession() {
      return session;
    }
  }
}
