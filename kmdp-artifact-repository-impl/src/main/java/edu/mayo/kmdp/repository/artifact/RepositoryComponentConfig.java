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

import static edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE;

import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import java.io.File;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan
public class RepositoryComponentConfig {

  @Bean
  @Profile("default")
  public KnowledgeArtifactRepository repository() throws Exception {
    File dataDir = new File(
        new KnowledgeArtifactRepositoryServerConfig().getTyped(BASE_NAMESPACE, File.class),
        "repo-data");

//    FileStore fs = FileStoreBuilder.fileStoreBuilder(dataDir).build();
//    SegmentNodeStore ns = SegmentNodeStoreBuilders.builder(fs).build();


    return new JcrKnowledgeArtifactRepository(JcrUtils.getRepository(), //fs::close,
        new KnowledgeArtifactRepositoryServerConfig());
  }

  @Bean
  @Profile("inmemory")
  public KnowledgeArtifactRepository inMemoryRepository() throws Exception {
    return new JcrKnowledgeArtifactRepository(new TransientRepository(),
        new KnowledgeArtifactRepositoryServerConfig());
  }

}
