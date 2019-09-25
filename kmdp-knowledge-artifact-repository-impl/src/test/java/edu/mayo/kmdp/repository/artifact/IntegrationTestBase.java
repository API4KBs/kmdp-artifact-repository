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

import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID;
import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME;

import edu.mayo.kmdp.repository.artifact.IntegrationTestBase.IntegrationTestConfig;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.server.Swagger2SpringBoot;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public abstract class IntegrationTestBase {

  private static ConfigurableApplicationContext ctx;

  @BeforeAll
  public static void setupServer() {
    SpringApplication app = new SpringApplication(Swagger2SpringBoot.class);
    ctx = app.run();
  }

  @AfterAll
  public static void stopServer() {
    SpringApplication.exit(ctx, (ExitCodeGenerator) () -> 0);
  }

  @Configuration
  @ComponentScan(basePackages = {"edu.mayo.kmdp.repository.artifact"})
  @PropertySource(value={"classpath:application.test.properties"})
  public static class IntegrationTestConfig {

    private static KnowledgeArtifactRepositoryServerConfig cfg =
        new KnowledgeArtifactRepositoryServerConfig()
            .with(DEFAULT_REPOSITORY_NAME, "TestRepository")
            .with(DEFAULT_REPOSITORY_ID, "TestRepo");

    @Bean
    @Profile("test")
    public KnowledgeArtifactRepository inMemoryRepository() {
      return new JcrKnowledgeArtifactRepository(
          new Jcr(new Oak()).createRepository(),
          cfg);
    }

  }

}
