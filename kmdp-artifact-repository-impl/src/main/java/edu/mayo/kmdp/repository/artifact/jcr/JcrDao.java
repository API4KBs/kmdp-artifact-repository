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

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository.JcrTypes;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.value.BinaryImpl;

public class JcrDao {

  protected String JCR_DATA = "jcr:data";

  private javax.jcr.Repository delegate;

  private Runnable cleanup;

  private List<String> types;


  public JcrDao(javax.jcr.Repository delegate, List<String> types) {
    this(delegate, null, types, new KnowledgeArtifactRepositoryServerConfig());
  }

  public JcrDao(javax.jcr.Repository delegate, List<String> types,
      KnowledgeArtifactRepositoryServerConfig config) {
    this(delegate, null, types, config);
  }

  public JcrDao(javax.jcr.Repository delegate, Runnable cleanup, List<String> types,
      KnowledgeArtifactRepositoryServerConfig config) {
    this.delegate = delegate;
    this.cleanup = cleanup;

    this.types = new ArrayList<>(types);
    init(config.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID));
  }

  public void init(String repositoryId) {
    try {
      Session session = this.delegate.login(
          new SimpleCredentials("admin", "admin".toCharArray()));

      // Oak does not supported federated workspaces - initialization would cause a NotSupportedException
//      if (!Arrays.asList(session.getWorkspace().getAccessibleWorkspaceNames())
//          .contains(repositoryId)) {
//        session.getWorkspace().createWorkspace(repositoryId);
//        session.save();
//      }

      this.execute(repositoryId, (Session sx) -> {

        try {
          for (String collection : types) {
            if (!sx.getRootNode().hasNode(collection)) {
              sx.getRootNode().addNode(collection);
            }
          }

          sx.save();
        } catch (RepositoryException e) {
          throw new RuntimeException(e);
        }

        return null;
      });
    } catch (RepositoryException re) {
      re.printStackTrace();
    }
  }

  public <T> DaoResult<T> execute(String repositoryId, Function<Session, T> f) {
    try {
      Session session = this.delegate.login(
          new SimpleCredentials("admin", "admin".toCharArray())
              // Oak does not supported federated workspaces - individual repo access would cause a NotSupportedException
//              , repositoryId
      );

      T result = f.apply(session);

      session.save();

      return new DaoResult<T>(result, session);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  public DaoResult<List<Version>> getResourceVersions(String resourceType, String repositoryId,
      String id_) {
    String id = Text.escapeIllegalJcrChars(id_);

    return this.execute(repositoryId, (Session session) -> {
      try {
        if (session.getRootNode().getNode(resourceType).hasNode(id)) {
          Node resource = session.getRootNode().getNode(resourceType).getNode(id);
          VersionHistory history = session.getWorkspace().getVersionManager()
              .getVersionHistory(resource.getPath());

          String[] versions = history.getVersionLabels();

          return Arrays.stream(versions).map(label -> {
            try {
              return history.getVersionByLabel(label);
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          }).collect(Collectors.toList());
        } else {
          return Collections.emptyList();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<Version> getLatestResource(String resourceType, String repositoryId, String id) {
    // TODO: It seems like this should be easier to do in JCR...
    DaoResult<List<Version>> result = this.getResourceVersions(resourceType, repositoryId, id);

    List<Version> versions = result.getValue();

    if (versions.isEmpty()) {
      result.close();
      throw new ResourceNotFoundException(id);
    }

    versions.sort((Version v1, Version v2) -> {
      try {
        return v2.getCreated().compareTo(v1.getCreated());
      } catch (RepositoryException e) {
        result.close();
        throw new RuntimeException(e);
      }
    });

    if (versions.size() == 0) {
      result.close();
      throw new ResourceNotFoundException(id);
    } else {
      return new DaoResult<>(versions.get(0), result.getSession());
    }
  }

  public DaoResult<Version> getResource(String resourceType, String repositoryId, String id_,
      String version) {
    String id = Text.escapeIllegalJcrChars(id_);

    return this.execute(repositoryId, (Session session) -> {
      try {
        Node resource = session.getRootNode().getNode(resourceType).getNode(id);
        VersionHistory history = session.getWorkspace().getVersionManager()
            .getVersionHistory(resource.getPath());

        return history.getVersionByLabel(version);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<List<Version>> getResources(String resourceType, String repositoryId,
      Map<String, String> query) {
    return this.execute(repositoryId, (Session session) -> {
      try {

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
            .createQuery(String.format("//%s/*%s", resourceType, queryString), "xpath")
            .execute();

        NodeIterator nodes = queryResult.getNodes();

        List<Version> result = new ArrayList<>();
        while (nodes.hasNext()) {
          Node node = nodes.nextNode();

          result.add(node.getBaseVersion());
        }

        return result;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

  }

  public void deleteResource(String resourceType, String repositoryId, String id_) {
    String id = Text.escapeIllegalJcrChars(id_);

    try (DaoResult<?> ignored = this.execute(repositoryId, (Session session) -> {
      try {
        Node resource = session.getRootNode().getNode(resourceType).getNode(id);
        VersionHistory history = session.getWorkspace().getVersionManager()
            .getVersionHistory(resource.getPath());
        String[] labels = history.getVersionLabels();
        for (String label : labels) {
          history.removeVersionLabel(label);
        }

        resource.remove();
        session.save();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    })) {
    }
  }

  public void deleteResource(String resourceType, String repositoryId, String id_, String version) {
    String id = Text.escapeIllegalJcrChars(id_);

    this.execute(repositoryId, (Session session) -> {
      try {
        Node resource = session.getRootNode().getNode(resourceType).getNode(id);
        VersionHistory history = session.getWorkspace().getVersionManager()
            .getVersionHistory(resource.getPath());
        history.removeVersionLabel(version);
        session.save();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    });
  }

  public void saveResource(String resourceType, String repositoryId, String id, String version,
      byte[] payload) {
    saveResource(resourceType, repositoryId, id, version, payload, new HashMap<>());
  }

  public DaoResult<Version> saveResource(String resourceType, String repositoryId, String id_,
      String version,
      byte[] payload, Map<String, String> metadata) {
    String id = Text.escapeIllegalJcrChars(id_);

    return this.execute(repositoryId, (Session session) -> {
      try {
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        Node assetNode = session.getRootNode().getNode(resourceType);

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

  public boolean isEmpty(String repositoryId) {
    DaoResult<Boolean> result = this.execute(repositoryId, (session) -> Arrays.stream(JcrTypes.values())
        .anyMatch((resourceType) -> {
              try {
                NodeIterator it = session.getRootNode().getNode(resourceType.name()).getNodes();
                return !it.hasNext();
              } catch (RepositoryException re) {
                return false;
              }
            }
        ));
    return result.value;
  }

  public void reset(String repositoryId) {
    this.execute(repositoryId, (session) -> {
      try {
        NodeIterator itr = session.getRootNode().getNodes();

        while (itr.hasNext()) {
          Node node = itr.nextNode();
          if (!node.getName().equals("jcr:system")) {
            node.remove();
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    });

    this.init(repositoryId);
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
