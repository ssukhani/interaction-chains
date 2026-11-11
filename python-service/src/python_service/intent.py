"""Turn a free-text change sentence into a structured intent."""

import re

_CHANNELS = (
    ("web", re.compile(r"\bweb\b")),
    ("mobile", re.compile(r"\bmobile\b")),
    ("branch", re.compile(r"\bbranch\b")),
)


def interpret(text):
    normalized = " ".join((text or "").lower().split())
    change_type = "unknown"
    subject = None
    if "sanction" in normalized:
        change_type = "add"
        subject = "sanctions-screening"
    elif "update" in normalized and "kyc" in normalized:
        change_type = "update"
        subject = "kyc"
    elif re.search(r"\badd\b", normalized):
        change_type = "add"
        subject = "other"

    return {
        "changeType": change_type,
        "subject": subject,
        "channel": _channel(normalized),
        "capability": _capability(normalized),
    }


def _channel(normalized):
    for name, pattern in _CHANNELS:
        if pattern.search(normalized):
            return name
    return None


def _capability(normalized):
    if "identity" in normalized or re.search(r"\bkyc\b", normalized):
        return "identity"
    if "product setup" in normalized or re.search(r"\bproduct\b", normalized):
        return "product_setup"
    if "customer record" in normalized or "customer-record" in normalized or re.search(r"\bcif\b", normalized):
        return "customer_record"
    return None
