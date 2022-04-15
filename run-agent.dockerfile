FROM ubuntu:focal
#
ARG ANSIBLE_VERSION=2.9.13
ARG LEVANT_VERSION=0.3.0
#
RUN apt-get update \
        && apt-get install -y --no-install-recommends \
        python3-pip \
        openssh-client \
        sshpass \
        git \
        wget \
        unzip \
        && rm -rf /var/lib/apt/lists/*
# Ansible
RUN pip3 install \
        setuptools \
        wheel \
        lxml
RUN pip3 install ansible==${ANSIBLE_VERSION}
# Changelog
RUN pip3 install \
        GitPython \
        atlassian-python-api \
        jinja2 \
        natsort \
        python-jenkins \
        pexpect \
        git-archive-all \
        python-nomad
##
ENTRYPOINT []
CMD tail -f /dev/null