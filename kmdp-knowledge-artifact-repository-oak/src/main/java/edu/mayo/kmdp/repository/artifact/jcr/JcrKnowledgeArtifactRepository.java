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
package edu.mayo.kmdp.repository.artifact.jcr;


import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryCore;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import javax.jcr.Repository;
import org.omg.spec.api4kp._20200801.services.KPServer;

@KPServer
public class JcrKnowledgeArtifactRepository extends KnowledgeArtifactRepositoryCore {

  public JcrKnowledgeArtifactRepository(ArtifactDAO dao,
      KnowledgeArtifactRepositoryServerProperties cfg) {
    super(dao, cfg);
  }

  public JcrKnowledgeArtifactRepository(JcrDao dao, KnowledgeArtifactRepositoryServerProperties cfg) {
    this(new JcrAdapter(dao), cfg);
  }

  public JcrKnowledgeArtifactRepository(Repository repo, KnowledgeArtifactRepositoryServerProperties cfg) {
    this(new JcrAdapter(repo), cfg);
  }

  public JcrKnowledgeArtifactRepository(Repository repo, Runnable cleanup,
      KnowledgeArtifactRepositoryServerProperties cfg) {
    this(new JcrAdapter(repo, cleanup), cfg);
  }

  //*********************************************************************************************/
  //* Constructors */
  //*********************************************************************************************/

}
