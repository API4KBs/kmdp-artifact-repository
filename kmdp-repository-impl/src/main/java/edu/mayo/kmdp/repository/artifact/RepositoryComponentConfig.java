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
