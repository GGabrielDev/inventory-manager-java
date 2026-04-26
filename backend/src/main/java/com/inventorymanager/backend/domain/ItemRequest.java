package com.inventorymanager.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.inventorymanager.backend.common.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_requests")
@JsonIgnoreProperties({"createdAt", "updatedAt"})
public class ItemRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private ItemRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemRequestStatus status = ItemRequestStatus.DRAFT;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "roles", "createdAt", "updatedAt", "branch"})
    private User requestedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by_user_id")
    @JsonIgnoreProperties({"passwordHash", "roles", "createdAt", "updatedAt", "branch"})
    private User reviewedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "executed_by_user_id")
    @JsonIgnoreProperties({"passwordHash", "roles", "createdAt", "updatedAt", "branch"})
    private User executedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_branch_id")
    @JsonIgnoreProperties("departments")
    private Branch targetBranch;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @OneToMany(mappedBy = "itemRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"itemRequest", "createdAt", "updatedAt"})
    private List<ItemRequestEntry> entries = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ItemRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(ItemRequestType requestType) {
        this.requestType = requestType;
    }

    public ItemRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ItemRequestStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public User getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(User executedBy) {
        this.executedBy = executedBy;
    }

    public Branch getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(Branch targetBranch) {
        this.targetBranch = targetBranch;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public List<ItemRequestEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ItemRequestEntry> entries) {
        this.entries.clear();
        if (entries != null) {
            for (ItemRequestEntry entry : entries) {
                addEntry(entry);
            }
        }
    }

    public void addEntry(ItemRequestEntry entry) {
        entry.setItemRequest(this);
        this.entries.add(entry);
    }
}
