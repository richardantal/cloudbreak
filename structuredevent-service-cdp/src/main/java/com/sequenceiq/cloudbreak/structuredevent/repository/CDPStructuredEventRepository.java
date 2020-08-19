package com.sequenceiq.cloudbreak.structuredevent.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CDPStructuredEventEntity.class)
@Transactional(TxType.REQUIRED)
@Repository
public interface CDPStructuredEventRepository extends AccountAwareResourceRepository<CDPStructuredEventEntity, Long> {

    @Override
    CDPStructuredEventEntity save(CDPStructuredEventEntity entity);

    @Query("SELECT se from CDPStructuredEventEntity se WHERE se.accountId = :accountId AND se.id = :id")
    CDPStructuredEventEntity findByAccountIdAndId(
            @Param("accountId") Long accountId,
            @Param("id") Long id);

    List<CDPStructuredEventEntity> findByAccountIdAndResourceTypeAndResourceCrn(
            String accountId,
            String resourceType,
            String resourceCrn);

    List<CDPStructuredEventEntity> findByAccountIdAndEventType(
            String accountId,
            StructuredEventType eventType);

    @Query("SELECT se from CDPStructuredEventEntity se WHERE se.accountId = :accountId AND se.eventType = :eventType AND se.timestamp >= :since")
    List<CDPStructuredEventEntity> findByAccountIdAndEventTypeSince(
            @Param("accountId") String accountId,
            @Param("eventType") StructuredEventType eventType,
            @Param("since") Long since);

    @Override
    default Optional<CDPStructuredEventEntity> findByNameAndAccountId(
            String name,
            String accountId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<CDPStructuredEventEntity> findByNameInAndAccountId(Set<String> name, String accountId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Optional<CDPStructuredEventEntity> findByResourceCrnAndAccountId(
            String crn,
            String accountId) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<CDPStructuredEventEntity> findByResourceCrnInAndAccountId(Set<String> crn, String accountId) {
        throw new UnsupportedOperationException();
    }
}