package uk.gov.pay.webhooks.eventtype.dao;

import uk.gov.pay.webhooks.eventtype.EventTypeName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@NamedQuery(
        name = EventTypeEntity.GET_BY_NAME,
        query = "select e from EventTypeEntity e where nameValue = :name"
)
@Entity
@SequenceGenerator(name="event_types_id_seq", sequenceName = "event_types_id_seq", allocationSize = 1)
@Table(name = "event_types")
public class EventTypeEntity {
    public static final String GET_BY_NAME = "EventTypeEntity.get_by_name";
    //for JPA
    public EventTypeEntity() {}
    
    public EventTypeEntity(EventTypeName name) {
        this.name = name;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_types_id_seq")
    private Long id;
    
    @Transient
    private EventTypeName name;
    
    @Column(name = "name")
    private String nameValue;

    public EventTypeName getName() {
        return name;
    }

    @PostLoad
    void fillTransient() {
        if (nameValue != null) {
            this.name = EventTypeName.of(nameValue);
        }
    }

    @PrePersist
    void fillPersistent() {
        if (name != null) {
            this.nameValue = name.getName();
        }
    }
}
