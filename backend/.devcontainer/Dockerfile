FROM python:3.11.5-slim

RUN apt-get update -y \
    && apt-get install -y --no-install-recommends \
    git \
    openssh-client \
    curl

RUN pip install pip-tools

RUN useradd --create-home --shell /bin/bash vscode


RUN mkdir -p /tmp/codeium/manager \
    && chown -R vscode:vscode /tmp/codeium \
    && chmod -R 777 /tmp/codeium

USER vscode

COPY requirements/main.txt requirements/main.txt
RUN pip-sync --user requirements/main.txt