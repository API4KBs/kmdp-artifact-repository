package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.util.Util;
import java.io.File;
import java.net.URL;

public enum KnowledgeArtifactRepositoryOptions implements
    Option<KnowledgeArtifactRepositoryOptions> {

  DEFAULT_REPOSITORY_ID(
      Opt.of("http://edu.mayo.kmdp/artifactRepository/identifier", getDefaultRepositoryId(),
          String.class)),

  DEFAULT_REPOSITORY_NAME(
      Opt.of("http://edu.mayo.kmdp/artifactRepository/name",
          "Default Knowledge Artifact Repository",
          String.class)),

  SERVER_HOST(
      Opt.of("http://edu.mayo.kmdp/artifactRepository/host", getHost(), URL.class)),

  BASE_DIR(
      Opt.of("http://edu.mayo.kmdp/artifactRepository/filesystem/directory", getConfigDir().getAbsolutePath(), File.class)),

  BASE_NAMESPACE(
      Opt.of("http://edu.mayo.kmdp/artifactRepository/baseNamespace", getBaseNamespace(),
          String.class));


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
    String namespace = System.getProperty("http://edu.mayo.kmdp/artifactRepository/baseNamespace");
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


