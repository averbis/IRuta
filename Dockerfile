FROM openjdk:11.0.3-jdk
COPY --from=python:3.8 / /

RUN apt-get update
RUN apt-get install -y python3-pip

# add requirements.txt, written this way to gracefully ignore a missing file
# sos-notebooks is built on top of juypter notebook and brings all necessary dependencies
COPY . .
RUN ([ -f requirements.txt ] \
    && pip3 install --no-cache-dir -r requirements.txt) \
        || pip3 install --no-cache-dir sos-notebook sos dkpro-cassis
RUN python -m sos_notebook.install

USER root

# Download the kernel release
ADD build/distributions/iruta-0.1.0.zip iruta.zip

# Unpack and install the kernel
RUN unzip iruta.zip -d iruta \
  && cd iruta \
  && python3 install.py --sys-prefix

# Set up the user environment
ENV NB_USER booker
ENV NB_UID 1000
ENV HOME /home/$NB_USER

RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid $NB_UID \
    $NB_USER

COPY notebooks $HOME
COPY notebooks/input $HOME/input
COPY notebooks/typesystems $HOME/typesystems
COPY notebooks/wordlists $HOME/wordlists
RUN chown -R $NB_UID $HOME

USER $NB_USER

# Launch the notebook server
WORKDIR $HOME
CMD ["jupyter", "notebook", "--ip", "0.0.0.0"]
