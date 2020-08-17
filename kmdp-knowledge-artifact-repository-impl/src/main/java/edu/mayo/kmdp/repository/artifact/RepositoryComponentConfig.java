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
import java.io.File;
import java.io.IOException;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan
public class RepositoryComponentConfig {

  private KnowledgeArtifactRepositoryServerConfig cfg = new KnowledgeArtifactRepositoryServerConfig();

  @Bean
  @Profile("default")
  @KPServer
  public KnowledgeArtifactRepositoryService repository() throws InvalidFileStoreVersionException, IOException {
    File dataDir = cfg.getTyped(
        KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions.BASE_DIR);
    if (!dataDir.exists()) {
      boolean dirCreationSuccess = dataDir.mkdir();
      if (!dirCreationSuccess) {
        throw new IOException("Unable to create data dir " + dataDir);
      }
    }

    FileStore fs = FileStoreBuilder.fileStoreBuilder(dataDir).build();
    SegmentNodeStore ns = SegmentNodeStoreBuilders.builder(fs).build();
    return new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak(ns)).createRepository(), fs::close, cfg);
  }

  @Bean
  @Profile("inmemory")
  @KPServer
  public KnowledgeArtifactRepositoryService inMemoryRepository() {
    return new JcrKnowledgeArtifactRepository(new Jcr(new Oak()).createRepository(), cfg);
  }

}
