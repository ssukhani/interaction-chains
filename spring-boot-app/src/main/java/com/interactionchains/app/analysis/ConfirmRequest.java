package com.interactionchains.app.analysis;

import java.util.List;

public record ConfirmRequest(List<String> journeyIds, String changeType, String subject) {
}
