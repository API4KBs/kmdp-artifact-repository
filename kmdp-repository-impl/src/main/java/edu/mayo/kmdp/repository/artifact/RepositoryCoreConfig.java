package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.repository.artifact.jcr.JcrRepositoryAdapter;
import java.io.File;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan
public class RepositoryCoreConfig {

  @Bean
  @Profile("default")
  public KnowledgeArtifactRepository repository() throws Exception {
    File dataDir = new File(ServerConfig.getConfigDir(), "repo-data");
    dataDir.mkdir();

    FileStore fs = FileStoreBuilder.fileStoreBuilder(dataDir).build();
    SegmentNodeStore ns = SegmentNodeStoreBuilders.builder(fs).build();

    return new JcrRepositoryAdapter(new Jcr(new Oak(ns)).createRepository(), () -> fs.close());
  }

  @Bean
  @Profile("inmemory")
  public KnowledgeArtifactRepository inMemoryRepository() throws Exception {
    return new JcrRepositoryAdapter(new Jcr(new Oak()).createRepository());
  }

}
