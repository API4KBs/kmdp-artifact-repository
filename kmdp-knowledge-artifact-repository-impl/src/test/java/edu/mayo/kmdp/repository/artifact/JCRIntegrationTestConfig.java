package edu.mayo.kmdp.repository.artifact;

import static edu.mayo.kmdp.repository.artifact.IntegrationTestBase.CommonTestConfig.testCfg;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME;

import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

@Configuration
@ComponentScan(basePackageClasses = {JcrKnowledgeArtifactRepository.class})
@TestPropertySource(value={"classpath:application.properties"})
@EnableAutoConfiguration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "jcr")
public class JCRIntegrationTestConfig {

  @Bean
  @KPServer
  @Profile("jcr")
  public KnowledgeArtifactRepositoryService inMemoryRepository() {
    return new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(),
        testCfg);
  }
}
