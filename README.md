# Space-fluid Adaptive Sampling: a Field-based, Self-organising Approach

This repository contains the experiments presented in the paper 

> Space-fluid Adaptive Sampling: a Field-based, Self-organising Approach

Accepted at [COORDINATION 2022](http://www.discotec.org/2022/coordination.html)
realised with the [Protelis programming language](https://www.protelis.org) and the [Alchemist Simulator](https://github.com/AlchemistSimulator/Alchemist).

Code and data are permanently archived on Zenodo for all the stable working versions of this repository: [![DOI](https://zenodo.org/badge/451919712.svg)](https://zenodo.org/badge/latestdoi/451919712)

## Prerequisites

* A working version of Java, the supported version range is 8 to 17
* A working version of Python 3, including `pip`
* A working Internet connection

## How to launch

To run the example you can rely on the pre-configured [Gradle](https://gradle.org) build script.
It will automatically download all the required libraries, set up the environment, and execute the simulator via command line for you.
As first step, use `git` to locally clone this repository.

Simulations can be included in the `src/main/yaml` folder,
and executed via the `runAll` Gradle task.

For each YAML file in `src/main/yaml` a task `runFileName` will be created.

In order to launch, open a terminal and move to the project root folder, then on UNIX:
```bash
./gradlew runSimulation
```
On Windows:
```
gradlew.bat runSimulation
```

Press <kb>P</kb> to start the simulation.
For further information about the GUI, see the [graphical interface shortcuts](https://alchemistsimulator.github.io/reference/default-ui/).

Note that the first launch will require some time, since Gradle will download all the required files.
They will get cached in the user's home folder (as per Gradle normal behavior).

## Re-generating all the data

The experiment is entirely reproducible.
Regenerating all the data may take *weeks* on a well-equipped 2022 personal computer.
The process is CPU-intensive and we do not recommend running it on devices where heat can be damaging to the battery.

In order to re-run all the experiments, launch:
```bash
./gradlew runSimulationBatch
```
data will be generated into the `data` folder

## Re-generating the charts from the paper

If you are just willing to re-run the data analysis and generate all the charts in the paper (plus hundreds other),
you can use the data we generated in the past.

If you do not have a compatible version of Python 3 onboard, consider using [pyenv](https://github.com/pyenv/pyenv).
Assuming a Linux/MacOS environment, it can be installed with [pyenv-installer](https://github.com/pyenv/pyenv-installer) as follows:
```bash
curl https://pyenv.run | bash
exec $SHELL
pyenv install
```

Otherwise, follow the instructions of pyenv and pyenv-installer,
or install Python 3.10.2 or above (with pip), and then run:

```bash
pip install --upgrade pip --user
pip install -r requirements.txt --user
mkdir -p charts
python process.py
```
