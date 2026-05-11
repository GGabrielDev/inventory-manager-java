package com.inventorymanager.backend.audit;

import java.util.Map;
import java.util.Optional;
import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final Javers javers;
    private final EntityRegistry entityRegistry;

    public AuditService(Javers javers, EntityRegistry entityRegistry) {
        this.javers = javers;
        this.entityRegistry = entityRegistry;
    }

    private String safeActor(Long actorId) {
        return actorId == null ? "system" : String.valueOf(actorId);
    }

    public void commitCreate(Long actorId, Object entity) {
        javers.commit(safeActor(actorId), entity, Map.of("operation", "create"));
    }

    public void commitUpdate(Long actorId, Object entity) {
        javers.commit(safeActor(actorId), entity, Map.of("operation", "update"));
    }

    public void commitDelete(Long actorId, Object entity) {
        javers.commit(safeActor(actorId),
                Map.of(
                        "operation", "delete",
                        "entity", entity.getClass().getSimpleName(),
                        "entityId", entityRegistry.entityId(entity)
                ),
                Map.of("operation", "delete"));
    }

    public void commitLink(Long actorId, String relation, Long parentId, Long childId) {
        javers.commit(safeActor(actorId),
                Map.of("relation", relation, "parentId", parentId, "childId", childId),
                Map.of("operation", "link"));
    }

    public void commitUnlink(Long actorId, String relation, Long parentId, Long childId) {
        javers.commit(safeActor(actorId),
                Map.of("relation", relation, "parentId", parentId, "childId", childId),
                Map.of("operation", "unlink"));
    }

    public PageResponse auditTrail(String entityName, Long id, int page, int pageSize) {
        if (entityName == null || entityName.isBlank()) {
            return new PageResponse(java.util.Collections.emptyList(), 0, 0, 1);
        }
        Class<?> entityClass = entityRegistry.resolve(entityName);
        var queryBuilder = id == null ? QueryBuilder.byClass(entityClass) : QueryBuilder.byInstanceId(id, entityClass);

        var snapshots = javers.findSnapshots(
                queryBuilder
                        .skip(page * pageSize)
                        .limit(pageSize)
                        .build()
        );
        var data = snapshots.stream()
                .map(snapshot -> {
                    Map<String, Object> stateMap = new java.util.HashMap<>();
                    snapshot.getState().getPropertyNames().forEach(name ->
                            stateMap.put(name, snapshot.getState().getPropertyValue(name))
                    );
                    return new AuditEntryResponse(
                            snapshot.getCommitMetadata().getProperties().getOrDefault("operation", "update"),
                            snapshot.getCommitMetadata().getAuthor(),
                            snapshot.getCommitMetadata().getCommitDateInstant(),
                            stateMap
                    );
                })
                .toList();

        // Pagination metadata: Set total to -1 (unknown) if there might be more pages, 
        // to avoid expensive and OOM-prone count(*) queries in JaVers snapshots.
        long total = (snapshots.size() == pageSize) ? -1 : (page * pageSize + snapshots.size());
        int totalPages = (total == -1) ? -1 : (int) Math.ceil((double) total / Math.max(pageSize, 1));
        return new PageResponse(data, total, totalPages, page + 1);
    }

    public record PageResponse(java.util.List<AuditEntryResponse> data, long total, int totalPages, int currentPage) {}
}
