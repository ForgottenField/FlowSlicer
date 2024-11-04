import os
import sys
import subprocess
import shutil

def run_maven_clean_package(pom_path):
    try:
        # Run Maven command to clean and package the project
        subprocess.check_call([
            "mvn", 
            "-f", pom_path, 
            "clean", "package"
        ])
        print("Maven build successful.")
    except subprocess.CalledProcessError as e:
        print("Maven build failed.")
        sys.exit(1)

def run_jar(jar_path, app_name):
    try:
        # Run the JAR file
        subprocess.check_call([
            "java", 
            "-jar", jar_path,
            "-name", app_name
        ])
    except subprocess.CalledProcessError as e:
        print("Failed to run the JAR file.")
        sys.exit(1)

if __name__ == '__main__' :
    print("Building FlowSlicer, please wait...")
    pom_file = "pom.xml"
    run_maven_clean_package(pom_file)

    original_jar_file = "target/flowslicer-2.0.jar"
    jar_file = "flowslicer-2.0.jar"
    if os.path.exists(original_jar_file):
        print("Successfully built!")
        shutil.copy(original_jar_file, jar_file)
        print("copy jar to the root directory.")
    else:
        print("Fail to build!")

    app_name = "Loop1"
    run_jar(jar_file, app_name)