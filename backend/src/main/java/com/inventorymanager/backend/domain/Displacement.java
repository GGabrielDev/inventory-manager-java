package com.inventorymanager.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.inventorymanager.backend.common.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "displacements")
@SQLDelete(sql = "UPDATE displacements SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@JsonIgnoreProperties({"createdAt", "updatedAt", "deletedAt"})
public class Displacement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bag_id")
    private Bag bag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "borrower_name", nullable = false)
    private String borrowerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisplacementStatus status = DisplacementStatus.ACTIVE;

    @Column(name = "removed_at", nullable = false)
    private OffsetDateTime removedAt = OffsetDateTime.now();

    @Column(name = "expected_return_date")
    private OffsetDateTime expectedReturnDate;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bag getBag() {
        return bag;
    }

    public void setBag(Bag bag) {
        this.bag = bag;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public DisplacementStatus getStatus() {
        return status;
    }

    public void setStatus(DisplacementStatus status) {
        this.status = status;
    }

    public OffsetDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(OffsetDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public OffsetDateTime getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(OffsetDateTime expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
