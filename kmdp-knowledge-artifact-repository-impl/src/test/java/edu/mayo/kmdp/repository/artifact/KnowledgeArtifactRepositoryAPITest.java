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

import edu.mayo.kmdp.repository.artifact.jcr.JcrDao;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import javax.jcr.Repository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class KnowledgeArtifactRepositoryAPITest {

  private static KnowledgeArtifactRepositoryApi repoApi;
  private static KnowledgeArtifactApi artApi;
  private static KnowledgeArtifactSeriesApi seriesApi;

  private static KnowledgeArtifactRepositoryServerConfig cfg =
      new KnowledgeArtifactRepositoryServerConfig()
          .with(DEFAULT_REPOSITORY_NAME, "TestRepository")
          .with(DEFAULT_REPOSITORY_ID, "TestRepo");

  @BeforeAll
  static void repo() {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr);
    edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository repoImpl = new JcrKnowledgeArtifactRepository(dao, cfg);

    repoApi = KnowledgeArtifactRepositoryApi.newInstance(repoImpl);
    artApi = KnowledgeArtifactApi.newInstance(repoImpl);
    seriesApi = KnowledgeArtifactSeriesApi.newInstance(repoImpl);
  }

  @Test
  public void testRepoDiscovery() {
    repoApi.listKnowledgeArtifactRepositories();
  }

}
