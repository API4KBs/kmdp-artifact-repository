package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.repository.artifact.server.KnowledgeArtifactApiDelegate;
import edu.mayo.kmdp.repository.artifact.server.KnowledgeArtifactRepositoryApiDelegate;
import edu.mayo.kmdp.repository.artifact.server.KnowledgeArtifactSeriesApiDelegate;

public interface KnowledgeArtifactRepository extends KnowledgeArtifactRepositoryApiDelegate,
    KnowledgeArtifactSeriesApiDelegate,
    KnowledgeArtifactApiDelegate {

}
