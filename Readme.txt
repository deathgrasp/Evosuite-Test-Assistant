There are a few Bash scripts, as well as a Java program inside.

All programs assume that:
We are running linux.
Defects4j is installed at ~/
We have hamcrest-core-1.3.jar, junit-4.12.jar, evosuite-runtime-1.0.3.jar, evosuite-1.0.3.jar files in the directory in which we are runningg the tests

The java program is used to run evosuite on a defects4j project, and count the number of exceptins in the resulting test suite.
The time allocated is based on the number of a class's appearance in defects4j, as a class that has been changed to solve a bug.
Edit BaseTime, ExtraTime, and runNumberOfTimes and recompile in order to change the time allocation and test numbers.
The program requires the ProjectBugs csvs in order to operate correctly.
To run, compile and run java Main <d4j project name> 

The scripts are used to run evosuite on a defects4j project, and count the number of bugs found.
This is done by creating tests on the version after the fix, running them on both fixed and pre-fix versions, and counting the bugs that appear only in the pre-fix version. 
A more detailed per-script functionality is found in them.
All scripts can be ran without input (./<script name>). In order to run on a remote location, used nohup and & is advised (nohup ./<script name> &)