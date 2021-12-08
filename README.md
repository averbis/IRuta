# IRuta
A Jupyter kernel for Apache [UIMA Ruta](https://uima.apache.org/ruta.html), a powerful rule-based engine for annotating documents.

## Try IRuta in a Binder
No installation required. You can immediately start experimenting with our example Ruta notebooks or write your own notebooks by clicking on the following link.
Please be patient, as this launches a Docker container which may take a short time depending on whether the Docker container is already in a cache. Please note that your notebooks in Binder are not persistent.

[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/averbis/IRuta/HEAD)

**Known issue:** 
- We recommend using IRuta in **Jupyter-Notebook** and **not** in JupyterLab as there are known issues that the syntax highlighting [(bug #6)](https://github.com/averbis/IRuta/issues/6) and AnnotationViewer [(bug #7)](https://github.com/averbis/IRuta/issues/7) are not working in JupyterLab.
- The integration from Python with IRuta using [SoS-notebooks](https://vatlab.github.io/sos-docs/) is not yet stable due to a race condition when starting the sos-kernel [(bug #9)](https://github.com/averbis/IRuta/issues/9) and problems when restarting the kernel [(bug #8)](https://github.com/averbis/IRuta/issues/8)

## Installation
### Option 1: Installing Ruta Kernel from our prepackaged releases
#### Preparation

1. Install a Java JDK 8 or newer (up to JDK 15). You may download a JDK from [AdoptOpenJDK](https://adoptopenjdk.net/).

2. Install Python3 with its package manager pip.

3. Once Python is installed, you will need to install the jupyter requirements. Download the [jupyter_requirements.txt](https://github.com/averbis/IRuta/blob/main/jupyter_requirements.txt) from this repository first, navigate to the download directory and open a terminal. It is recommended to install everything into a [virtual environment](https://docs.python.org/3/library/venv.html) by running the following commands:

    ```sh
    # 1. Navigate to the IRuta directory
    # 2. Installing jupyter notebook requirements into a virtual python environment
    python -m venv venv
    source venv/bin/activate # or venv/Scripts/activate.bat for Windows
    venv/bin/pip  install --upgrade pip wheel setuptools # Upgrading library managers in venv
    pip install -r jupyter_requirements.txt
    ```

4. In case you are upgrading the IRuta version, you need to remove an old IRuta installation by running `jupyter kernelspec remove ruta`. 

#### Actual installation of IRuta kernel

1. Download the prepackaged distribution *IRuta-{release-version}.zip* from the latest Release in the [Releases](https://github.com/averbis/IRuta/releases) tab. 

2. Unzip it into a temporary location and change into the unzipped directory.

3. Run the installer with the following command. The installer is a python script and has the same options as `jupyter kernelspec install`.

    ```sh
    python install.py --user
    ```

#### Install sos kernel
The SoS Notebook kernel is a Jupyter kernel that allows the use of multiple kernels in one Jupyter notebook. The kernel itself is in Python. We use it to support Python and Ruta in the same notebook using [sos-ruta](https://github.com/averbis/sos-ruta). After you have installed SoS as part of the `jupyter_requirements.txt` in the step above, you can run the following command to install the SoS kernel.

 ```sh
  python -m sos_notebook.install --user
 ```

Check that all kernels are correctly installed with `jupyter kernelspec list` which should yield something like the following:
```
(venv) ➜  IRuta: jupyter kernelspec list
Available kernels:
  python3    /home/{username}/.local/share/jupyter/kernels/python3
  ruta       /home/{username}/.local/share/jupyter/kernels/ruta
  sos        /home/{username}/.local/share/jupyter/kernels/sos
```

#### Run a Notebook
Start a notebook by executing `jupyter-notebook` inside the virtual environment. Have a look at the example notebooks and tutorials in [notebooks](notebooks/).

### Option 2: Running IRuta in a Docker container.
The repository contains a Dockerfile. You can simply execute it by running the following two commands:
* Build the docker image: `docker build -t iruta .`
* Run the docker image: `docker run -p 8888:8888 iruta`

---
## Building IRuta Kernel on OS X / Linux from source
If you want to develop the kernel, then the following steps help you compiling the kernel from source using Gradle. Java JDK version needs to be between 8 and 15.

* Install SDKMAN: `curl -s "https://get.sdkman.io" | bash`
* You may need to activate the `sdk` with `source .bashrc` or `source .zshrc` or set the path to it.
* Install Gradle 4.8.1: `sdk install gradle 4.8.1`
* Use Gradle 4.8.1: `sdk use gradle 4.8.1`
* Optional, but strongly recommended: Create a virtual environment
* Install IRuta: `gradle clean installKernel -PinstallKernel.python=venv/bin/python --user` 
