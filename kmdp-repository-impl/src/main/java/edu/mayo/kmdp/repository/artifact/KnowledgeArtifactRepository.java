package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.repository.server.KnowledgeArtifactApiDelegate;
import edu.mayo.kmdp.repository.server.KnowledgeArtifactRepositoryApiDelegate;
import edu.mayo.kmdp.repository.server.KnowledgeArtifactSeriesApiDelegate;

public interface KnowledgeArtifactRepository extends KnowledgeArtifactRepositoryApiDelegate,
    KnowledgeArtifactSeriesApiDelegate,
    KnowledgeArtifactApiDelegate {

}
