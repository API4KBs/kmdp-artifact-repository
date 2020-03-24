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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.mayo.kmdp.repository.artifact.v3.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.v3.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.v3.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.v3.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.Pointer;

public class KnowledgeArtifactRepositoryAPITest extends IntegrationTestBase {

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
  public void testListArtifactsOnNonexistingRepo() {
    Answer<List<Pointer>> artifacts = seriesApi
        .listKnowledgeArtifacts("missing");
    assertFalse(artifacts.isSuccess());
    assertEquals(ResponseCodeSeries.NotFound, artifacts.getOutcomeType());
  }

}
