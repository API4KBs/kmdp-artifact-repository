package edu.mayo.kmdp.repository.artifact.jcr;

import com.google.common.collect.Maps;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.repository.artifact.HrefBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.jcr.version.Version;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class JcrRepositoryAdapter implements DisposableBean,
    edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository {

  private static final String JCR_DATA = "jcr:data";
  private static final String URI_BASE = "http://TODO/";

  private HrefBuilder hrefBuilder = new HrefBuilder();

  enum JcrTypes {
    ARTIFACT(byte[].class);

    private final Class type;

    JcrTypes(final Class type) {
      this.type = type;
    }
  }

  public JcrDao dao;

  public JcrRepositoryAdapter(javax.jcr.Repository delegate)
      throws IOException, InvalidFileStoreVersionException {
    this(delegate, null);
  }

  public JcrRepositoryAdapter(javax.jcr.Repository delegate, Runnable cleanup)
      throws IOException, InvalidFileStoreVersionException {
    this(new JcrDao(delegate, cleanup, Arrays.stream(JcrTypes.values())
        .map(Enum::name)
        .collect(Collectors.toList())));
  }

  public JcrRepositoryAdapter(JcrDao dao) {
    this.dao = dao;
  }


  @Override
  public ResponseEntity<Void> addKnowledgeArtifactVersion(String repositoryId, String artifactId,
      byte[] document) {
    Map<String, String> params = Maps.newHashMap();

    String versionId = UUID.randomUUID().toString();

    try (DaoResult<Version> result = this.dao
        .saveResource(JcrTypes.ARTIFACT.name(), artifactId, versionId, document, params)) {
      return new ResponseEntity<>(HttpStatus.OK);
    }
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactSeries(String repositoryId,
      String artifactId) {
    this.dao.deleteResource(JcrTypes.ARTIFACT.name(), artifactId);

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactVersion(String repositoryId, String artifactId,
      String versionTag) {
    this.dao.deleteResource(JcrTypes.ARTIFACT.name(), artifactId, versionTag);

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<KnowledgeArtifactRepository> getKnowledgeArtifactRepository(
      String repositoryId) {
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteKnowledgeArtifactRepository(String repositoryId) {
    return null;
  }

  @Override
  public ResponseEntity<Void> setKnowledgeArtifactRepository(String repositoryId,
      KnowledgeArtifactRepository repositoryDescr) {
    return null;
  }



  @Override
  public ResponseEntity<byte[]> getLatestKnowledgeArtifact(String repositoryId, String artifactId) {
    return null;
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeArtifactSeries(String repositoryId,
      String artifactId, Integer offset, Integer limit, String beforeTag, String afterTag,
      String sort){
    try (DaoResult<Optional<List<Version>>> result = this.dao
        .getResourceVersions(JcrTypes.ARTIFACT.name(), artifactId)) {
      Optional<List<Version>> versions = result.getValue();

      return versions.map(versions1 -> this.wrap(versions1.stream()
          .map(this::versionToPointer)
          .collect(Collectors.toList())))
          .orElseGet(() -> ResponseEntity.notFound().build());
    }
  }

  @Override
  public ResponseEntity<Void> setKnowledgeArtifactSeries(String repositoryId, String artifactId) {
    return null;
  }

  @Override
  public ResponseEntity<byte[]> getKnowledgeArtifactVersion(String repositoryId, String artifactId,
      String versionTag) {
    try (DaoResult<Version> result = this.dao
        .getResource(JcrTypes.ARTIFACT.name(), artifactId, versionTag)) {
      Version version = result.getValue();
      byte[] bytes = this.getDataFromNode(version);

      // TODO: set mime type
      HttpHeaders headers = new HttpHeaders();

      return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
  }


  @Override
  public ResponseEntity<Void> initKnowledgeArtifactRepository( KnowledgeArtifactRepository descr ) {
    return null;
  }

  @Override
  public ResponseEntity<Void> initKnowledgeArtifactSeries(String repositoryId) {
    return null;
  }

  @Override
  public ResponseEntity<Void> isKnowledgeArtifactRepository(String repositoryId) {
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
  public ResponseEntity<List<Pointer>> listKnowledgeArtifactRepositories() {
    return null;
  }

  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeArtifacts(String repositoryId, Integer offset,
      Integer limit) {
    try (DaoResult<List<Version>> result = this.dao
        .getResources(JcrTypes.ARTIFACT.name(), Maps.newHashMap())) {
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
    Map<String, String> params = Maps.newHashMap();

    try (DaoResult<Version> result = this.dao
        .saveResource(JcrTypes.ARTIFACT.name(), artifactId, versionTag, document, params)) {
      Version node = result.getValue();

      return new ResponseEntity<>(HttpStatus.OK);
    }
  }

  @Override
  public void destroy() {
    this.dao.shutdown();
  }

  private Pointer versionToPointer(Version version) {
    try {
      String artifactId = version.getFrozenNode().getProperty("jcr:id").getString();
      String label = version.getContainingHistory().getVersionLabels(version)[0];

      Pointer pointer = new edu.mayo.kmdp.common.model.Pointer();
      pointer.setHref(this.hrefBuilder.getArtifactHref(artifactId, label));

      URIIdentifier id = DatatypeHelper.uri(URI_BASE, artifactId, label);
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
