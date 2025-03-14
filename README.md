# FlowSlicer

**FlowSlicer** is a tool designed to streamline the debloating and slicing of Android applications, enabling the identification and analysis of multi-level dependence and generating debloated apps based on the built MDG. This repository includes all necessary code, configuration files, and raw data to reproduce our experiments.

## Table of Contents

- [FlowSlicer](#flowslicer)
  - [Table of Contents](#table-of-contents)
  - [Project Structure](#project-structure)
  - [Setup and Requirements](#setup-and-requirements)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
  - [Usage](#usage)
  - [Experiment Reproduction](#experiment-reproduction)

## Project Structure

The repository is organized as follows:

- **`StubDroid/`**：Contains files related to StubDroid, used to obtain data flows from library methods.
  
- **`apk/`**：Contains sample APK files, including those from DroidBench, COVABench$_1$, and COVABench$_2$.

- **`debloated_apk/`**：Contains debloated APK files.

- **`config/`**：Stores configuration files for setting up the slicing parameters, such as source-sink APIs and Android callback APIs.

- **`experiment/`**：Contains experimental results files to facilitate easy replication of experiments and to benchmark the tool’s performance.

- **`flowdroid_result/`**：Contains FlowDroid analysis results before and after debloating.

- **`lib/`**：Includes the Android jar file needed for running FlowSlicer and related python scripts.

- **`src/`**：The main source code for FlowSlicer.

- **`summariesManual/`**：Summary files used in FlowDroid's LazySummaryProvider.

- **`AndroidCallbacks.txt`**：A list of Android callback methods that are considered in the slicing process.

- **`README.md`**：This file, providing an overview and instructions on using the repository.

- **`SourcesAndSinks.txt`**：Lists source and sink API methods for identifying data flow leaks.

- **`SourcesAndSinks_COVA.txt`**：The source-sink API list used in our tool。This list is a simplified version of the default source-sink API list from the prior work [COVA](https://github.com/secure-software-engineering/COVA).

- **`flowslicer-2.0.jar`**：The compiled jar file for FlowSlicer, ready for execution.

- **`pom.xml`**：Maven configuration file for managing dependencies and building the project.

- **`virtualedges.xml`**：Adds virtual edges in the dependency graph, useful for representing implicit invoking edges performed by the Android framework.

## Setup and Requirements

### Prerequisites

- **Java**：Version 17 or later.
- **Maven**：For managing project dependencies and building the tool.
- **Android SDK**：Just use the default Android SDK in the lib/platforms/android23/ directory.
- **Soot and FlowDroid**：Use the pom.xml file to include Soot and FlowDroid dependencies.

### Installation

Download the repository and Build the project using Maven:

```bash
cd FlowSlicer
mvn clean install
```

## Usage

FlowSlicer provides a CLI to specify APK files, source-sink configurations, and slicing options. The main steps to run the tool are as follows:

1. **Set Up Configuration**: Adjust settings in the config/config.json file to specify the source-sink API list and callback list according to your requirements. It is ok to use the default config.json file to achieve a common debloating task.
2. **Run FlowSlicer**: Use the following command to analyze an APK and generate sliced APK files:

    ```bash
    java -jar [jarFile] [options] [-n] [-m] E.g., -n Loop1 -m MatchSliceMode
    -c,--config <arg>                             -c [default: config/config.json]: Path to config.json
    -cg,--callGraphAlgorithm <arg>                -cg [default: CHA]: Set algorithm for CG, CHA or SPARK.
    -cm,--dependencyGraphConstructionMode <arg>   -cm [default: PartialConstructionMode]: PartialConstructionMode: Construct the Dependency Graph partially.
                                               GlobalConstructionMode: Construct the Dependency Graph globally.
    -d,--draw                                     -d : draw the dependency graph of sliced app.
    -fo,--flowDroidOutput <arg>                   -fo [default: flowdroid/apkname/output.xml]: Set the path of flowdroid
                                               analysis output file.
    -fp,--flowDroidProduct <arg>                  -fp [default: flowdroid/apkname/product.xml]: Set the path of flowdroid
                                               analysis product file.
    -fr,--flowDroidResult <arg>                   -fr [default: flowdroid/apkname/result.xml]: Set the path of flowdroid
                                               analysis result file.
    -h,--help                                     -h: Show the help information.
    -ICC <arg>                                    -ICC [default: ICCResult/]: Set the file path of ICC analysis
                                               results(this tool supports ICCBot only).
    -j,--androidJar <arg>                         -j [default: lib/platforms]: Set the path of android jar platform.
    -m,--mode <arg>                               -m FlowSliceMode: Do FlowDroid analysis first, then slice the app and perform FlowDroid analysis again.
                                               MatchSliceMode: Slice apk and then perform FlowDroid analysis.
                                               FlowDroidMode: Run flowdroid only.
    -n,--name <arg>                               -n : Set the name of the apk under analysis.
    -o,--output <arg>                             -o [default: outputDir]: Set the output folder of sliced apk.
    -of,--outputFormat <arg>                      -of [default: APK]: Set the output format of sliced apk (APK or JIMPLE or
                                               CLASS).
    -p,--path <arg>                               -p [default: apk/apkName.apk]: Set the relative path of the apk under
                                               analysis.
    -pc,--permissionConsidered                    -pc: Set that permissions are not considered when pre-matching
                                               source-sink invocations.
    -r,--random                                   -r: Set that the slicing criteria are selected randomly from one source
                                               and one sink invocation.
    -sd,--stubDroidDir <arg>                      -sd [default: StubDroid/StubDroidSummaries]: Set the path of StubDroid
                                               Summaries Dir.
    -sdm,--stubDroidManualDir <arg>               -sdm [default: StubDroid/StubDroidSummaries/manual]: Set the path of
                                               StubDroid Summaries Manual Dir.
    -t,--time <arg>                               -t [default: 60]: Set the max running time (min).
    -ve,--virtualEdgesFile <arg>                  -ve [default: config/virtualedges.xml]: Set the path of Virtual Edges
                                               file.
    ```

    You can use the compiled jar file in the directory directly or compile the project through the pom.xml file by yourself.
3. **Output**: The sliced APKs will be generated in the output directory specified in the options. Logs and any errors encountered will be outputted in the logs/ directory for debugging purposes.

## Experiment Reproduction

To replicate the experiments documented in our research paper:

1. **Use Sample APKs**: The apk/ directory contains DroidBench, COVABench$_1$, and COVABench$_2$ APKs which were used to evaluate FlowSlicer’s performance. You may replace these with other APKs of your choice, but for exact replication, use the provided samples.

2. **Revise the script**: Navigate to the lib/script/ directory, where you’ll find the run.py script to run FlowSlicer directly. Before running the script, you need to replace the variables in the script with suitable targets for your replication. For example, you may replace the "apk_directory" with the directory of apps to be debloated and replaced the default execution mode with "flowslicemode", "matchslicemode", or "flowdroidmode" according to your targets. The difference between these three modes is that "flowslicemode" performs a FlowDroid analysis first, slices the app, and finally performs FlowDroid analysis on the debloated app; "matchslicemode" directly performs the debloating task and utilizes FlowDroid to analyze the debloated app; "flowdroidmode" just performs a FlowDroid analysis only.

3. **Run the script**: Utilize the `nohup` command to run the run.py script process:

    ```bash
    cp /lib/script/run.py . 
    nohup python3 run.py &
    ```

4. **Experiment details**: For three RQs, the execution configurations are as follows:

- RQ1: Run the "flowslicemode" on DroidBench and COVABench$_1$. The default configuration is with the permission check. So add the "-pc" option to obtain results without the permission check.
- RQ2: Run the "matchslicemode" on COVABench$_1$ and COVABench$_2$.
- RQ3: Reuse the results from RQ1 and RQ2. Open the option "-r" to randomly select a pair of source and sink API as the slicing criteria and observe the changes on code removal percentages.