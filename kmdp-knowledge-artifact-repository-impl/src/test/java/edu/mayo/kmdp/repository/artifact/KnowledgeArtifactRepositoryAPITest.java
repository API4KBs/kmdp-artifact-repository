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
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotFound;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.KnowledgeArtifactApi;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.KnowledgeArtifactRepositoryApi;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.KnowledgeArtifactSeriesApi;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository;

class KnowledgeArtifactRepositoryAPITest extends IntegrationTestBase {

  private ApiClientFactory webClientFactory;

  protected KnowledgeArtifactRepositoryApi repoApi;
  protected KnowledgeArtifactApi artApi;
  protected KnowledgeArtifactSeriesApi seriesApi;

  @BeforeEach
  void init() {
    webClientFactory = new ApiClientFactory("http://localhost:" + port, WithFHIR.NONE);
    repoApi = KnowledgeArtifactRepositoryApi.newInstance(webClientFactory);
    artApi = KnowledgeArtifactApi.newInstance(webClientFactory);
    seriesApi = KnowledgeArtifactSeriesApi.newInstance(webClientFactory);
  }

  @Test
  void testListArtifactsOnNonexistingRepo() {
    Answer<List<Pointer>> artifacts = seriesApi
        .listKnowledgeArtifacts("missing");
    assertFalse(artifacts.isSuccess());
    assertEquals(NotFound, artifacts.getOutcomeType());
  }

  @Test
  void testGetDefaultRepositoryID() {
    List<KnowledgeArtifactRepository> repos = repoApi.listKnowledgeArtifactRepositories()
        .orElseGet(Collections::emptyList);
    assertEquals(1, repos.size());
    KnowledgeArtifactRepository mainRepo = repos.get(0);
    assertEquals(
        IntegrationTestConfig.cfg.get(DEFAULT_REPOSITORY_ID).orElseGet(Assertions::fail),
        mainRepo.getId().getTag());
    assertEquals(
        IntegrationTestConfig.cfg.get(DEFAULT_REPOSITORY_NAME).orElseGet(Assertions::fail),
        mainRepo.getName());
  }

}
