package edu.mayo.kmdp.repository.artifact.jpa;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = JPAArtifactDAO.class)
public class TestJPAConfiguration {

  @Bean
  KnowledgeArtifactRepositoryServerConfig testCfg() {
    return new KnowledgeArtifactRepositoryServerConfig()
        .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, "1")
        .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME, "jUnit Mock");
  }

}
