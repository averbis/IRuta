# IRuta
A Jupyter kernel for [APACHE UIMA Ruta](https://uima.apache.org/ruta.html), a powerful rule-based engine for annotating documents.

## Try IRuta in a Binder
No installation required. You can immediately start experimenting with our example Ruta notebooks or write your own notebooks by clicking on the following link.
Please be patient, as this launches a Docker container which may take a short time.

[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/averbis/IRuta/HEAD)
## Installation
### Installing Ruta Kernel from our prepackaged releases
0. **Note**: You first will need to remove an old installation by running `jupyter kernelspec remove ruta`. 

1. Download the prepackaged distribution *IRuta-{release-version}.zip* from the latest Release in the [Releases](https://github.com/averbis/IRuta/releasesreleases) tab. 

2. Unzip it into a temporary location and change into the unzipped directory.

3.  Run the installer with the same python command used to install jupyter. The installer is a python script and has the same options as `jupyter kernelspec install`.

    ```bash
    # Pass the -h option to see the help page
    > python3 install.py -h

    # Otherwise a common install command is
    > python3 install.py --user
    ```

4.  Check that it installed with `jupyter kernelspec list` which should contain `ruta`.

### Running IRuta in a Docker container.
The repository contains a Dockerfile. You can simply execute it by running thw following two commands:
* Build the docker image: `docker build -t iruta .`
* Run the docker image: `docker run -p 8888:8888 iruta`

### Building IRuta Kernel on OS X / Linux from source
* Install SDKMAN: `curl -s "https://get.sdkman.io" | bash`
* You may need to activate the `sdk` with `source .bashrc` or `source .zshrc` or set the path to it.
* Install Gradle 4.8.1: `sdk install gradle 4.8.1`
* Use Gradle 4.8.1: `sdk use gradle 4.8.1`
* Optional, but strongly recommended: Create a virtual environment
* Install IRuta: `gradle clean installKernel -PinstallKernel.python=venv/bin/python --user` 