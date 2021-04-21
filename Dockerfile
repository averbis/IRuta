FROM openjdk:11.0.3-jdk
COPY --from=python:3.8 / /

RUN apt-get update

# Copy content of IRuta directory into Docker container
COPY . .

# Install python dependencies
RUN apt-get install -y python3-pip
RUN pip3 install --no-cache-dir -r jupyter_requirements.txt
RUN python -m sos_notebook.install

# Install node.js which is required for building jupyterlab-sos addons
ENV NODE_VERSION=12.6.0
RUN apt install -y curl
RUN curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.34.0/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN node --version
RUN npm --version

# Install jupyterlab extensions for sos
# RUN jupyter labextension install transient-display-data # <-- optional
RUN jupyter labextension install jupyterlab-sos

# Download the IRuta release
RUN curl -L https://github.com/averbis/IRuta/releases/download/0.1.0/IRuta-0.1.0.zip > iruta.zip

# Unpack and install the kernel
RUN unzip iruta.zip -d iruta \
  && cd iruta \
  && python3 install.py --sys-prefix

# Set up the user environment
USER root
ENV NB_USER booker
ENV NB_UID 1000
ENV HOME /home/$NB_USER

RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid $NB_UID \
    $NB_USER

COPY notebooks/ $HOME/
RUN chown -R $NB_UID $HOME

USER $NB_USER

# Launch the notebook server
WORKDIR $HOME
CMD ["jupyter", "notebook", "--ip", "0.0.0.0"]
