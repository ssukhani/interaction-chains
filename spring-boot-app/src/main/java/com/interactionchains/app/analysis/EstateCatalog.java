package com.interactionchains.app.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EstateCatalog {

    private final Map<String, ChainApplication> applications;
    private final Map<String, ChainOperation> operations;
    private final List<JourneyDefinition> journeys;
    private final Map<String, JourneyDefinition> journeysById;
    private final List<TraceRecord> traces;
    private final List<DeclaredEdge> declaredEdges;

    @Autowired
    public EstateCatalog() {
        this(readDocument(JsonMapper.builder().build()));
    }

    EstateCatalog(EstateDocument document) {
        this.applications = index(document.applications(), ChainApplication::id, "application");
        this.operations = index(document.operations(), ChainOperation::id, "operation");
        this.journeys = List.copyOf(document.journeys());
        this.journeysById = index(document.journeys(), JourneyDefinition::id, "journey");
        this.traces = List.copyOf(document.traces());
        this.declaredEdges = List.copyOf(document.declaredEdges());
        validate();
    }

    public List<JourneyDefinition> journeys() {
        return journeys;
    }

    public Optional<JourneyDefinition> findJourney(String id) {
        return Optional.ofNullable(journeysById.get(id));
    }

    public List<ChainOperation> operations() {
        return List.copyOf(operations.values());
    }

    public Optional<ChainOperation> findOperation(String applicationId, String operationId) {
        ChainOperation operation = operations.get(operationId);
        if (operation == null || !operation.applicationId().equals(applicationId)) {
            return Optional.empty();
        }
        return Optional.of(operation);
    }

    public Optional<TraceRecord> findTrace(String id) {
        return traces.stream().filter(trace -> trace.id().equals(id)).findFirst();
    }

    ChainApplication application(String id) {
        ChainApplication application = applications.get(id);
        if (application == null) {
            throw new IllegalStateException("Unknown application: " + id);
        }
        return application;
    }

    ChainOperation operation(String id) {
        ChainOperation operation = operations.get(id);
        if (operation == null) {
            throw new IllegalStateException("Unknown operation: " + id);
        }
        return operation;
    }

    List<TraceRecord> tracesFor(String journeyId) {
        return traces.stream().filter(trace -> trace.journeyId().equals(journeyId)).toList();
    }

    List<DeclaredEdge> declaredEdgesFor(String journeyId) {
        return declaredEdges.stream().filter(edge -> edge.journeyId().equals(journeyId)).toList();
    }

    boolean journeyContains(JourneyDefinition journey, String applicationId) {
        return journey.hops().stream().anyMatch(hop -> hop.applicationId().equals(applicationId));
    }

    private void validate() {
        for (ChainOperation operation : operations.values()) {
            requireApplication(operation.applicationId(), "operation " + operation.id());
        }
        for (JourneyDefinition journey : journeys) {
            if (journey.hops() == null || journey.hops().isEmpty()) {
                throw new IllegalStateException("Journey " + journey.id() + " has no hops.");
            }
            for (JourneyHopRef hop : journey.hops()) {
                requireApplication(hop.applicationId(), "journey " + journey.id());
                ChainOperation operation = operation(hop.operationId());
                if (!operation.applicationId().equals(hop.applicationId())) {
                    throw new IllegalStateException(
                            "Operation " + hop.operationId() + " does not belong to " + hop.applicationId());
                }
            }
        }
        for (TraceRecord trace : traces) {
            if (findJourney(trace.journeyId()).isEmpty()) {
                throw new IllegalStateException("Trace " + trace.id() + " names unknown journey " + trace.journeyId());
            }
            if (trace.spans() == null || trace.spans().isEmpty()) {
                throw new IllegalStateException("Trace " + trace.id() + " has no spans.");
            }
            for (TraceSpan span : trace.spans()) {
                requireApplication(span.applicationId(), "trace " + trace.id());
                operation(span.operationId());
            }
        }
        for (DeclaredEdge edge : declaredEdges) {
            if (findJourney(edge.journeyId()).isEmpty()) {
                throw new IllegalStateException("Declared edge " + edge.id() + " names unknown journey " + edge.journeyId());
            }
            requireApplication(edge.fromApplicationId(), "declared edge " + edge.id());
            requireApplication(edge.toApplicationId(), "declared edge " + edge.id());
        }
    }

    private void requireApplication(String id, String owner) {
        if (!applications.containsKey(id)) {
            throw new IllegalStateException(owner + " names unknown application " + id);
        }
    }

    private static EstateDocument readDocument(JsonMapper jsonMapper) {
        try (InputStream input = EstateCatalog.class.getResourceAsStream("/sample/account-opening-estate.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing sample estate on the classpath.");
            }
            return jsonMapper.readValue(input, EstateDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the sample estate.", exception);
        }
    }

    private static <T> Map<String, T> index(List<T> items, Function<T, String> id, String noun) {
        Map<String, T> indexed = new LinkedHashMap<>();
        for (T item : items) {
            String key = id.apply(item);
            if (indexed.put(key, item) != null) {
                throw new IllegalStateException("Duplicate " + noun + " id: " + key);
            }
        }
        return Map.copyOf(indexed);
    }
}
