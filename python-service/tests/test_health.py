import unittest

from python_service.health import status


class HealthTest(unittest.TestCase):
    def test_status(self):
        payload = status()
        self.assertEqual("ok", payload["status"])
        self.assertEqual("python-service", payload["service"])


if __name__ == "__main__":
    unittest.main()
