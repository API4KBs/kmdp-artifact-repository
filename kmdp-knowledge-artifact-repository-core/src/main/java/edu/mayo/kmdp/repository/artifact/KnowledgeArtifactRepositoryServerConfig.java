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

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.util.Util;
import java.io.File;
import java.net.URL;
import java.util.Properties;


public class KnowledgeArtifactRepositoryServerConfig extends
    ConfigProperties<KnowledgeArtifactRepositoryServerConfig, KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions> {

  private static final Properties DEFAULTED = defaulted(KnowledgeArtifactRepositoryOptions.class);

  public KnowledgeArtifactRepositoryServerConfig() {
    super(DEFAULTED);
  }

  @Override
  public KnowledgeArtifactRepositoryOptions[] properties() {
    return KnowledgeArtifactRepositoryOptions.values();
  }

  public enum KnowledgeArtifactRepositoryOptions implements
      Option<KnowledgeArtifactRepositoryOptions> {

    DEFAULT_REPOSITORY_ID(
        Opt.of("http://edu.mayo.kmdp/artifactRepository/identifier",
            getDefaultRepositoryId(),
            "ID of the default artifact repository",
            String.class,
            false)),

    DEFAULT_REPOSITORY_NAME(
        Opt.of("http://edu.mayo.kmdp/artifactRepository/name",
            "Default Knowledge Artifact Repository",
            "Name of the default artifact repository",
            String.class,
            false)),

    SERVER_HOST(
        Opt.of("http://edu.mayo.kmdp/artifactRepository/host",
            getHost(),
            "Host",
            URL.class,
            false)),

    BASE_DIR(
        Opt.of("http://edu.mayo.kmdp/artifactRepository/filesystem/directory",
            getConfigDir().getAbsolutePath(),
            "Root directory for filesystem-based repositories",
            File.class,
            false)),

    BASE_NAMESPACE(
        Opt.of("http://edu.mayo.kmdp/artifactRepository/baseNamespace",
            getBaseNamespace(),
            "Base namespace",
            String.class,
            false));


    private Opt<KnowledgeArtifactRepositoryOptions> opt;

    KnowledgeArtifactRepositoryOptions(Opt<KnowledgeArtifactRepositoryOptions> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<KnowledgeArtifactRepositoryOptions> getOption() {
      return opt;
    }


    private static String getHost() {
      String envHost = System.getProperty("http://edu.mayo.kmdp/artifactRepository/host");
      return !Util.isEmpty(envHost) ? envHost : "http://localhost:8080";
    }

    private static String getDefaultRepositoryId() {
      return "default";
    }

    private static String getBaseNamespace() {
      String namespace = System
          .getProperty("http://edu.mayo.kmdp/artifactRepository/baseNamespace");
      return !Util.isEmpty(namespace) ? namespace : "http://edu.mayo.kmdp/test";
    }

    private static File getConfigDir() {
      File home = null;

      String repoHome = System
          .getProperty("http://edu.mayo.kmdp/artifactRepository/repositoryHomeDir");

      if (Util.isEmpty(repoHome)) {
        repoHome = System.getProperty("user.home");
      }

      if (!Util.isEmpty(repoHome)) {
        home = new File(repoHome, ".artifactRepo");
        if (!home.exists()) {
          home = home.mkdirs() ? home : null;
        }
      }

      return home;
    }

  }
}
