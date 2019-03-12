package edu.mayo.kmdp.repository.artifact;

import java.io.File;
import org.springframework.util.StringUtils;

public class ServerConfig {

  public static File getConfigDir() {
    File home;

    String repoHome = System.getProperty("REPO_HOME");

    if (StringUtils.isEmpty(repoHome)) {
      home = new File(System.getProperty("user.home"), ".artifactRepo");
      home.mkdir();
    } else {
      home = new File(repoHome);
    }

    return home;
  }
}
