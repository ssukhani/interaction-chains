package com.interactionchains.app.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BaAnalysisServiceTest {

    private static final List<String> CONSULTING_TEAMS = List.of(
            "Digital Channels",
            "Customer Origination",
            "Financial Crime",
            "Deposits",
            "Core Banking");

    private static EstateCatalog catalog;
    private static ChainQueryService chains;

    @BeforeAll
    static void loadEstate() {
        catalog = new EstateCatalog();
        chains = new ChainQueryService(catalog);
    }

    @Test
    void sanctionsSentenceAsksWhichChannel() {
        InterpretResponse response = analyst(new Intent("add", "sanctions-screening", null, null))
                .interpret(request("Add sanctions screening to opening an account", null, null, false));
        assertEquals("clarify", response.type());
        assertEquals("channel", response.field());
        assertEquals(List.of("web", "mobile", "branch"), response.options().stream().map(Choice::id).toList());
    }

    @Test
    void confirmingWebInsertsSanctionsBetweenKycAndProductSetup() {
        BaAnalysisService analyst = analyst(new Intent("add", "sanctions-screening", null, null));
        InterpretResponse shortlist = analyst.interpret(
                request("Add sanctions screening to opening an account", List.of("web"), null, true));
        assertEquals("shortlist", shortlist.type());
        assertEquals(List.of("web-account-open"), shortlist.journeys().stream().map(JourneySummary::id).toList());

        ConfirmResponse confirmed = analyst.confirm(
                new ConfirmRequest(List.of("web-account-open"), "add", "sanctions-screening"));
        ChainView chain = confirmed.chains().getFirst();
        assertEquals(
                List.of("web-ui", "orchestrator", "kyc", "sanctions-screening", "product-setup", "cif"),
                chain.hops().stream().map(ResolvedHop::applicationId).toList());
        ResolvedHop proposed = chain.hops().get(3);
        assertTrue(proposed.proposed());
        assertEquals(List.of("proposed"), proposed.evidence().stream().map(Evidence::label).toList());
        assertNull(proposed.team());
        assertEquals(CONSULTING_TEAMS, confirmed.teams());
    }

    @Test
    void confirmingWebAndMobileReturnsEachChainAndOneTeamList() {
        BaAnalysisService analyst = analyst(new Intent("add", "sanctions-screening", null, null));
        InterpretResponse shortlist = analyst.interpret(
                request("Add sanctions screening to opening an account", List.of("web", "mobile"), null, true));
        assertEquals(List.of("web-account-open", "mobile-account-open"),
                shortlist.journeys().stream().map(JourneySummary::id).toList());

        ConfirmResponse confirmed = analyst.confirm(new ConfirmRequest(
                shortlist.journeys().stream().map(JourneySummary::id).toList(),
                "add",
                "sanctions-screening"));
        assertEquals(2, confirmed.chains().size());
        assertEquals(CONSULTING_TEAMS, confirmed.teams());
    }

    @Test
    void updateHighlightsKycWithoutInsertingAHop() {
        BaAnalysisService analyst = analyst(new Intent("update", "kyc", null, "identity"));
        InterpretResponse clarify = analyst.interpret(
                request("Update the KYC check used in account opening", null, null, false));
        assertEquals("clarify", clarify.type());
        assertEquals(List.of("web", "mobile"), clarify.options().stream().map(Choice::id).toList());

        ConfirmResponse confirmed = analyst.confirm(
                new ConfirmRequest(List.of("web-account-open"), "update", "kyc"));
        ChainView chain = confirmed.chains().getFirst();
        assertEquals(5, chain.hops().size());
        ResolvedHop kyc = chain.hops().get(2);
        assertTrue(kyc.highlighted());
        assertEquals("kyc", kyc.applicationId());
        assertFalse(chain.hops().stream().anyMatch(ResolvedHop::proposed));
    }

    @Test
    void otherAddDoesNotInventAnInsertionPoint() {
        ConfirmResponse confirmed = analyst(new Intent("add", "other", "web", null))
                .confirm(new ConfirmRequest(List.of("web-account-open"), "add", "other"));
        assertEquals(5, confirmed.chains().getFirst().hops().size());
        assertFalse(confirmed.chains().getFirst().hops().stream().anyMatch(ResolvedHop::proposed));
    }

    @Test
    void secondClarifyOnAnUnknownSentenceStopsWithoutHops() {
        BaAnalysisService analyst = analyst(new Intent("unknown", null, null, null));
        InterpretResponse first = analyst.interpret(request("Replace the wire fee engine", null, null, false));
        assertEquals("clarify", first.type());
        assertEquals("capability", first.field());

        InterpretResponse second = analyst.interpret(
                new InterpretRequest("Replace the wire fee engine", null, null, "product_setup", true));
        assertEquals("none", second.type());
        assertEquals("No journey matched. No hops were added.", second.message());
    }

    @Test
    void blankSentenceDoesNotCallTheInterpreter() {
        BaAnalysisService analyst = new BaAnalysisService(catalog, chains, text -> {
            throw new AssertionError("interpreter should not be called");
        });
        InterpretResponse response = analyst.interpret(request("  ", null, null, false));
        assertEquals("none", response.type());
    }

    @Test
    void unknownJourneyIsRejected() {
        BaAnalysisService analyst = analyst(new Intent("add", "other", null, null));
        assertThrows(InvalidAnalysisRequestException.class,
                () -> analyst.confirm(new ConfirmRequest(List.of("missing"), "add", "other")));
    }

    private static BaAnalysisService analyst(Intent intent) {
        return new BaAnalysisService(catalog, chains, text -> intent);
    }

    private static InterpretRequest request(String text, List<String> channels, String capability, boolean clarifyUsed) {
        return new InterpretRequest(text, null, channels, capability, clarifyUsed);
    }
}
