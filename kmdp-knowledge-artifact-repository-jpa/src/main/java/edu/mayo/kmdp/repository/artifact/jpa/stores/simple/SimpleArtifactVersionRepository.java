package edu.mayo.kmdp.repository.artifact.jpa.stores.simple;

import static edu.mayo.kmdp.repository.artifact.jpa.entities.ArtifactVersionEntity.pattern;

import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.jpa.entities.ArtifactVersionEntity;
import edu.mayo.kmdp.repository.artifact.jpa.entities.KeyId;
import edu.mayo.kmdp.repository.artifact.jpa.stores.ArtifactVersionRepository;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Static implementation of the internal JPA Artifact Repository interface Should only be used for
 * testing purpose.
 * <p>
 * https://www.baeldung.com/the-persistence-layer-with-spring-and-jpa
 */
public class SimpleArtifactVersionRepository
    extends SimpleJpaRepository<ArtifactVersionEntity, KeyId>
    implements ArtifactVersionRepository, Closeable {

  public EntityManager emRef;

  public static SimpleArtifactVersionRepository simpleRepo(DataSource ds) {
    EntityManagerFactory emf = emfProvider(ds).getObject();
    EntityManager em = emf.createEntityManager();
    return new SimpleArtifactVersionRepository(em);
  }

  private SimpleArtifactVersionRepository(EntityManager em) {
    super(ArtifactVersionEntity.class, em);
    this.emRef = em;
  }

  public void close() {
    emRef.close();
  }

  @Override
  public <S extends ArtifactVersionEntity> S save(S entity) {
    EntityTransaction tx = emRef.getTransaction();
    tx.begin();
    S s = super.save(entity);
    tx.commit();
    return s;
  }

  @Override
  public void deleteAll() {
    EntityTransaction tx = emRef.getTransaction();
    tx.begin();
    super.deleteAll();
    tx.commit();
  }

  private Optional<ArtifactVersionEntity> findFirst(Specification<ArtifactVersionEntity> spec) {
    return getQuery(spec, PageRequest.of(0,1)).getResultStream().findFirst();
  }


  @Override
  public List<Artifact> findAllByKey_RepositoryIdAndSeries(String repositoryId, boolean series) {
    return new ArrayList<>(
        findAll((root, cq, cb) ->
            cb.and(
                cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
                cb.equal(root.get(AE.SERIES), series))));
  }


  @Override
  public List<Artifact> findAllByKey_RepositoryIdAndSeriesAndSoftDeleted(String repositoryId,
      boolean series, boolean softDeleted) {
    return new ArrayList<>(
        findAll((root, cq, cb) ->
            cb.and(
                cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
                cb.equal(root.get(AE.SERIES), series),
                cb.equal(root.get(AE.SOFT_DELETED), softDeleted))));
  }

  @Override
  public List<ArtifactVersion> findAllByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeletedOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series, boolean softDeleted) {
    return new ArrayList<>(
        findAll((root, cq, cb) ->
            cb.and(
                cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
                cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
                cb.equal(root.get(AE.SERIES), series),
                cb.equal(root.get(AE.SOFT_DELETED), softDeleted))));
  }


  @Override
  public List<ArtifactVersion> findAllByKey_RepositoryIdAndKey_ArtifactIdAndSeriesOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series) {
    return new ArrayList<>(
        findAll((root, cq, cb) -> {
          cq.orderBy(cb.desc(root.get(AE.CREATED)));
          return cb.and(
              cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
              cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
              cb.equal(root.get(AE.SERIES), series));
        }));
  }

  @Override
  public Optional<ArtifactVersion> findFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeletedOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series, boolean softDeleted) {

    return
        findFirst((root, cq, cb) -> {
          cq.orderBy(cb.desc(root.get(AE.CREATED)));
          return cb.and(
              cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
              cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
              cb.equal(root.get(AE.SOFT_DELETED), softDeleted),
              cb.equal(root.get(AE.SERIES), series));
        }).map(ArtifactVersion.class::cast);
  }

  @Override
  public Optional<ArtifactVersion> findFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeriesOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series) {
    return
        findFirst((root, cq, cb) -> {
          cq.orderBy(cb.desc(root.get(AE.CREATED)));
          return cb.and(
              cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
              cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
              cb.equal(root.get(AE.SERIES), series));
        }).map(ArtifactVersion.class::cast);
  }


  @Override
  public List<ArtifactVersionEntity> getArtifactVersionEntityByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeleted(
      String repositoryId, UUID artifactId, boolean series, boolean softDeleted) {
    return
        findAll((root, cq, cb) ->
            cb.and(
                cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
                cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
                cb.equal(root.get(AE.SERIES), series),
                cb.equal(root.get(AE.SOFT_DELETED), softDeleted)));
  }

  @Override
  public List<ArtifactVersionEntity> getArtifactVersionEntityByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
      String repositoryId, UUID artifactId, boolean series) {
    return
        findAll((root, cq, cb) ->
            cb.and(
                cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
                cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
                cb.equal(root.get(AE.SERIES), series)));
  }


  @Override
  public Optional<ArtifactVersionEntity> getFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
      String repositoryId, UUID artifactId, boolean series) {
    return
        findOne((root, cq, cb) ->
            cb.and(
                cb.equal(root.get(AE.KEY).get(K.REPOSITORY_ID), repositoryId),
                cb.equal(root.get(AE.KEY).get(K.ARTIFACT_ID), artifactId),
                cb.equal(root.get(AE.SERIES), series)));
  }


  @Override
  public boolean existsByKey_RepositoryId(String repositoryId) {
    return exists(
        Example.of(
            pattern()
                .withRepositoryId(repositoryId)
        ));
  }

  @Override
  public boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndSeries(String repositoryId,
      UUID artifactId, boolean isSeries) {
    return exists(
        Example.of(
            pattern()
                .withRepositoryId(repositoryId)
                .withArtifactId(artifactId)
                .withSeries(isSeries)
        ));
  }

  @Override
  public boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeleted(
      String repositoryId, UUID artifactId, boolean isSeries, boolean softDeleted) {
    return exists(
        Example.of(
            pattern()
                .withRepositoryId(repositoryId)
                .withArtifactId(artifactId)
                .withSeries(isSeries)
                .withSoftDeleted(softDeleted)
        ));
  }

  @Override
  public boolean existsByKey_RepositoryIdAndKey_ArtifactId(String repositoryId, UUID artifactId) {
    return exists(
        Example.of(
            pattern()
                .withRepositoryId(repositoryId)
                .withArtifactId(artifactId)
        ));
  }

  @Override
  public boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndSoftDeleted(String repositoryId,
      UUID artifactId, boolean softDeleted) {
    return exists(
        Example.of(
            pattern()
                .withRepositoryId(repositoryId)
                .withArtifactId(artifactId)
                .withSoftDeleted(softDeleted)
        ));
  }


  /*******************/

  private static EntityManager entityManager(EntityManagerFactory emf) {
    return emf.createEntityManager();
  }

  private static EntityManagerFactory entityManagerFactory(DataSource ds) {
    EntityManagerFactory emf = emfProvider(ds).getObject();
    if (emf == null) {
      throw new UnsupportedOperationException("Unable to instantiate Artifact Persistence Layer");
    }
    return emf;
  }

  private static LocalContainerEntityManagerFactoryBean emfProvider(
      DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean emfb
        = new LocalContainerEntityManagerFactoryBean();
    emfb.setDataSource(dataSource);
    emfb.setPackagesToScan(
        ArtifactVersionEntity.class.getPackageName(),
        AE.class.getPackageName());

    JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    emfb.setJpaVendorAdapter(vendorAdapter);
    emfb.setJpaProperties(additionalProperties());

    emfb.afterPropertiesSet();

    return emfb;
  }

  private static PlatformTransactionManager transactionManager(
      EntityManagerFactory emf) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(emf);

    return transactionManager;
  }

  private static PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }

  private static Properties additionalProperties() {
    Properties properties = new Properties();
    properties.setProperty("hibernate.hbm2ddl.auto", "create");

    return properties;
  }

}