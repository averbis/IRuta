FROM adoptopenjdk/openjdk11:jdk-11.0.11_9-ubuntu

# Install python 
RUN apt-get update \
  && apt-get install -y python3-pip python3-dev \
  && cd /usr/local/bin \
  && ln -s /usr/bin/python3 python \
  && pip3 install --upgrade pip

# Copy content of IRuta directory into Docker container
COPY . .

# Install python dependencies
RUN pip3 install --no-cache-dir -r jupyter_requirements.txt
RUN python -m sos_notebook.install

# Download the IRuta release
RUN curl -L https://github.com/averbis/IRuta/releases/download/0.2.0/IRuta-0.2.0.zip > iruta.zip

# Unpack and install the kernel
RUN apt-get install -y unzip
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
CMD ["jupyter", "notebook", "--ip", "0.0.0.0", "--no-browser","--NotebookApp.token=''","--NotebookApp.password=''"]
