FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1
WORKDIR /app

# WITH_E5=true installs sentence-transformers (torch) for EMBEDDER=e5; default image stays small.
ARG WITH_E5=false
COPY requirements.txt requirements-e5.txt ./
RUN if [ "$WITH_E5" = "true" ]; then pip install -r requirements-e5.txt; else pip install -r requirements.txt; fi

COPY lmsrag ./lmsrag

RUN useradd --system --uid 10001 --create-home app && chown -R app /app
USER app

EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=20s \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8000/health').status==200 else 1)"
CMD ["uvicorn", "lmsrag.api:app", "--host", "0.0.0.0", "--port", "8000"]
