# IRuta
A Jupyter kernel for Apache [UIMA Ruta](https://uima.apache.org/ruta.html), a powerful rule-based engine for annotating documents.

## Try IRuta in a Binder
No installation required. You can immediately start experimenting with our example Ruta notebooks or write your own notebooks by clicking on the following link.
Please be patient, as this launches a Docker container which may take a short time depending on whether the Docker container is already in a cache. Please note that your notebooks in Binder are not persistent.

[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/averbis/IRuta/HEAD)

**Known issue:** Right now, the pure IRuta kernel is working without known issues. The integration from Python with IRuta using [SoS-notebooks](https://vatlab.github.io/sos-docs/) is not yet stable. Sometimes the kernel is not starting properly due to a race condition. We are working on it.

## Installation
### Option 1: Installing Ruta Kernel from our prepackaged releases
#### Preparation

1. Install a Java JDK 8 or newer. You may download a JDK from [AdoptOpenJDK](https://adoptopenjdk.net/).

2. Install Python3 with its package manager pip.

3. Once Python is installed, you will need to install the jupyter requirements by running the following command.

    ```bash
    # Installing jupyter notebook requirements
    pip install -r jupyter_requirements.txt
    ```

    It is recommended to install the packages into a [virtual enviroment](https://docs.python.org/3/tutorial/venv.html). You may also use Anaconda for the purpose of installing Python and managing its packages.

4. In case you are upgrading the IRuta version, you need to remove an old IRuta installation by running `jupyter kernelspec remove ruta`. 

#### Actual installation

1. Download the prepackaged distribution *IRuta-{release-version}.zip* from the latest Release in the [Releases](https://github.com/averbis/IRuta/releases) tab. 

2. Unzip it into a temporary location and change into the unzipped directory.

3. Run the installer with the following command. The installer is a python script and has the same options as `jupyter kernelspec install`.

    ```bash
    # A common install command is
    python3 install.py --user
    ```

4. Check that it installed with `jupyter kernelspec list` which should contain `ruta`.

### Option 2: Running IRuta in a Docker container.
The repository contains a Dockerfile. You can simply execute it by running the following two commands:
* Build the docker image: `docker build -t iruta .`
* Run the docker image: `docker run -p 8888:8888 iruta`

---
## Building IRuta Kernel on OS X / Linux from source
If you want to develop the kernel, then the following steps help you compiling the kernel from source using Gradle.

* Install SDKMAN: `curl -s "https://get.sdkman.io" | bash`
* You may need to activate the `sdk` with `source .bashrc` or `source .zshrc` or set the path to it.
* Install Gradle 4.8.1: `sdk install gradle 4.8.1`
* Use Gradle 4.8.1: `sdk use gradle 4.8.1`
* Optional, but strongly recommended: Create a virtual environment
* Install IRuta: `gradle clean installKernel -PinstallKernel.python=venv/bin/python --user` 
