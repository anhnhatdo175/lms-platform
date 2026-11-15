"""Trích văn bản từ tệp tài liệu (txt, md, pdf, docx) cho ingestion. Không giữ trạng thái, không ghi đĩa."""
from __future__ import annotations

import io

MAX_BYTES = 20 * 1024 * 1024
MAX_PDF_PAGES = 500
SUPPORTED = (".txt", ".md", ".pdf", ".docx")


class ExtractError(ValueError):
    """Tệp không đọc được hoặc không có văn bản (ví dụ PDF scan, cần OCR)."""


def extract_text(filename: str, content: bytes) -> str:
    if not content:
        raise ExtractError("empty file")
    if len(content) > MAX_BYTES:
        raise ExtractError("file too large")
    name = (filename or "").lower()
    if name.endswith((".txt", ".md")):
        text = _decode(content)
    elif name.endswith(".pdf"):
        text = _pdf(content)
    elif name.endswith(".docx"):
        text = _docx(content)
    else:
        raise ExtractError("unsupported file type; supported: " + ", ".join(SUPPORTED))
    text = "\n".join(line.rstrip() for line in text.replace("\x00", "").splitlines()).strip()
    if not text:
        raise ExtractError("no extractable text (scanned documents need OCR)")
    return text


def _decode(content: bytes) -> str:
    for encoding in ("utf-8-sig", "utf-16"):
        try:
            return content.decode(encoding)
        except UnicodeError:
            continue
    return content.decode("latin-1")


def _pdf(content: bytes) -> str:
    try:
        from pypdf import PdfReader
        reader = PdfReader(io.BytesIO(content))
        if reader.is_encrypted:
            raise ExtractError("encrypted PDF")
        return "\n\n".join((page.extract_text() or "") for page in reader.pages[:MAX_PDF_PAGES])
    except ExtractError:
        raise
    except Exception as exc:  # pypdf raises many types on malformed input
        raise ExtractError("cannot read PDF") from exc


def _docx(content: bytes) -> str:
    try:
        from docx import Document
        doc = Document(io.BytesIO(content))
        parts = [p.text for p in doc.paragraphs]
        for table in doc.tables:
            for row in table.rows:
                parts.append(" | ".join(cell.text.strip() for cell in row.cells))
        return "\n".join(parts)
    except Exception as exc:
        raise ExtractError("cannot read DOCX") from exc
