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

import edu.mayo.kmdp.repository.artifact.IntegrationTestBase.CommonTestConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.Swagger2SpringBoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;



@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = Swagger2SpringBoot.class)
@ContextConfiguration(classes = {
    CommonTestConfig.class,
    JCRIntegrationTestConfig.class,
    JPAIntegrationTestConfig.class
})
public abstract class IntegrationTestBase {

  @LocalServerPort
  int port;

  public static class CommonTestConfig {

    @Value("${edu.mayo.kmdp.repository.artifact.identifier:default}")
    private String repoId;
    @Value("${edu.mayo.kmdp.repository.artifact.name:Default Artifact Repository}")
    private String repoName;
    @Value("${edu.mayo.kmdp.repository.artifact.namespace}")
    private String namespace;

    @Bean
    public KnowledgeArtifactRepositoryServerProperties cfg() {
      return KnowledgeArtifactRepositoryServerProperties.emptyConfig()
          .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, repoId)
          .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME, repoName)
          .with(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE, namespace);
    }
  }

}
