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
        .removeEnd(cfg.getTyped(
            KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.SERVER_HOST)
            .toString(), "/");
  }

  public URI getArtifactHref(String artifactId,
      String versionTag, String repositoryId) {
    return URI.create(URIPathHelper.knowledgeArtifactLocation(host,
        repositoryId, artifactId,
        versionTag));
  }

  public URI getSeriesHref(String artifactId, String repositoryId) {
    return URI.create(knowledgeArtifactSeriesLocation(host,
        repositoryId, artifactId));
  }

  public static String knowledgeArtifactSeriesLocation(String host, String repositoryId,
      String artifactId) {
    return String
        .format("%s%s", host, knowledgeArtifactSeriesPath(repositoryId, artifactId));
  }

  public static String knowledgeArtifactSeriesPath(String repositoryId, String artifactId) {
    return String
        .format("/repos/%s/artifacts/%s", repositoryId, artifactId);
  }

}
