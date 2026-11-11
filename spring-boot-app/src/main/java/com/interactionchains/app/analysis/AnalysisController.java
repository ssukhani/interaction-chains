package com.interactionchains.app.analysis;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final BaAnalysisService analyst;
    private final ChainQueryService chains;

    public AnalysisController(BaAnalysisService analyst, ChainQueryService chains) {
        this.analyst = analyst;
        this.chains = chains;
    }

    @PostMapping("/ba/interpret")
    public InterpretResponse interpret(@RequestBody InterpretRequest request) {
        return analyst.interpret(request);
    }

    @PostMapping("/ba/confirm")
    public ConfirmResponse confirm(@RequestBody ConfirmRequest request) {
        return analyst.confirm(request);
    }

    @GetMapping("/catalog/operations")
    public List<OperationChoice> operations(@RequestParam(name = "q", defaultValue = "") String query) {
        return chains.typeahead(query);
    }

    @GetMapping("/analysis/impact")
    public ImpactResponse impact(@RequestParam String applicationId, @RequestParam String operationId) {
        return chains.impact(applicationId, operationId);
    }

    @GetMapping("/traces/{traceId}")
    public TraceView trace(@PathVariable String traceId) {
        return chains.trace(traceId);
    }
}
