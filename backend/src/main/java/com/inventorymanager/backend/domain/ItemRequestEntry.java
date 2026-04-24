package com.inventorymanager.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.inventorymanager.backend.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "item_request_entries")
@JsonIgnoreProperties({"createdAt", "updatedAt"})
public class ItemRequestEntry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_request_id", nullable = false)
    private ItemRequest itemRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id")
    @JsonIgnoreProperties({"createdAt", "updatedAt"})
    private Item item;

    @Column(name = "requested_item_name")
    private String requestedItemName;

    @Column(name = "requested_quantity")
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_unit")
    private Item.UnitType requestedUnit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_category_id")
    @JsonIgnoreProperties({"createdAt", "updatedAt"})
    private Category requestedCategory;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_department_id")
    @JsonIgnoreProperties({"createdAt", "updatedAt"})
    private Department sourceDepartment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_department_id")
    @JsonIgnoreProperties({"createdAt", "updatedAt"})
    private Department targetDepartment;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "characteristics_json", columnDefinition = "TEXT")
    private String characteristicsJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ItemRequest getItemRequest() {
        return itemRequest;
    }

    public void setItemRequest(ItemRequest itemRequest) {
        this.itemRequest = itemRequest;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getRequestedItemName() {
        return requestedItemName;
    }

    public void setRequestedItemName(String requestedItemName) {
        this.requestedItemName = requestedItemName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Item.UnitType getRequestedUnit() {
        return requestedUnit;
    }

    public void setRequestedUnit(Item.UnitType requestedUnit) {
        this.requestedUnit = requestedUnit;
    }

    public Category getRequestedCategory() {
        return requestedCategory;
    }

    public void setRequestedCategory(Category requestedCategory) {
        this.requestedCategory = requestedCategory;
    }

    public Department getSourceDepartment() {
        return sourceDepartment;
    }

    public void setSourceDepartment(Department sourceDepartment) {
        this.sourceDepartment = sourceDepartment;
    }

    public Department getTargetDepartment() {
        return targetDepartment;
    }

    public void setTargetDepartment(Department targetDepartment) {
        this.targetDepartment = targetDepartment;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getCharacteristicsJson() {
        return characteristicsJson;
    }

    public void setCharacteristicsJson(String characteristicsJson) {
        this.characteristicsJson = characteristicsJson;
    }
}
