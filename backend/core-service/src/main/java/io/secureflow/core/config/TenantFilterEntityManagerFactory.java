/*
 * TenantFilterEntityManagerFactory — Wrapper che abilita il filtro tenant sugli EM.
 *
 * Delegate pattern: ogni createEntityManager() delega al factory reale, poi se
 * TenantContext ha un tenantId abilita session.enableFilter("tenantFilter")
 * sull'EntityManager. Così tutte le query JPA sono automaticamente filtrate per
 * tenant_id senza modificare repository o service.
 */
package io.secureflow.core.config;

import io.secureflow.core.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.Session;

import java.util.Map;

/**
 * Wraps the real EntityManagerFactory to enable the tenant filter on each EntityManager
 * when it's created, using the tenant ID from TenantContext.
 */
public class TenantFilterEntityManagerFactory implements EntityManagerFactory {

    private final EntityManagerFactory delegate;

    public TenantFilterEntityManagerFactory(EntityManagerFactory delegate) {
        this.delegate = delegate;
    }

    /**
     * Ogni volta che Spring/JPA crea un EntityManager (es. all'inizio di una transazione),
     * controlliamo se TenantContext ha un tenantId. Se sì, unwrap alla Session Hibernate
     * e abilitiamo il filtro "tenantFilter" con quel parametro. Da quel momento tutte le
     * query su entity con @Filter(tenantFilter) avranno WHERE tenant_id = :tenantId.
     */
    @Override
    public EntityManager createEntityManager() {
        EntityManager em = delegate.createEntityManager();
        return enableTenantFilterIfNeeded(em);
    }

    @Override
    public EntityManager createEntityManager(Map properties) {
        EntityManager em = delegate.createEntityManager(properties);
        return enableTenantFilterIfNeeded(em);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        EntityManager em = delegate.createEntityManager(synchronizationType);
        return enableTenantFilterIfNeeded(em);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map properties) {
        EntityManager em = delegate.createEntityManager(synchronizationType, properties);
        return enableTenantFilterIfNeeded(em);
    }

    /**
     * Abilita il filtro solo se TenantContext ha un tenantId (richiesta autenticata).
     * session.enableFilter("tenantFilter").setParameter("tenantId", tenantId): da ora
     * tutte le query su User, Role, Permission, ecc. avranno WHERE tenant_id = :tenantId.
     */
    private EntityManager enableTenantFilterIfNeeded(EntityManager em) {
        TenantContext.getTenantId().ifPresent(tenantId -> {
            Session session = em.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        });
        return em;
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return delegate.getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return delegate.getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public jakarta.persistence.Cache getCache() {
        return delegate.getCache();
    }

    @Override
    public jakarta.persistence.PersistenceUnitUtil getPersistenceUnitUtil() {
        return delegate.getPersistenceUnitUtil();
    }

    @Override
    public <T> void addNamedEntityGraph(String name, jakarta.persistence.EntityGraph<T> entityGraph) {
        delegate.addNamedEntityGraph(name, entityGraph);
    }

    @Override
    public void addNamedQuery(String name, jakarta.persistence.Query query) {
        delegate.addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return delegate.unwrap(cls);
    }
}
