/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.artifact.jcr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.util.Collections;
import java.util.List;
import javax.jcr.Repository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository;

class JcrKnowledgeArtifactRepositoryServiceTest {

  private static KnowledgeArtifactRepositoryService repo;

  @BeforeAll
  static void repo() {
    KnowledgeArtifactRepositoryServerProperties cfg =
        new KnowledgeArtifactRepositoryServerProperties(
            JcrKnowledgeArtifactRepositoryServiceTest.class.getResourceAsStream(
                "/application.test2.properties"));

    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    JcrDao dao = new JcrDao(jcr);
    repo = new JcrKnowledgeArtifactRepository(dao, cfg);
  }

  @AfterAll
  static void cleanup() {
    JcrKnowledgeArtifactRepository jackrabbit = (JcrKnowledgeArtifactRepository) repo;
    jackrabbit.shutdown();
  }

  @Test
  void testListRepositoryWithDefault() {
    Answer<List<KnowledgeArtifactRepository>> ans =
        repo.listKnowledgeArtifactRepositories();
    assertTrue(ans.isSuccess());
    assertEquals(1, ans.orElse(Collections.emptyList()).size());

    KnowledgeArtifactRepository descr = ans.map(l -> l.get(0)).orElseGet(Assertions::fail);
    assertTrue(descr.isDefaultRepository());
  }

  @Test
  void testSetRepositoryWithDefault() {
    Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> ans =
        repo.setKnowledgeArtifactRepository("repository", new KnowledgeArtifactRepository());
    assertEquals(ResponseCodeSeries.NotImplemented, ans.getOutcomeType());
  }

  @Test
  void testInitRepositoryWithDefault() {
    Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> ans =
        repo.initKnowledgeArtifactRepository();
    assertEquals(ResponseCodeSeries.NotImplemented, ans.getOutcomeType());
  }

  @Test
  void testIsRepositoryWithDefault() {
    Answer<Void> ans = repo
        .isKnowledgeArtifactRepository("repository");
    assertEquals(ResponseCodeSeries.NotImplemented, ans.getOutcomeType());
  }

  @Test
  void testGetRepositoryWithNonDefault() {
    Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> ans =
        repo.getKnowledgeArtifactRepository("repository");
    assertEquals(ResponseCodeSeries.NotFound, ans.getOutcomeType());
  }

  @Test
  void testGetRepositoryWithDefault() {
    Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> ans =
        repo.getKnowledgeArtifactRepository("TestRepo");
    assertEquals(ResponseCodeSeries.OK, ans.getOutcomeType());
  }

  @Test
  void testDisableRepositoryWithDefault() {
    Answer<Void> ans =
        repo.disableKnowledgeArtifactRepository("repository");
    assertEquals(ResponseCodeSeries.NotImplemented, ans.getOutcomeType());
  }

}
