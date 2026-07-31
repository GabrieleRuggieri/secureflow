/**
 * Package delle entità JPA del Core Service.
 *
 * FilterDef "tenantFilter": definito una sola volta a livello package per evitare duplicati.
 * Usato da Role, Permission, User, RoleAssignment, ApiKey. Il parametro tenantId viene
 * impostato da TenantFilterEntityManagerFactory all'apertura di ogni EntityManager.
 */
@org.hibernate.annotations.FilterDef(
        name = "tenantFilter",
        parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = java.util.UUID.class)
)
package io.secureflow.core.entity;
