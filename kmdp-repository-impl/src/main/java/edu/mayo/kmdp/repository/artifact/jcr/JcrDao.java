package edu.mayo.kmdp.repository.artifact.jcr;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.apache.jackrabbit.oak.plugins.version.VersionConstants;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.value.BinaryImpl;

public class JcrDao {

  protected String JCR_DATA = "jcr:data";

  private javax.jcr.Repository delegate;

  private Runnable cleanup;

  private Runnable initFn;

  public JcrDao(javax.jcr.Repository delegate, List<String> types)
      throws IOException, InvalidFileStoreVersionException {
    this(delegate, null, types);
  }

  public JcrDao(javax.jcr.Repository delegate, Runnable cleanup, List<String> types)
      throws IOException, InvalidFileStoreVersionException {
    this.delegate = delegate;
    this.cleanup = cleanup;

    this.initFn = () -> {
      this.execute((Session session) -> {

        try {
          for (String collection : types) {
            if (!session.getRootNode().hasNode(collection)) {
              session.getRootNode().addNode(collection);
            }
          }

          session.save();
        } catch (RepositoryException e) {
          throw new RuntimeException(e);
        }

        return null;
      });
    };

    this.initFn.run();
  }

  public <T> DaoResult<T> execute(Function<Session, T> f) {
    try {
      Session session = this.delegate.login(
          new SimpleCredentials("admin", "admin".toCharArray()));

      T result = f.apply(session);

      session.save();

      return new DaoResult(result, session);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  public DaoResult<Optional<List<Version>>> getResourceVersions(String resourceType, String id_) {
    String id = Text.escapeIllegalJcrChars(id_);

    return this.execute((Session session) -> {
      try {
        if (session.getRootNode().getNode(resourceType).hasNode(id)) {
          Node resource = session.getRootNode().getNode(resourceType).getNode(id);
          VersionHistory history = session.getWorkspace().getVersionManager()
              .getVersionHistory(resource.getPath());

          String[] versions = history.getVersionLabels();

          return Optional.of(Arrays.stream(versions).map(label -> {
            try {
              return history.getVersionByLabel(label);
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          }).collect(Collectors.toList()));
        } else {
          return Optional.empty();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<Version> getLatestResource(String resourceType, String id) {
    // TODO: It seems like this should be easier to do in JCR...
    DaoResult<Optional<List<Version>>> result = this.getResourceVersions(resourceType, id);

    Optional<List<Version>> value = result.getValue();

    if (!value.isPresent()) {
      result.close();
      throw new ResourceNotFoundException(id);
    }
    List<Version> versions = value.get();

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
      return new DaoResult(versions.get(0), result.getSession());
    }
  }

  public DaoResult<Version> getResource(String resourceType, String id_, String version) {
    String id = Text.escapeIllegalJcrChars(id_);

    return this.execute((Session session) -> {
      try {
        Node resource = session.getRootNode().getNode(resourceType).getNode(id);
        VersionHistory history = session.getWorkspace().getVersionManager()
            .getVersionHistory(resource.getPath());

        Version versionNode = history.getVersionByLabel(version);

        return versionNode;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public DaoResult<List<Version>> getResources(String resourceType, Map<String, String> query) {
    return this.execute((Session session) -> {
      try {
        String type = resourceType;

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
            .createQuery(String.format("//%s/*%s", type, queryString), "xpath").execute();

        NodeIterator nodes = queryResult.getNodes();

        List<Version> result = Lists.newArrayList();
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

  public void deleteResource(String resourceType, String id_) {
    String id = Text.escapeIllegalJcrChars(id_);

    try (DaoResult<?> _ = this.execute((Session session) -> {
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
      return;
    }
  }

  public void deleteResource(String resourceType, String id_, String version) {
    String id = Text.escapeIllegalJcrChars(id_);

    try (DaoResult<?> _ = this.execute((Session session) -> {
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
    })) {
      return;
    }
  }

  public void saveResource(String resourceType, String id, String version, byte[] payload) {
    try (DaoResult<Version> _ = this
        .saveResource(resourceType, id, version, payload, Maps.newHashMap())) {
      return;
    }
  }

  public DaoResult<Version> saveResource(String resourceType, String id_, String version,
      byte[] payload, Map<String, String> metadata) {
    String id = Text.escapeIllegalJcrChars(id_);

    return this.execute((Session session) -> {
      try {
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        Node assetNode = session.getRootNode().getNode(resourceType);

        Node node;
        if (assetNode.hasNode(id)) {
          node = assetNode.getNode(id);
          versionManager.checkout(node.getPath());

        } else {
          node = assetNode.addNode(id);
          node.addMixin(VersionConstants.MIX_VERSIONABLE);
          node.setProperty("jcr:id", id);
        }

        node.setProperty(JCR_DATA, new BinaryImpl(payload));
        if (metadata != null) {
          metadata.entrySet().forEach(entry -> {
            try {
              node.setProperty("jcr:" + entry.getKey(), entry.getValue());
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

  public void reset() {
    this.execute((session -> {
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
    }));

    this.initFn.run();
  }

  protected void shutdown() {
    this.cleanup.run();
  }

}
