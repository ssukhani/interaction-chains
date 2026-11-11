package com.interactionchains.app.analysis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BaAnalysisService {

    private static final List<String> ACCOUNT_TERMS = List.of("account", "onboarding", "opening");
    private static final List<String> CHANNELS = List.of("web", "mobile", "branch");

    private final EstateCatalog catalog;
    private final ChainQueryService chains;
    private final IntentClient intentClient;

    public BaAnalysisService(EstateCatalog catalog, ChainQueryService chains, IntentClient intentClient) {
        this.catalog = catalog;
        this.chains = chains;
        this.intentClient = intentClient;
    }

    public InterpretResponse interpret(InterpretRequest request) {
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            return InterpretResponse.none("Enter a sentence about an account-opening change.");
        }
        Intent intent = merge(intentClient.interpret(text), request.channel(), request.capability());
        Set<String> channels = requestedChannels(request, intent);
        List<JourneyDefinition> hits = match(intent, text, channels);
        if (channels.isEmpty() && distinctChannels(hits).size() > 1) {
            return InterpretResponse.clarify(
                    "channel",
                    "Which channel is this account opening for?",
                    channelChoices(hits));
        }
        if (hits.isEmpty() && blank(intent.capability()) && !request.clarifyAlreadyUsed()) {
            return InterpretResponse.clarify(
                    "capability",
                    "Which capability does this change affect?",
                    capabilityChoices());
        }
        if (hits.isEmpty()) {
            return InterpretResponse.none("No journey matched. No hops were added.");
        }
        List<JourneySummary> summaries = hits.stream()
                .map(journey -> new JourneySummary(journey.id(), journey.name(), journey.channel()))
                .toList();
        return InterpretResponse.shortlist(intent, summaries);
    }

    public ConfirmResponse confirm(ConfirmRequest request) {
        if (request.journeyIds() == null || request.journeyIds().isEmpty()) {
            throw new InvalidAnalysisRequestException("Select at least one journey.");
        }
        List<String> ids = new ArrayList<>(new LinkedHashSet<>(request.journeyIds()));
        List<JourneyDefinition> selected = new ArrayList<>();
        for (String id : ids) {
            selected.add(catalog.findJourney(id)
                    .orElseThrow(() -> new InvalidAnalysisRequestException("Unknown journey " + id + ".")));
        }
        boolean insertSanctions = "add".equals(request.changeType()) && "sanctions-screening".equals(request.subject());
        String highlightApplicationId = "update".equals(request.changeType()) ? request.subject() : null;
        List<ChainView> chainViews = new ArrayList<>();
        for (JourneyDefinition journey : selected) {
            chainViews.add(chains.chain(journey, highlightApplicationId, operationFor(highlightApplicationId), insertSanctions));
        }
        return new ConfirmResponse(List.copyOf(chainViews), teams(chainViews));
    }

    private Intent merge(Intent interpreted, String channel, String capability) {
        String changeType = interpreted.changeType() == null ? "unknown" : interpreted.changeType();
        return new Intent(
                changeType,
                interpreted.subject(),
                firstText(channel, interpreted.channel()),
                firstText(capability, interpreted.capability()));
    }

    private Set<String> requestedChannels(InterpretRequest request, Intent intent) {
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        if (request.channels() != null) {
            for (String channel : request.channels()) {
                if (!blank(channel)) {
                    channels.add(channel.trim());
                }
            }
        }
        if (!blank(request.channel())) {
            channels.add(request.channel().trim());
        }
        if (!blank(intent.channel())) {
            channels.add(intent.channel().trim());
        }
        return channels;
    }

    private List<JourneyDefinition> match(Intent intent, String text, Set<String> channels) {
        if (!related(intent, text)) {
            return List.of();
        }
        List<JourneyDefinition> hits = new ArrayList<>();
        for (JourneyDefinition journey : catalog.journeys()) {
            if (!channels.isEmpty() && !channels.contains(journey.channel())) {
                continue;
            }
            if (!capabilityMatches(intent.capability(), journey)) {
                continue;
            }
            hits.add(journey);
        }
        return List.copyOf(hits);
    }

    private boolean related(Intent intent, String text) {
        if ("sanctions-screening".equals(intent.subject()) || "kyc".equals(intent.subject())) {
            return true;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return ACCOUNT_TERMS.stream().anyMatch(normalized::contains);
    }

    private boolean capabilityMatches(String capability, JourneyDefinition journey) {
        if (blank(capability)) {
            return true;
        }
        return switch (capability) {
            case "identity" -> catalog.journeyContains(journey, "kyc");
            case "product_setup" -> catalog.journeyContains(journey, "product-setup");
            case "customer_record" -> catalog.journeyContains(journey, "cif");
            default -> true;
        };
    }

    private Set<String> distinctChannels(List<JourneyDefinition> hits) {
        Set<String> channels = new LinkedHashSet<>();
        for (JourneyDefinition journey : hits) {
            channels.add(journey.channel());
        }
        return channels;
    }

    private List<Choice> channelChoices(List<JourneyDefinition> hits) {
        Set<String> present = distinctChannels(hits);
        List<Choice> choices = new ArrayList<>();
        for (String channel : CHANNELS) {
            if (present.contains(channel)) {
                choices.add(new Choice(channel, channelLabel(channel)));
            }
        }
        return List.copyOf(choices);
    }

    private List<Choice> capabilityChoices() {
        return List.of(
                new Choice("identity", "Identity check"),
                new Choice("product_setup", "Product setup"),
                new Choice("customer_record", "Customer-record create"));
    }

    private String operationFor(String applicationId) {
        if (applicationId == null) {
            return null;
        }
        return catalog.operations().stream()
                .filter(operation -> operation.applicationId().equals(applicationId))
                .map(ChainOperation::id)
                .findFirst()
                .orElse(null);
    }

    private List<String> teams(List<ChainView> chainViews) {
        LinkedHashSet<String> teams = new LinkedHashSet<>();
        for (ChainView chain : chainViews) {
            for (ResolvedHop hop : chain.hops()) {
                if (hop.team() != null && !hop.team().isBlank()) {
                    teams.add(hop.team());
                }
            }
        }
        return List.copyOf(teams);
    }

    private String channelLabel(String channel) {
        return switch (channel) {
            case "web" -> "Web";
            case "mobile" -> "Mobile";
            case "branch" -> "Branch";
            default -> channel;
        };
    }

    private String firstText(String preferred, String fallback) {
        if (!blank(preferred)) {
            return preferred.trim();
        }
        if (!blank(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
