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
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactSeriesApiInternal;

public interface KnowledgeArtifactRepositoryService extends KnowledgeArtifactRepositoryApiInternal,
    KnowledgeArtifactSeriesApiInternal, KnowledgeArtifactApiInternal {

  static KnowledgeArtifactRepositoryService inMemoryArtifactRepository() {
    return new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(),
        new KnowledgeArtifactRepositoryServerConfig());
  }

}


