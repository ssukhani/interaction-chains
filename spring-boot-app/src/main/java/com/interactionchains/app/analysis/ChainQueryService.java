package com.interactionchains.app.analysis;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChainQueryService {

    private final EstateCatalog catalog;

    public ChainQueryService(EstateCatalog catalog) {
        this.catalog = catalog;
    }

    public List<OperationChoice> typeahead(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        return catalog.operations().stream()
                .map(operation -> choice(operation))
                .filter(choice -> needle.isEmpty() || matches(choice, needle))
                .sorted((left, right) -> {
                    int byApplication = left.applicationName().compareToIgnoreCase(right.applicationName());
                    if (byApplication != 0) {
                        return byApplication;
                    }
                    return left.operationName().compareToIgnoreCase(right.operationName());
                })
                .toList();
    }

    public ImpactResponse impact(String applicationId, String operationId) {
        ChainOperation operation = catalog.findOperation(applicationId, operationId)
                .orElseThrow(() -> new UnknownCatalogItemException(
                        "Unknown operation " + operationId + " on application " + applicationId + "."));
        ChainApplication application = catalog.application(applicationId);
        List<ChainView> journeys = new ArrayList<>();
        for (JourneyDefinition journey : catalog.journeys()) {
            boolean contains = journey.hops().stream()
                    .anyMatch(hop -> hop.applicationId().equals(applicationId) && hop.operationId().equals(operationId));
            if (contains) {
                journeys.add(chain(journey, applicationId, operationId, false));
            }
        }
        return new ImpactResponse(applicationId, application.name(), operationId, operation.name(), List.copyOf(journeys));
    }

    public TraceView trace(String traceId) {
        TraceRecord trace = catalog.findTrace(traceId)
                .orElseThrow(() -> new UnknownCatalogItemException("Unknown trace " + traceId + "."));
        JourneyDefinition journey = catalog.findJourney(trace.journeyId())
                .orElseThrow(() -> new IllegalStateException("Trace " + traceId + " has no journey."));
        List<TraceHopView> hops = new ArrayList<>();
        int sequence = 1;
        for (TraceSpan span : trace.spans()) {
            hops.add(new TraceHopView(
                    sequence++,
                    catalog.application(span.applicationId()).name(),
                    catalog.operation(span.operationId()).name(),
                    span.time(),
                    span.status()));
        }
        return new TraceView(trace.id(), journey.id(), journey.name(), List.copyOf(hops));
    }

    public ChainView chain(JourneyDefinition journey, String highlightApplicationId, String highlightOperationId, boolean insertSanctions) {
        List<ResolvedHop> hops = new ArrayList<>(resolveHops(journey));
        if (insertSanctions) {
            insertSanctions(hops);
        }
        if (highlightApplicationId != null && highlightOperationId != null) {
            for (int index = 0; index < hops.size(); index++) {
                ResolvedHop hop = hops.get(index);
                if (highlightApplicationId.equals(hop.applicationId()) && highlightOperationId.equals(hop.operationId())) {
                    hops.set(index, hop.highlight());
                }
            }
        }
        List<ResolvedHop> numbered = new ArrayList<>();
        for (int index = 0; index < hops.size(); index++) {
            numbered.add(hops.get(index).atSequence(index + 1));
        }
        return new ChainView(
                journey.id(),
                journey.name(),
                journey.channel(),
                catalog.tracesFor(journey.id()).stream().map(TraceRecord::id).toList(),
                List.copyOf(numbered),
                sideFacts(journey));
    }

    private List<ResolvedHop> resolveHops(JourneyDefinition journey) {
        List<JourneyHopRef> refs = journey.hops();
        List<ResolvedHop> hops = new ArrayList<>();
        for (int index = 0; index < refs.size(); index++) {
            JourneyHopRef ref = refs.get(index);
            ChainApplication application = catalog.application(ref.applicationId());
            ChainOperation operation = catalog.operation(ref.operationId());
            List<String> traceIds = observedTraceIds(journey.id(), ref);
            List<Evidence> evidence = new ArrayList<>();
            if (!traceIds.isEmpty()) {
                evidence.add(new Evidence("observed", String.join(", ", traceIds)));
            }
            for (String source : declaredSources(journey, refs, index)) {
                evidence.add(new Evidence("declared", source));
            }
            if (evidence.isEmpty()) {
                evidence.add(new Evidence("inferred", null));
            }
            hops.add(new ResolvedHop(
                    index + 1,
                    application.id(),
                    application.name(),
                    application.team(),
                    operation.id(),
                    operation.name(),
                    List.copyOf(evidence),
                    !traceIds.isEmpty(),
                    false,
                    false));
        }
        return hops;
    }

    private List<String> observedTraceIds(String journeyId, JourneyHopRef ref) {
        return catalog.tracesFor(journeyId).stream()
                .filter(trace -> trace.spans().stream().anyMatch(span ->
                        span.applicationId().equals(ref.applicationId()) && span.operationId().equals(ref.operationId())))
                .map(TraceRecord::id)
                .toList();
    }

    private List<String> declaredSources(JourneyDefinition journey, List<JourneyHopRef> refs, int index) {
        String applicationId = refs.get(index).applicationId();
        String nextId = index + 1 < refs.size() ? refs.get(index + 1).applicationId() : null;
        String previousId = index > 0 ? refs.get(index - 1).applicationId() : null;
        List<String> sources = new ArrayList<>();
        for (DeclaredEdge edge : catalog.declaredEdgesFor(journey.id())) {
            boolean startsHere = applicationId.equals(edge.fromApplicationId()) && edge.toApplicationId().equals(nextId);
            boolean arrivesHere = applicationId.equals(edge.toApplicationId()) && edge.fromApplicationId().equals(previousId);
            if ((startsHere || arrivesHere) && !sources.contains(edge.source())) {
                sources.add(edge.source());
            }
        }
        return sources;
    }

    private List<SideFact> sideFacts(JourneyDefinition journey) {
        List<JourneyHopRef> refs = journey.hops();
        List<SideFact> facts = new ArrayList<>();
        for (DeclaredEdge edge : catalog.declaredEdgesFor(journey.id())) {
            if (!consecutive(refs, edge)) {
                facts.add(new SideFact(
                        edge.source(),
                        catalog.application(edge.fromApplicationId()).name(),
                        catalog.application(edge.toApplicationId()).name(),
                        "declared",
                        seenInTrace(journey.id(), edge)));
            }
        }
        return List.copyOf(facts);
    }

    private boolean consecutive(List<JourneyHopRef> refs, DeclaredEdge edge) {
        for (int index = 0; index < refs.size() - 1; index++) {
            if (refs.get(index).applicationId().equals(edge.fromApplicationId())
                    && refs.get(index + 1).applicationId().equals(edge.toApplicationId())) {
                return true;
            }
        }
        return false;
    }

    private boolean seenInTrace(String journeyId, DeclaredEdge edge) {
        return catalog.tracesFor(journeyId).stream().anyMatch(trace -> {
            int from = -1;
            int to = -1;
            List<TraceSpan> spans = trace.spans();
            for (int index = 0; index < spans.size(); index++) {
                if (spans.get(index).applicationId().equals(edge.fromApplicationId())) {
                    from = index;
                }
                if (from >= 0 && spans.get(index).applicationId().equals(edge.toApplicationId())) {
                    to = index;
                }
            }
            return from >= 0 && to > from;
        });
    }

    private void insertSanctions(List<ResolvedHop> hops) {
        for (int index = 0; index < hops.size() - 1; index++) {
            if ("kyc".equals(hops.get(index).applicationId())
                    && "product-setup".equals(hops.get(index + 1).applicationId())) {
                hops.add(index + 1, proposedSanctionsHop());
                return;
            }
        }
    }

    private ResolvedHop proposedSanctionsHop() {
        return new ResolvedHop(
                0,
                "sanctions-screening",
                "Sanctions Screening",
                null,
                null,
                null,
                List.of(new Evidence("proposed", null)),
                false,
                true,
                true);
    }

    private OperationChoice choice(ChainOperation operation) {
        ChainApplication application = catalog.application(operation.applicationId());
        return new OperationChoice(
                application.id(),
                application.name(),
                application.team(),
                operation.id(),
                operation.name());
    }

    private boolean matches(OperationChoice choice, String needle) {
        return choice.applicationName().toLowerCase().contains(needle)
                || choice.operationName().toLowerCase().contains(needle)
                || choice.team().toLowerCase().contains(needle);
    }
}
