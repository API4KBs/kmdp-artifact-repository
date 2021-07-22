/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.artifact;

import static org.omg.spec.api4kp._20200801.services.URIPathHelper.knowledgeArtifactLocation;

import java.net.URI;

public class HrefBuilder {

  private static final String DEFAULT_HOST = "http:/";
  private static final String DEFAULT_CURR_URL = "";

  protected String getHost() {
    return DEFAULT_HOST;
  }

  public String getCurrentURL() {
    return DEFAULT_CURR_URL;
  }

  public HrefBuilder(KnowledgeArtifactRepositoryServerProperties cfg) {
    // nothing to do
  }

  public URI getArtifactHref(String artifactId,
      String versionTag, String repositoryId) {
    return URI.create(
        knowledgeArtifactLocation(getHost(), repositoryId, artifactId, versionTag));
  }

  public URI getSeriesHref(String artifactId, String repositoryId) {
    return URI.create(knowledgeArtifactSeriesLocation(
        getHost(), repositoryId, artifactId));
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
