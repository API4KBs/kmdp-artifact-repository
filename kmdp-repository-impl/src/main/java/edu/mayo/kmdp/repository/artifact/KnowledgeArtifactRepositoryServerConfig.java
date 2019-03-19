package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.xslt.XSLTOptions;
import java.util.Properties;


public class KnowledgeArtifactRepositoryServerConfig extends
    ConfigProperties<KnowledgeArtifactRepositoryServerConfig, KnowledgeArtifactRepositoryOptions> {

  private static final Properties defaults = defaulted(XSLTOptions.class);

  public KnowledgeArtifactRepositoryServerConfig() {
    super(defaults);
  }

  @Override
  protected KnowledgeArtifactRepositoryOptions[] properties() {
    return KnowledgeArtifactRepositoryOptions.values();
  }

}
