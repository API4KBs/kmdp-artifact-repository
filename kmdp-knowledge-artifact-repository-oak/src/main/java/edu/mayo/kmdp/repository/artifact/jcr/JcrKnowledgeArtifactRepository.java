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


import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.unsupported;

import edu.mayo.kmdp.repository.artifact.ClearableKnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.HrefBuilder;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryCore;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceIdentificationException;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.version.Version;
import org.apache.commons.io.IOUtils;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.PlatformComponentHelper;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.beans.factory.DisposableBean;

@KPServer
public class JcrKnowledgeArtifactRepository extends KnowledgeArtifactRepositoryCore {

  public JcrKnowledgeArtifactRepository(ArtifactDAO dao,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    super(dao, cfg);
  }

  public JcrKnowledgeArtifactRepository(JcrDao dao, KnowledgeArtifactRepositoryServerConfig cfg) {
    this(new JcrAdapter(dao), cfg);
  }

  public JcrKnowledgeArtifactRepository(Repository repo, KnowledgeArtifactRepositoryServerConfig cfg) {
    this(new JcrAdapter(repo), cfg);
  }

  public JcrKnowledgeArtifactRepository(Repository repo, Runnable cleanup,
      KnowledgeArtifactRepositoryServerConfig cfg) {
    this(new JcrAdapter(repo, cleanup), cfg);
  }

  //*********************************************************************************************/
  //* Constructors */
  //*********************************************************************************************/

}
