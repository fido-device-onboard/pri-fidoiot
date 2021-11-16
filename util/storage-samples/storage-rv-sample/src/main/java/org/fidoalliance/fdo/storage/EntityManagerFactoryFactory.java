package org.fidoalliance.fdo.storage;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;

public class EntityManagerFactoryFactory {

  private String persistenceUnitName = "";
  private Properties properties = new Properties();
  private List<String> entityClassNames = new LinkedList<>();

  /**
   * Build a new EntityManagerFactory with current settings.
   * @return the new EntityManagerFactory.
   */
  public EntityManagerFactory build() {
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(
        persistenceUnitInfo(persistenceUnitName, entityClassNames, properties),
        new HashMap<>());
  }

  public EntityManagerFactoryFactory(String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
  }

  public EntityManagerFactoryFactory withEntity(Class<?> cls) {
    entityClassNames.add(cls.getName());
    return this;
  }

  public EntityManagerFactoryFactory withProperty(String key, Object value) {
    properties.put(key, value);
    return this;
  }

  protected PersistenceUnitInfo persistenceUnitInfo(
      String persistenceUnitName,
      List<String> entityClassNames,
      Properties properties
  ) {
    return new PersistenceUnitInfo() {
      @Override
      public String getPersistenceUnitName() {
        return persistenceUnitName;
      }

      @Override
      public String getPersistenceProviderClassName() {
        return HibernatePersistenceProvider.class.getName();
      }

      @Override
      public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
      }

      @Override
      public DataSource getJtaDataSource() {
        return null;
      }

      @Override
      public DataSource getNonJtaDataSource() {
        return null;
      }

      @Override
      public List<String> getMappingFileNames() {
        return Collections.emptyList();
      }

      @Override
      public List<URL> getJarFileUrls() {
        return Collections.emptyList();
      }

      @Override
      public URL getPersistenceUnitRootUrl() {
        return null;
      }

      @Override
      public List<String> getManagedClassNames() {
        return entityClassNames;
      }

      @Override
      public boolean excludeUnlistedClasses() {
        return false;
      }

      @Override
      public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.UNSPECIFIED;
      }

      @Override
      public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
      }

      @Override
      public Properties getProperties() {
        return properties;
      }

      @Override
      public String getPersistenceXMLSchemaVersion() {
        return "2.1";
      }

      @Override
      public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
      }

      @Override
      public void addTransformer(ClassTransformer classTransformer) {}

      @Override
      public ClassLoader getNewTempClassLoader() {
        return null;
      }
    };
  }
}
