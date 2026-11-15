"""Embedding back-ends. All return L2-normalised float32 matrices (so inner product = cosine).

* ``bge``   : BAAI/bge-small-en-v1.5 via sentence-transformers (what the original repo used).
              Needs the HuggingFace hub, i.e. run it on your own machine / Colab.
* ``e5``    : intfloat/multilingual-e5-base (768-d, multilingual incl. Vietnamese). Production choice (D3).
              Uses the ``query: `` / ``passage: `` prefixes the model was trained with.
* ``hash``  : feature-hashing bag of words/bigrams. Offline, deterministic, no model download.
              For unit tests, CI and the fake-LLM demo only; it is not a semantic model.
* ``lsa``   : TF-IDF (1-2 grams) + truncated SVD. Fully offline, deterministic, no downloads.
              Used for the numbers produced inside the sandbox where HF is blocked.
"""
from __future__ import annotations

import hashlib
import os
import pickle
import re
from pathlib import Path

import numpy as np


def _l2(x: np.ndarray) -> np.ndarray:
    x = np.asarray(x, dtype="float32")
    return x / np.maximum(np.linalg.norm(x, axis=1, keepdims=True), 1e-12)


class LsaEmbedder:
    name = "lsa"

    def __init__(self, dim: int = 256, seed: int = 0):
        self.dim, self.seed = dim, seed
        self.tfidf = self.svd = None

    def fit(self, texts: list[str]) -> "LsaEmbedder":
        from sklearn.decomposition import TruncatedSVD
        from sklearn.feature_extraction.text import TfidfVectorizer
        self.tfidf = TfidfVectorizer(token_pattern=r"(?u)\\?[A-Za-z]{2,}|\d+", ngram_range=(1, 2), min_df=3,
                                     max_features=80000, sublinear_tf=True, lowercase=True)
        x = self.tfidf.fit_transform(texts)
        self.svd = TruncatedSVD(n_components=min(self.dim, x.shape[1] - 1), random_state=self.seed).fit(x)
        return self

    def encode(self, texts: list[str]) -> np.ndarray:
        return _l2(self.svd.transform(self.tfidf.transform(texts)))

    encode_queries = encode

    def save(self, path: Path):
        path.write_bytes(pickle.dumps((self.tfidf, self.svd, self.dim)))

    @classmethod
    def load(cls, path: Path) -> "LsaEmbedder":
        o = cls(); o.tfidf, o.svd, o.dim = pickle.loads(path.read_bytes()); return o


class BgeEmbedder:
    name = "bge"

    def __init__(self, model: str = "BAAI/bge-small-en-v1.5"):
        from sentence_transformers import SentenceTransformer
        self.model = SentenceTransformer(model)

    def fit(self, texts):  # stateless
        return self

    QUERY_PREFIX = "Represent this sentence for searching relevant passages: "      # bge retrieval convention

    def encode(self, texts: list[str]) -> np.ndarray:
        return _l2(self.model.encode(texts, batch_size=64, normalize_embeddings=True, show_progress_bar=False))

    def encode_queries(self, texts: list[str]) -> np.ndarray:
        return self.encode([self.QUERY_PREFIX + t for t in texts])


class E5Embedder:
    name = "e5"
    dim = 768
    QUERY_PREFIX = "query: "
    PASSAGE_PREFIX = "passage: "

    def __init__(self, model: str | None = None):
        from sentence_transformers import SentenceTransformer
        self.model_name = model or os.environ.get("EMBED_MODEL", "intfloat/multilingual-e5-base")
        self.model = SentenceTransformer(self.model_name)

    def fit(self, texts):  # stateless
        return self

    def _encode(self, texts: list[str]) -> np.ndarray:
        return _l2(self.model.encode(texts, batch_size=32, normalize_embeddings=True, show_progress_bar=False))

    def encode(self, texts: list[str]) -> np.ndarray:
        """Documents/passages (what gets indexed)."""
        return self._encode([self.PASSAGE_PREFIX + t for t in texts])

    encode_passages = encode

    def encode_queries(self, texts: list[str]) -> np.ndarray:
        return self._encode([self.QUERY_PREFIX + t for t in texts])


class HashEmbedder:
    """Deterministic feature-hashing embedder (no downloads). Uses sha1, never ``hash()``,
    so vectors are identical across processes and machines."""

    name = "hash"
    _TOKEN = re.compile(r"\w+", re.UNICODE)

    def __init__(self, dim: int = 256):
        self.dim = dim

    def fit(self, texts):  # stateless
        return self

    def _features(self, text: str) -> list[str]:
        words = self._TOKEN.findall(text.casefold())
        return words + [f"{a} {b}" for a, b in zip(words, words[1:])]

    def encode(self, texts: list[str]) -> np.ndarray:
        out = np.zeros((len(texts), self.dim), dtype="float32")
        for row, text in enumerate(texts):
            for feature in self._features(text):
                digest = hashlib.sha1(feature.encode("utf-8")).digest()
                bucket = int.from_bytes(digest[:4], "big") % self.dim
                sign = 1.0 if digest[4] & 1 else -1.0
                out[row, bucket] += sign
        return _l2(out)

    encode_passages = encode
    encode_queries = encode


def make_embedder(kind: str | None = None):
    kind = (kind or os.environ.get("EMBEDDER", "lsa")).lower()
    if kind == "e5":
        return E5Embedder()
    if kind == "hash":
        return HashEmbedder()
    if kind == "bge":
        return BgeEmbedder()
    if kind == "lsa":
        return LsaEmbedder()
    raise ValueError(f"Unsupported EMBEDDER: {kind!r}")
