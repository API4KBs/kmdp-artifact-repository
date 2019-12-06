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

import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.v3.server.KnowledgeArtifactApiInternal;
import edu.mayo.kmdp.repository.artifact.v3.server.KnowledgeArtifactRepositoryApiInternal;
import edu.mayo.kmdp.repository.artifact.v3.server.KnowledgeArtifactSeriesApiInternal;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;

public interface KnowledgeArtifactRepositoryService extends KnowledgeArtifactRepositoryApiInternal,
    KnowledgeArtifactSeriesApiInternal, KnowledgeArtifactApiInternal {

  static KnowledgeArtifactRepositoryService inMemoryArtifactRepository() {
    return new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(),
        new KnowledgeArtifactRepositoryServerConfig());
  }

}


