package com.interactionchains.app.analysis;

import java.util.List;

public record ConfirmResponse(List<ChainView> chains, List<String> teams) {
}
