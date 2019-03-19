package edu.mayo.kmdp.repository.artifact;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._1_0.services.URIPathHelper;

public class HrefBuilder {

  private String host;
  private KnowledgeArtifactRepositoryServerConfig cfg;

  public HrefBuilder(KnowledgeArtifactRepositoryServerConfig cfg) {
    this.cfg = cfg;
    this.host = StringUtils
        .removeEnd(cfg.getTyped(KnowledgeArtifactRepositoryOptions.SERVER_HOST).toString(), "/");
  }

  public URI getArtifactHref(String artifactId,
      String versionTag) {
    return URI.create(URIPathHelper.knowledgeArtifactLocation(this.host,
        cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID), artifactId,
        versionTag));
  }

}
