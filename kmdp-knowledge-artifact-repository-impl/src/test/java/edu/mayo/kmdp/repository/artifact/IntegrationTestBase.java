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
import edu.mayo.kmdp.repository.artifact.v4.server.Swagger2SpringBoot;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;



@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = Swagger2SpringBoot.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public abstract class IntegrationTestBase {

  @LocalServerPort
  int port;

  @Configuration
  @ComponentScan(basePackageClasses = {KnowledgeArtifactRepositoryService.class})
  @TestPropertySource(value={"classpath:application.test.properties"})
  public static class IntegrationTestConfig {

    private static KnowledgeArtifactRepositoryServerConfig cfg =
        new KnowledgeArtifactRepositoryServerConfig()
            .with(DEFAULT_REPOSITORY_NAME, "TestRepository")
            .with(DEFAULT_REPOSITORY_ID, "TestRepo");

    @Bean
    @KPServer
    @Profile("test")
    public KnowledgeArtifactRepositoryService inMemoryRepository() {
      return new JcrKnowledgeArtifactRepository(
          new Jcr(new Oak()).createRepository(),
          cfg);
    }
  }

}
