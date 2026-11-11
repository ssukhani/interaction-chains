import io
import unittest

from python_service.server import read_body


class ReadBodyTest(unittest.TestCase):
    def test_reads_a_content_length_body(self):
        raw = b'{"text":"Add sanctions screening"}'
        body = read_body({"Content-Length": str(len(raw))}, io.BytesIO(raw))
        self.assertEqual(raw, body)

    def test_reads_a_chunked_body(self):
        raw = b'{"text":"Add sanctions screening"}'
        chunked = f"{len(raw):x}\r\n".encode() + raw + b"\r\n0\r\n\r\n"
        body = read_body({"Transfer-Encoding": "chunked"}, io.BytesIO(chunked))
        self.assertEqual(raw, body)


if __name__ == "__main__":
    unittest.main()
