package uk.gov.pay.webhooks.eventtype.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.eventtype.EventTypeName;

import jakarta.inject.Inject;
import java.util.Optional;

public class EventTypeDao extends AbstractDAO<EventTypeEntity> {

    @Inject
    public EventTypeDao(SessionFactory factory) {
        super(factory);
    }

    public Optional<EventTypeEntity> findByName(EventTypeName eventTypeName) {
        return Optional.ofNullable(namedTypedQuery(EventTypeEntity.GET_BY_NAME)
                .setParameter("name", eventTypeName.getName())
                .getSingleResult());
    }
}
