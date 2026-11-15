"""Trích văn bản tài liệu cho ingestion (/v1/extract)."""
import io

import pytest
from fastapi.testclient import TestClient

from lmsrag.extract import ExtractError, extract_text


def _docx(paragraphs):
    from docx import Document
    doc = Document()
    for p in paragraphs:
        doc.add_paragraph(p)
    table = doc.add_table(rows=1, cols=2)
    table.rows[0].cells[0].text = "a"
    table.rows[0].cells[1].text = "b"
    buf = io.BytesIO()
    doc.save(buf)
    return buf.getvalue()


def _pdf(text):
    # PDF tối giản một trang, một dòng chữ Latin, đủ để pypdf đọc.
    stream = f"BT /F1 12 Tf 72 720 Td ({text}) Tj ET".encode()
    objs = [b"<< /Type /Catalog /Pages 2 0 R >>", b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
            b"/Resources << /Font << /F1 5 0 R >> >> >>",
            b"<< /Length %d >>\nstream\n" % len(stream) + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"]
    out, offsets = b"%PDF-1.4\n", []
    for i, o in enumerate(objs, 1):
        offsets.append(len(out))
        out += b"%d 0 obj\n" % i + o + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 %d\n0000000000 65535 f \n" % (len(objs) + 1)
    out += b"".join(b"%010d 00000 n \n" % o for o in offsets)
    out += b"trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF" % (len(objs) + 1, xref)
    return out


def test_text_and_markdown_keep_vietnamese():
    assert extract_text("a.md", "# Phân số\r\nMột nửa".encode("utf-8")) == "# Phân số\nMột nửa"
    assert extract_text("b.txt", b"\xef\xbb\xbfxin ch\xc3\xa0o") == "xin chào"


def test_docx_paragraphs_and_tables():
    text = extract_text("d.DOCX", _docx(["Phân số là gì?", "Ví dụ 1/2"]))
    assert "Phân số là gì?" in text and "Ví dụ 1/2" in text and "a | b" in text


def test_pdf_text():
    assert "Hello fractions" in extract_text("p.pdf", _pdf("Hello fractions"))


@pytest.mark.parametrize("name,data", [("x.exe", b"abc"), ("e.txt", b""), ("blank.txt", b"  \n "),
                                       ("bad.pdf", b"not a pdf"), ("bad.docx", b"not a zip")])
def test_rejects_unreadable_or_empty(name, data):
    with pytest.raises(ExtractError):
        extract_text(name, data)


def test_endpoint_returns_text_and_422_for_bad_files():
    from lmsrag.api import Services, create_app
    from lmsrag.embedder import HashEmbedder
    from lmsrag.qdrant_store import QdrantChunkStore, make_client
    svc = Services(embedder=HashEmbedder(), store=QdrantChunkStore(make_client(":memory:")))
    client = TestClient(create_app(svc))
    ok = client.post("/v1/extract", params={"filename": "n.md"}, content="Nội dung bài học".encode())
    assert ok.status_code == 200 and ok.json() == {"text": "Nội dung bài học", "char_count": 16}
    bad = client.post("/v1/extract", params={"filename": "n.exe"}, content=b"x")
    assert bad.status_code == 422 and bad.json()["detail"]["code"] == "EXTRACT_FAILED"
