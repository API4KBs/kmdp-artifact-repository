package edu.mayo.kmdp.repository.artifact;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;

public class HrefBuilder {

  private static final String DEFAULT_REPOS_ID = "1";

  private String DEFAULT_HOST = "http://localhost:8080";
  private String host;

  public HrefBuilder() {
    String envHost = System.getProperty("REPOSITORY_URL");
    if (StringUtils.isNotBlank(envHost)) {
      this.host = envHost;
    } else {
      this.host = DEFAULT_HOST;
    }

    this.host = StringUtils.removeEnd(this.host, "/");
  }

  public URI getArtifactHref(String id, String version) {
    return URI.create(String
        .format("%s/repos/%s/artifacts/%s/versions/%s", this.host, DEFAULT_REPOS_ID, id, version));
  }

}
