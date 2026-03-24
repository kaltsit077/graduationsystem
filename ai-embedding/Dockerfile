ARG DOCKERHUB_PROXY=docker.m.daocloud.io
FROM ${DOCKERHUB_PROXY}/library/python:3.10-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY requirements.txt ./
ARG PIP_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple
RUN pip config set global.index-url ${PIP_INDEX_URL} \
    && pip install --no-cache-dir -r requirements.txt

COPY main.py ./main.py

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

