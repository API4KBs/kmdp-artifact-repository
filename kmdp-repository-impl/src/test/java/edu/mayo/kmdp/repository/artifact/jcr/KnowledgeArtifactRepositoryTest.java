package edu.mayo.kmdp.repository.artifact.jcr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.jcr.Repository;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class KnowledgeArtifactRepositoryTest {

  private String TYPE_NAME = JcrKnowledgeArtifactRepository.JcrTypes.ARTIFACT.name();

  private JcrKnowledgeArtifactRepository repo;
  private JcrDao dao;

  @BeforeEach
  void repo() throws IOException, InvalidFileStoreVersionException {
    Repository jcr = new Jcr(new Oak()).with(new OpenSecurityProvider()).createRepository();

    dao = new JcrDao(jcr, Collections.singletonList(TYPE_NAME));

    repo = new JcrKnowledgeArtifactRepository(dao, new KnowledgeArtifactRepositoryServerConfig());
  }

  @Test
  public void testListRepository() {
    ResponseEntity<List<Pointer>> ans = repo.listKnowledgeArtifactRepositories();
    assertEquals(HttpStatus.OK, ans.getStatusCode());
    assertEquals(1, ans.getBody().size());

    Pointer ptr = ans.getBody().get(0);
    assertEquals( "Default", ptr.getName() );
  }


//  ResponseEntity<Void> deleteKnowledgeArtifactRepository(String repositoryId);
//
//  ResponseEntity<org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository> getKnowledgeArtifactRepository(String repositoryId);
//
//  ResponseEntity<Void> initKnowledgeArtifactRepository(org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository repositoryDescr);
//  ResponseEntity<Void> isKnowledgeArtifactRepository(String repositoryId);
//
//  ResponseEntity<List<org.omg.spec.api4kp._1_0.identifiers.Pointer>> listKnowledgeArtifactRepositories();
//
//  ResponseEntity<Void> setKnowledgeArtifactRepository(String repositoryId,
//      org.omg.spec.api4kp._1_0.services.repository.KnowledgeArtifactRepository repositoryDescr);

}
