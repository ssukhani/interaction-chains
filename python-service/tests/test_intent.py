import unittest

from python_service.intent import interpret


class IntentTest(unittest.TestCase):
    def test_sanctions_sentence_is_an_add_without_a_channel(self):
        intent = interpret("Add sanctions screening to opening an account")
        self.assertEqual("add", intent["changeType"])
        self.assertEqual("sanctions-screening", intent["subject"])
        self.assertIsNone(intent["channel"])
        self.assertIsNone(intent["capability"])

    def test_kyc_update_names_the_identity_capability(self):
        intent = interpret("Update the KYC check used in account opening")
        self.assertEqual("update", intent["changeType"])
        self.assertEqual("kyc", intent["subject"])
        self.assertIsNone(intent["channel"])
        self.assertEqual("identity", intent["capability"])

    def test_web_is_captured_when_the_sentence_names_it(self):
        intent = interpret("Add sanctions screening to web account opening")
        self.assertEqual("web", intent["channel"])
        self.assertEqual("sanctions-screening", intent["subject"])

    def test_unrelated_sentence_stays_unknown(self):
        intent = interpret("Replace the wire fee engine")
        self.assertEqual("unknown", intent["changeType"])
        self.assertIsNone(intent["subject"])
        self.assertIsNone(intent["channel"])
        self.assertIsNone(intent["capability"])


if __name__ == "__main__":
    unittest.main()
