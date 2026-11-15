"""Qdrant-backed chunk store for the LMS AI service (D2).

Authorization is NOT decided here (BR-AI-01): Spring Boot computes which courses/documents the
caller may read and passes them in. This store only enforces that a search is always restricted to
that list, and refuses to search at all when the list is empty.
"""
from __future__ import annotations

import os
import uuid
import warnings
from dataclasses import dataclass
from typing import Any, Sequence

import numpy as np
from qdrant_client import QdrantClient, models

_NAMESPACE = uuid.UUID("6f1b6a4e-3c0e-4a57-9d2e-5d6b0f4f9c11")


@dataclass(frozen=True)
class ChunkRecord:
    document_id: int
    course_id: int
    chunk_index: int
    text: str
    lesson_id: int | None = None
    skill_code: str | None = None


def point_id(document_id: int, chunk_index: int) -> str:
    return str(uuid.uuid5(_NAMESPACE, f"{document_id}:{chunk_index}"))


def make_client(url: str | None = None) -> QdrantClient:
    """``":memory:"`` gives an in-process client for tests."""
    url = url or os.environ.get("QDRANT_URL", "http://localhost:6333")
    return QdrantClient(location=":memory:") if url == ":memory:" else QdrantClient(url=url)


class QdrantChunkStore:
    def __init__(self, client: QdrantClient | None = None, collection: str | None = None):
        self.client = client or make_client()
        self.collection = collection or os.environ.get("QDRANT_COLLECTION", "ai_document_chunks")

    def ensure_collection(self, dim: int) -> None:
        if self.client.collection_exists(self.collection):
            existing = self.client.get_collection(self.collection).config.params.vectors
            if existing.size != dim:
                raise ValueError(
                    f"collection {self.collection!r} has dim {existing.size}, embedder produces {dim}; "
                    "reindex into a new collection"
                )
            return
        self.client.create_collection(
            self.collection,
            vectors_config=models.VectorParams(size=dim, distance=models.Distance.COSINE),
        )
        with warnings.catch_warnings():            # local (:memory:) mode warns that indexes are no-ops
            warnings.simplefilter("ignore", UserWarning)
            for field in ("course_id", "document_id"):
                self.client.create_payload_index(self.collection, field, models.PayloadSchemaType.INTEGER)

    def upsert_chunks(self, chunks: Sequence[ChunkRecord], vectors: np.ndarray) -> int:
        if len(chunks) != len(vectors):
            raise ValueError("chunks and vectors must have the same length")
        if not chunks:
            return 0
        self.ensure_collection(int(vectors.shape[1]))
        points = [
            models.PointStruct(
                id=point_id(c.document_id, c.chunk_index),
                vector=np.asarray(v, dtype="float32").tolist(),
                payload={
                    "course_id": c.course_id, "document_id": c.document_id,
                    "lesson_id": c.lesson_id, "chunk_index": c.chunk_index,
                    "skill_code": c.skill_code, "text": c.text,
                },
            )
            for c, v in zip(chunks, vectors)
        ]
        self.client.upsert(self.collection, points=points, wait=True)
        return len(points)

    def search(self, vector: np.ndarray, allowed_course_ids: Sequence[int],
               allowed_document_ids: Sequence[int] | None = None,
               top_k: int = 5, skill_code: str | None = None) -> list[dict[str, Any]]:
        """Top-k chunks restricted to the allowed courses (and documents, when given).

        An empty/None allow-list returns [] without querying: there is no "search everything" mode.
        """
        if not allowed_course_ids:
            return []
        if allowed_document_ids is not None and len(allowed_document_ids) == 0:
            return []
        if not self.client.collection_exists(self.collection):
            return []
        must: list[models.Condition] = [
            models.FieldCondition(key="course_id", match=models.MatchAny(any=list(allowed_course_ids)))
        ]
        if allowed_document_ids is not None:
            must.append(models.FieldCondition(
                key="document_id", match=models.MatchAny(any=list(allowed_document_ids))))
        if skill_code:
            must.append(models.FieldCondition(key="skill_code", match=models.MatchValue(value=skill_code)))
        response = self.client.query_points(
            self.collection, query=np.asarray(vector, dtype="float32").reshape(-1).tolist(),
            query_filter=models.Filter(must=must), limit=top_k, with_payload=True,
        )
        return [
            {"score": float(p.score), "doc_id": f"{p.payload['document_id']}:{p.payload['chunk_index']}", **p.payload}
            for p in response.points
        ]

    def delete_document(self, document_id: int) -> None:
        if not self.client.collection_exists(self.collection):
            return
        self.client.delete(
            self.collection,
            points_selector=models.FilterSelector(filter=models.Filter(must=[
                models.FieldCondition(key="document_id", match=models.MatchValue(value=document_id))
            ])),
            wait=True,
        )
