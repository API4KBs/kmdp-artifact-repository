package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.repository.artifact.jpa.JPAArtifactDAO;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import javax.sql.DataSource;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

@Configuration
@ComponentScan(basePackageClasses = {JPAKnowledgeArtifactRepository.class})
@TestPropertySource(value={"classpath:application.properties"})
@EnableAutoConfiguration
@EnableJpaRepositories
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "jpa")
public class JPAIntegrationTestConfig {

  @Autowired
  DataSource source;

  @Autowired
  KnowledgeArtifactRepositoryServerProperties testCfg;

  @Autowired
  JPAArtifactDAO dao;

  @Bean
  @KPServer
  @Profile("jpa")
  public KnowledgeArtifactRepositoryService inMemoryRepository() {
    return new JPAKnowledgeArtifactRepository(
        dao,
        testCfg);
  }
}
