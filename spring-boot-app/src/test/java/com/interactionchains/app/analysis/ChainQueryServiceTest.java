package com.interactionchains.app.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChainQueryServiceTest {

    private static ChainQueryService chains;

    @BeforeAll
    static void loadEstate() {
        chains = new ChainQueryService(new EstateCatalog());
    }

    @Test
    void webHopsAreObservedFromTheTrace() {
        ChainView chain = web();
        assertEquals(List.of("web-ui", "orchestrator", "kyc", "product-setup", "cif"), applicationIds(chain));
        assertTrue(chain.hops().stream().allMatch(ResolvedHop::seenInTrace));
        assertEquals(List.of("observed"), labels(chain.hops().getFirst()));
        assertEquals("T-WEB-100", chain.hops().getFirst().evidence().getFirst().source());
        assertTrue(chain.sideFacts().isEmpty());
    }

    @Test
    void mobileEntryHopIsInferred() {
        ChainView chain = chains.impact("mobile-ui", "mobile-post-applications").journeys().getFirst();
        assertEquals(List.of("inferred"), labels(chain.hops().getFirst()));
        assertFalse(chain.hops().getFirst().seenInTrace());
        assertEquals(List.of("observed"), labels(chain.hops().get(1)));
        assertEquals("T-MOB-100", chain.hops().get(1).evidence().getFirst().source());
    }

    @Test
    void branchDeclaredHopStaysSeparateFromTheAdr022SideFact() {
        ChainView chain = chains.impact("cif", "cif-opncif").journeys().stream()
                .filter(view -> view.journeyId().equals("branch-account-open"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("branch-ui", "orchestrator", "product-setup", "cif"), applicationIds(chain));
        assertEquals(List.of("declared"), labels(chain.hops().get(0)));
        assertEquals("ADR-014", chain.hops().get(0).evidence().getFirst().source());
        assertFalse(chain.hops().get(0).seenInTrace());
        assertEquals(List.of("declared"), labels(chain.hops().get(1)));
        assertEquals(List.of("inferred"), labels(chain.hops().get(2)));
        assertEquals(List.of("inferred"), labels(chain.hops().get(3)));
        assertEquals(1, chain.sideFacts().size());
        SideFact sideFact = chain.sideFacts().getFirst();
        assertEquals("ADR-022", sideFact.source());
        assertEquals("Onboarding Orchestrator", sideFact.fromApplicationName());
        assertEquals("KYC Service", sideFact.toApplicationName());
        assertFalse(sideFact.seenInTrace());
    }

    @Test
    void cifOperationImpactsAllThreeJourneys() {
        ImpactResponse impact = chains.impact("cif", "cif-opncif");
        assertEquals(List.of("web-account-open", "mobile-account-open", "branch-account-open"),
                impact.journeys().stream().map(ChainView::journeyId).toList());
        for (ChainView journey : impact.journeys()) {
            ResolvedHop changed = journey.hops().getLast();
            assertTrue(changed.highlighted());
            assertEquals("cif", changed.applicationId());
            assertEquals("CICS OPNCIF", changed.operationName());
        }
    }

    @Test
    void traceDrillDownReturnsOnlySpansOnThatTrace() {
        TraceView trace = chains.trace("T-WEB-100");
        assertEquals("web-account-open", trace.journeyId());
        assertEquals(5, trace.hops().size());
        assertEquals("Web Onboarding UI", trace.hops().getFirst().applicationName());
        assertEquals("POST /applications", trace.hops().getFirst().operationName());
        assertEquals("2026-04-02T14:01:00Z", trace.hops().getFirst().time());
        assertEquals("success", trace.hops().getFirst().status());
        assertEquals("CIF Open", trace.hops().getLast().applicationName());
        assertEquals("CICS OPNCIF", trace.hops().getLast().operationName());
    }

    @Test
    void typeaheadFindsTheMainframeOperation() {
        List<OperationChoice> choices = chains.typeahead("opncif");
        assertEquals(1, choices.size());
        assertEquals("cif", choices.getFirst().applicationId());
        assertEquals("cif-opncif", choices.getFirst().operationId());
        assertEquals("Core Banking", choices.getFirst().team());
    }

    private ChainView web() {
        return chains.impact("web-ui", "web-post-applications").journeys().getFirst();
    }

    private static List<String> applicationIds(ChainView chain) {
        return chain.hops().stream().map(ResolvedHop::applicationId).toList();
    }

    private static List<String> labels(ResolvedHop hop) {
        return hop.evidence().stream().map(Evidence::label).toList();
    }
}
