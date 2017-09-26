import org.evosuite.symbolic.vm.string.Substring;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;


/**
 * Created by rel on 27-Mar-17.
 */
public class Main {
    private static int ExtraTime = 0;
    private static int BaseTime = 60;
	private static int runNumberOfTimes=3
    private static float totalTime = 0;
    private static int checkOneIn = 0;
    private static int testNumber;
    private static int iteration;
    private static boolean staticDynamic = false;
    private static HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> map = new HashMap<>();
    private static HashMap<String, Integer> errCount = new HashMap<>();
    private static HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> prevMap = new HashMap<>();
    private static HashMap<String, Integer> prevErrCount = new HashMap<>();
    private static HashMap<String, Integer> timeAllocated = new HashMap<>();
    private static String gs = "";
    private static int rng = new Random().nextInt();
    private static int numberOfFiles = -1;
    private static int numberOfException = -1;


    static String directoryName = "evosuite-tests/";
    static String pname = "";
    static String path = "";
    static String classesStartIn = "";

    enum doOnFiles{ //used to call correct function when running on all files
        CREATEDYNAMICTEST, ONLYEXCEPTION, CREATETEST, CHECKISBUGGY
    }
    //returns false if a bad string is received
    public static boolean setProject(String s) {
        String error = "Can't find a project with the name " + s + ". Please enter a valid one from the list:\n";
        error += "Lang\n";
        error += "Chart\n";
        error += "Time\n";
        error += "Math\n";
        error += "Closure";
        if (s.equals("Lang")) {
            directoryName = "evosuite-tests/";
            classesStartIn = "org";
            pname = "Lang";
            path = "Lang19b/target/classes"; //Lang
            checkOneIn = 1;
        } else if (s.equals("Chart")) {
            pname = "Chart";
            path = "Chart10b/jfreechart-1.2.0-pre1";
            checkOneIn = 6;
            directoryName = "evosuite-tests/";
            classesStartIn = "org";
        } else if (s.equals("Time")) {
            pname = "Time";
            path = "Time10b/target/classes";
            checkOneIn = 1;
            directoryName = "evosuite-tests/";
            classesStartIn = "org";
        } else if (s.equals("Math")) {
            pname = "Math";
            path = "Math50b/target/classes";
            checkOneIn = 6;
            directoryName = "evosuite-tests/";
            classesStartIn = "org";
        } else if (s.equals("Closure")) {
            pname = "Closure";
            classesStartIn = "com";//Closure
            path = "Closure10b/build/classes"; //Closure
            checkOneIn = 4;
            directoryName = "evosuite-tests/";
        } else {
            print(error);
            return false;
        }
        return true;
    }
    //script in to take a bugged class from defects4j, and run a test on the whole package it's in.
    //for example, if we get the class A.B.C, we will check the package A.B.*
    public static void CompareSingleCheckToPackage(String[] args){
        String name= args[0];
        //region Lang

        if (name.equals("Lang")){ //run over Lang, going in recursively
            int[] classes=new int[66];
            for (int i=1; i<=65; i++){
                String[] infoInput=ExecAndExport("defects4j info -p Lang -b "+i).split("---------------------");
                int modifiedPlace=0;
                for (int j=0; j<infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified"))
                    {
                        modifiedPlace=j;
                        break;
                    }
                }
                String klass=infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length-1].trim();
                String pack=klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Lang -v "+i+"\"f\" -w Lang"+i+"fixed\n");
                ExecAndExport(	"(cd Lang"+i+"fixed; ant compile)");
                String folder="Lang"+i+"fixed/target/classes";
                int t=60;
                for (int j=0; j<5; j++){
                    String[] packInput=ExecAndExport("java -jar evosuite-1.0.3.jar -prefix "+pack+" -projectCP "+folder+"  -Dsearch_budget="+t+" -Dassertion_timeout="+t+" -Dextra_timeout="+t).split("\\*");
                    for (int k=0; k<packInput.length; k++) {
                        if (packInput[k].contains("matching classes for prefix"))
                        {
                            modifiedPlace=k;
                            break;
                        }
                    }
                    classes[i]=Integer.parseInt(packInput[modifiedPlace].trim().split(" ")[1].trim());
                    exec("javac "+pack.replace(".","/")+"* -cp evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:"+folder);
                    exec("cp -r evosuite-tests evosuite-tests_Lang_"+i+"_"+j+"PACK");
                    exec("rm -rf evosuite-tests");
                }

            }

            for (int i=1; i<=65; i++){

                String[] infoInput=ExecAndExport("defects4j info -p Lang -b "+i).split("---------------------");
                int modifiedPlace=0;
                for (int j=0; j<infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified"))
                    {
                        modifiedPlace=j;
                        break;
                    }
                }
                String klass=infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length-1].trim();
                String pack=klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Lang -v "+i+"\"f\" -w Lang"+i+"buggy\n");
                ExecAndExport(	"(cd Lang"+i+"buggy; ant compile)");
                String folder="Lang"+i+"buggy/target/classes";
                int t=60;
                for (int j=0; j<5; j++){
                    String packInput=ExecAndExport("java -cp evosuite-runtime-1.0.3.jar:evosuite-tests_Lang_"+i+"_"+j+"PACK"+":junit-4.12.jar:hamcrest-core-1.3.jar:"+folder+" org.junit.runner.JUnitCore "+pack.replace(".","/")+"*>> FixedOnBuggy_Lang_"+i+"PACK");
                }
                String errorsString= exec("grep -c FAILURES\\!\\!\\! FixedOnBuggy_Lang_"+i+"PACK").trim();
                    errorsString=errorsString.substring(0,errorsString.length()-5);
                int errors=Integer.parseInt(errorsString);
                exec("echo Lang, "+i+", ???, " +t+", "+errors+" >> packagebugFile");
            }


            for (int i=1; i<=65; i++){
                String[] infoInput=ExecAndExport("defects4j info -p Lang -b "+i).split("---------------------");
                int modifiedPlace=0;
                for (int j=0; j<infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified"))
                    {
                        modifiedPlace=j;
                        break;
                    }
                }
                String klass=infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length-1].trim();
                String pack=klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Lang -v "+i+"\"f\" -w Lang"+i+"fixed\n");
                ExecAndExport(	"(cd Lang"+i+"fixed; ant compile)");
                String folder="Lang"+i+"fixed/target/classes";
                int t=60*classes[i];
                for (int j=0; j<5; j++){
                    String[] packInput=ExecAndExport("java -jar evosuite-1.0.3.jar -class "+klass+" -projectCP "+folder+"  -Dsearch_budget="+t+" -Dassertion_timeout="+t+" -Dextra_timeout="+t).split("\\*");
                    for (int k=0; k<packInput.length; k++) {
                        if (packInput[k].contains("matching classes for prefix"))
                        {
                            modifiedPlace=k;
                            break;
                        }
                    }
                    exec("javac "+pack.replace(".","/")+"* -cp evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:"+folder);
                    exec("cp -r evosuite-tests evosuite-tests_Lang_"+i+"_"+j);
                    exec("rm -rf evosuite-tests");
                }

            }

            for (int i=1; i<=65; i++){

                String[] infoInput=ExecAndExport("defects4j info -p Lang -b "+i).split("---------------------");
                int modifiedPlace=0;
                for (int j=0; j<infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified"))
                    {
                        modifiedPlace=j;
                        break;
                    }
                }
                String klass=infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length-1].trim();
                String pack=klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Lang -v "+i+"\"f\" -w Lang"+i+"buggy\n");
                ExecAndExport(	"(cd Lang"+i+"buggy; ant compile)");
                String folder="Lang"+i+"buggy/target/classes";
                int t=60*classes[i];
                for (int j=0; j<5; j++){
                    String packInput=ExecAndExport("java -cp evosuite-runtime-1.0.3.jar:evosuite-tests_Lang_"+i+"_"+j+":junit-4.12.jar:hamcrest-core-1.3.jar:"+folder+" org.junit.runner.JUnitCore "+pack.replace(".","/")+"*>> FixedOnBuggy_Lang_"+i);
                }
                String errorsString= exec("grep -c FAILURES\\!\\!\\! FixedOnBuggy_Lang_"+i).trim();
                errorsString=errorsString.substring(0,errorsString.length()-5);
                int errors=Integer.parseInt(errorsString);
                exec("echo Lang, "+i+", "+klass+", " +t+", "+errors+" >> packagebugFile");
            }


        }
        //endregion
        //region Time
        if (name.equals("Time")) { //run over time. NOT going in recursively
            int[] classes = new int[28];
            for (int i = 1; i <= 27; i++) {
                String[] infoInput = ExecAndExport("defects4j info -p Time -b " + i).split("---------------------");
                int modifiedPlace = 0;
                for (int j = 0; j < infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified")) {
                        modifiedPlace = j;
                        break;
                    }
                }
                String klass = infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length - 1].trim();
                String pack = klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Time -v " + i + "\"f\" -w Time" + i + "fixed\n");
                ExecAndExport("(cd Time" + i + "fixed; ant compile)");
                String folder = "Time" + i + "fixed/target/classes";
                if (i > 11) {
                    folder = "Time" + i + "fixed/build/classes";
                }
                int t = 60;
                for (int j = 0; j < 5; j++) {
                    File directory = new File(folder+"/"+pack.replace('.','/'));
                    // get all the files from a directory
                    File[] fList = directory.listFiles();
                    int counter=0;
                    for (File file : fList) {
                        if (file.isFile() && file.getAbsolutePath().contains(".java") || file.getAbsolutePath().contains(".class")) {
                        }

                    packInput = ExecAndExport("java -jar evosuite-1.0.3.jar -prefix " + pack + " -projectCP " + folder + "  -Dsearch_budget=" + t + " -Dassertion_timeout=" + t + " -Dextra_timeout=" + t).split("\\*");

                    classes[i] = Integer.parseInt(packInput[modifiedPlace].trim().split(" ")[1].trim());
                    exec("javac " + pack.replace(".", "/") + "* -cp evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:" + folder);
                    exec("cp -r evosuite-tests evosuite-tests_Time_" + i + "_" + j + "PACK");
                    exec("rm -rf evosuite-tests");
                }
                if (i == 1 || i == 2 || i == 12) { // two bugs in these versions
                    modifiedPlace++;
                    klass = infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length - 1].trim();
                    pack = klass.substring(0, klass.lastIndexOf('.'));
                    folder = "Time" + i + "fixed/target/classes";
                    if (i > 11) {
                        folder = "Time" + i + "fixed/build/classes";
                    }
                    t = 60;
                    for (int j = 0; j < 5; j++) {
                        String[] packInput = ExecAndExport("java -jar evosuite-1.0.3.jar -prefix " + pack + " -projectCP " + folder + "  -Dsearch_budget=" + t + " -Dassertion_timeout=" + t + " -Dextra_timeout=" + t).split("\\*");
                        for (int k = 0; k < packInput.length; k++) {
                            if (packInput[k].contains("matching classes for prefix")) {
                                modifiedPlace = k;
                                break;
                            }
                        }
                        classes[i] = Integer.parseInt(packInput[modifiedPlace].trim().split(" ")[1].trim());
                        exec("javac " + pack.replace(".", "/") + "* -cp evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:" + folder);
                        exec("cp -r evosuite-tests evosuite-tests_Time_" + i + "_" + j + "PACK2");
                        exec("rm -rf evosuite-tests");
                    }
                }

            }

            for (int i = 1; i <= 27; i++) {

                String[] infoInput = ExecAndExport("defects4j info -p Time -b " + i).split("---------------------");
                int modifiedPlace = 0;
                for (int j = 0; j < infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified")) {
                        modifiedPlace = j;
                        break;
                    }
                }
                String klass = infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length - 1].trim();
                String pack = klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Time -v " + i + "\"f\" -w Time" + i + "buggy\n");
                ExecAndExport("(cd Time" + i + "buggy; ant compile)");
                String folder = "Time" + i + "buggy/target/classes";
                if (i > 11) {
                    folder = "Time" + i + "buggy/build/classes";
                }
                int t = 60;
                for (int j = 0; j < 5; j++) {
                    String packInput = ExecAndExport("java -cp evosuite-runtime-1.0.3.jar:evosuite-tests_Time_" + i + "_" + j + "PACK" + ":junit-4.12.jar:hamcrest-core-1.3.jar:" + folder + " org.junit.runner.JUnitCore " + pack.replace(".", "/") + "*>> FixedOnBuggy_Time_" + i + "PACK");
                }
                String errorsString = exec("grep -c FAILURES\\!\\!\\! FixedOnBuggy_Time_" + i + "PACK").trim();
                errorsString = errorsString.substring(0, errorsString.length() - 5);
                int errors = Integer.parseInt(errorsString);
                exec("echo Time, " + i + ", ???, " + t + ", " + errors + " >> packagebugFile");
            }


            for (int i = 1; i <= 27; i++) {
                String[] infoInput = ExecAndExport("defects4j info -p Time -b " + i).split("---------------------");
                int modifiedPlace = 0;
                for (int j = 0; j < infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified")) {
                        modifiedPlace = j;
                        break;
                    }
                }
                String klass = infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length - 1].trim();
                String pack = klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Time -v " + i + "\"f\" -w Time" + i + "fixed\n");
                ExecAndExport("(cd Time" + i + "fixed; ant compile)");
                String folder = "Time" + i + "fixed/target/classes";
                if (i > 11) {
                    folder = "Time" + i + "fixed/build/classes";
                }
                int t = 60 * classes[i];
                for (int j = 0; j < 5; j++) {
                    String[] packInput = ExecAndExport("java -jar evosuite-1.0.3.jar -class " + klass + " -projectCP " + folder + "  -Dsearch_budget=" + t + " -Dassertion_timeout=" + t + " -Dextra_timeout=" + t).split("\\*");
                    for (int k = 0; k < packInput.length; k++) {
                        if (packInput[k].contains("matching classes for prefix")) {
                            modifiedPlace = k;
                            break;
                        }
                    }
                    exec("javac " + pack.replace(".", "/") + "* -cp evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:" + folder);
                    exec("cp -r evosuite-tests evosuite-tests_Time_" + i + "_" + j);
                    exec("rm -rf evosuite-tests");
                }

            }

            for (int i = 1; i <= 27; i++) {

                String[] infoInput = ExecAndExport("defects4j info -p Time -b " + i).split("---------------------");
                int modifiedPlace = 0;
                for (int j = 0; j < infoInput.length; j++) {
                    if (infoInput[j].contains("List of modified")) {
                        modifiedPlace = j;
                        break;
                    }
                }
                String klass = infoInput[modifiedPlace].split("-")[infoInput[modifiedPlace].split("-").length - 1].trim();
                String pack = klass.substring(0, klass.lastIndexOf('.'));
                ExecAndExport("\tdefects4j checkout -p Time -v " + i + "\"f\" -w Time" + i + "buggy\n");
                ExecAndExport("(cd Time" + i + "buggy; ant compile)");
                String folder = "Time" + i + "buggy/target/classes";
                if (i > 11) {
                    folder = "Time" + i + "buggy/build/classes";
                }
                int t = 60 * classes[i];
                for (int j = 0; j < 5; j++) {
                    String packInput = ExecAndExport("java -cp evosuite-runtime-1.0.3.jar:evosuite-tests_Time_" + i + "_" + j + ":junit-4.12.jar:hamcrest-core-1.3.jar:" + folder + " org.junit.runner.JUnitCore " + pack.replace(".", "/") + "*>> FixedOnBuggy_Time_" + i);
                }
                String errorsString = exec("grep -c FAILURES\\!\\!\\! FixedOnBuggy_Time_" + i).trim();
                errorsString = errorsString.substring(0, errorsString.length() - 5);
                int errors = Integer.parseInt(errorsString);
                exec("echo Time, " + i + ", " + klass + ", " + t + ", " + errors + " >> packagebugFile");
            }
        }
        //endregion
    }
	}
    public static String ExecAndExport(String s){
        return exec(("export PATH=$PATH:~/defects4j/framework/bin\n"+s).split("\n"));
    }
    public static void main(String[] args) {
        if (args.length <= 0) {
            print("enter the project name you wish to run. Write multiple to run multiple ones one after the other.");
        }
        for (int k = 0; k < args.length; k++) {
            if (!setProject(args[k]))
                return;


            testNumber = 0;
            //createFileTests();
            //countBugs();
            //generateDynamicTimesMath();
            for (int i = 0; i < runNumberOfTimes; i++) { // run 3 tests, each test will include one base run (60/all) and 3 iterations of dynamic on it.
                //grant access to the file with commands to run on evosuite. base case. Optional: change create and run from java instead of a bash script
                iteration = 0;
                print("starting");
                int tmp = ExtraTime;
                ExtraTime = 0;
                getFileTests();
                ExtraTime = tmp;
                //exec("chmod 777 runme+"+pname+"Weighted_Base60_ExtraFactor0.bat");
                //exec("./runme"+pname+"Weighted_Base60_ExtraFactor0.bat");
                print(gs);
                exec(gs.split("\n"));
                print("exec done");
                countBugs();
                print("base done");

                gs = "";
                for (int j = 0; j < runNumberOfTimes; j++) {
                    iteration++;
                    generateDynamicTimesMath();
                    print("test " + testNumber + " iteration " + iteration + " completed");
                    exec("cp -r evosuite-tests evosuite-tests_" + pname + "_" + testNumber + "_" + iteration); //copy test directory in case we want to take a look
                }
                countBugs(); //used to print the last iteration
                testNumber++;
            }
        }
        print("finished running");
    }

    //print the current state of the tests, and run createDynamicTestEvoRunner on all files
    public static void generateDynamicTimesMath() {
        //reset data from previous iterations
        prevErrCount = errCount;
        prevMap = map;
        errCount.clear();
        map.clear();

        countBugs(); //used to print the found data, and to populate the database

        gs = "";
        HashSet<String> remove = new HashSet<>(); //remove useless files we don't want to check
        for (String s : errCount.keySet()) {
            if (s.contains("_ESTest_scaf"))
                remove.add(s);
        }
        for (String s : remove) {
            errCount.remove(s);
        }
        totalTime = 0;
        timeAllocated.clear();
        DoOnAllFiles(path,doOnFiles.CREATEDYNAMICTEST);
        print("executing");
        exec(gs.trim().split("\n"));
        String fileToWrite = "runme" + pname + "Weighted_Base" + testNumber + "_" + iteration;
        String content = gs;
        writeFile(fileToWrite, content);
        /*content="#!/bin/bash\nchmod 777 "+fileToWrite+"\n./"+fileToWrite;
        fileToWrite="runme"+pname+"Weighted_Base";
        writeFile(fileToWrite, content);*/
    }

    //gets a directory and an enum for function. according to the enum, use a function on all the java/class files in the directory
    public static void DoOnAllFiles(String directoryName, doOnFiles func) {
        File directory = new File(directoryName);
        // get all the files from a directory
        File[] fList = directory.listFiles();
        int counter=0;
        for (File file : fList) {
            if (file.isFile() && file.getAbsolutePath().contains(".java") || file.getAbsolutePath().contains(".class")) {
//                onlyExceptions(file);
                switch (func){
                    case CREATETEST:
                        if (counter == 0) {
                            createTestEvoRunner(file);
                            counter = checkOneIn;
                        }
                        break;
                    case CHECKISBUGGY:
                        if (counter == 0) {
                            IsBuggy(file);
                            counter = checkOneIn;
                        }
                        break;
                    case ONLYEXCEPTION:
                        onlyExceptions(file);
                        break;
                    case CREATEDYNAMICTEST:
                        createDynamicTestEvoRunner(file);
                        break;
                }
                counter--;
            } else if (file.isDirectory()) {
                DoOnAllFiles(file.getAbsolutePath(), func);

            }
        }
    }
/*
    used to populate the string 'gs' with tests.
    time for each test is based on the results from the previous iteration, so that files where we found exceptions get more time.
    if 'staticDynamic' is true, then each test will get the basetime for each test, with additional time.
    if 'staticDynamic' is false, the total time for the project is fixed, and files where we found exceptions gets more time.
 */
    private static void createDynamicTestEvoRunner(File file) {
        if (file.getName().contains("$")) return;
        String name = file.getName().substring(0, file.getName().length() - ".class".length());
        if (!errCount.containsKey(name))
            return;
        int[] tmp = {0}; //ide requested to look like this...
        errCount.forEach((x, y) -> tmp[0] += y);
        final int numberOfExceptions = tmp[0];
        numberOfFiles = errCount.keySet().size();
        int numberOfFilesWithExceptions = 0;
        for (String s : errCount.keySet()) {
            if (errCount.get(s) > 0)
                numberOfFilesWithExceptions += 1;
        }

        float weightPerExtraTimeExceptionFile = 1f * (BaseTime * numberOfFiles) / (numberOfFiles + numberOfFilesWithExceptions * (ExtraTime));
        if (staticDynamic)
            weightPerExtraTimeExceptionFile = BaseTime;
        String filename = file.getAbsolutePath();
        String classname = filename.substring(filename.indexOf("/" + classesStartIn) + 1).replace('/', '.');
        classname = classname.substring(0, classname.length() - ".class".length());

        int weight = (int) weightPerExtraTimeExceptionFile;
        if (errCount.containsKey(name) && errCount.get(name) > 0) {
            weight += ExtraTime * weightPerExtraTimeExceptionFile;
        }
        timeAllocated.put(name, weight);
        //gs+="java -jar evosuite-1.0.3.jar -class "+classname+" -projectCP "+path+"  -Dsearch_budget="+weight+" -Dassertion_timeout="+weight+"\n"; //windows
        gs += "java -jar evosuite-1.0.3.jar -class " + classname + " -projectCP " + path + "  -Dsearch_budget=" + weight + " -Dassertion_timeout=" + weight + "\n"; //linux server
        totalTime += weight;
        //gs+="javac evosuite-tests/"+ classname.replace('.','/').substring(0,classname.length()-".class".length()) +"_ESTest.java -cp \"evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:\""+path+"\n";
        //gs+="javac evosuite-tests/"+ classname.replace('.','/').substring(0,classname.length()-".class".length()) +"_ESTest.java -cp \"evosuite-runtime-1.0.3.jar;evosuite-tests;target/dependency/junit-4.12.jar;target/dependency/hamcrest-core-1.3.jar;"+path+"\"\n";
        //gs+="java -cp \"evosuite-runtime-1.0.3.jar:evosuite-tests:junit-4.12.jar:hamcrest-core-1.3.jar:Lang/\""+path+" org.junit.runner.JUnitCore "+classname.replace('.','/').substring(0,classname.length()-".class".length()) +"\"_ESTest\"\n";
        //gs+="java -cp \"evosuite-runtime-1.0.3.jar;evosuite-tests;target/dependency/junit-4.12.jar;target/dependency/hamcrest-core-1.3.jar;"+path+"\" org.junit.runner.JUnitCore "+classname.replace('.','/').substring(0,classname.length()-".class".length()) +"\"_ESTest\"\n";
    }
/*
    passes over the evosuite tests, and enters the errors found into the various data structures.
    afterwards, creates a csv with the data.
 */

    public static void countBugs() {
        gs = "";
        DoOnAllFiles(directoryName, doOnFiles.ONLYEXCEPTION);
        String content = "Test Number, Iteration, Full path, Test file, Exception, Throwing class, AllocatedTime, HadException, Error message\n" + gs;
        String fileToWrite = pname + "_" + testNumber + "_" + iteration + ".csv";
        writeFile(fileToWrite, content);
    }

    //create files that use evosuite on a project with fixed time, changing based on the number of errors that defects4j reported
    //also creates a project_runme file to run all of the tests with from a single file
    public static void createFileTests() {

        //listf("C:\\Users\\rel\\workspace\\evotrimmer\\tests");
        //createTest("C:\\Users\\rel\\workspace\\Lang19b\\target\\classes");
        String runme = "#!/bin/bash\n";

        for (int i = 30; i <= 120; i += 30)
            for (int j = 0; j < 5; j += 4) {
                BaseTime = i;
                ExtraTime = j;
                gs = "";
                totalTime = 0;
                getFileTests();

                System.out.println("total time spent on testing: " + totalTime);
                String fname = "runme" + pname + "Weighted_Base" + BaseTime + "_ExtraFactor" + ExtraTime + ".bat";
                runme = runme + "chmod 777 " + fname + "\n" +
                        "./" + fname + "\n" +
                        "cp -r evosuite-tests evosuite-tests_" + BaseTime + "_" + ExtraTime + "\n" +
                        "mv evosuite-tests" + BaseTime + "_" + ExtraTime + " " + pname + "_tests\n";
                String content = "Full path, Name, Exception, From, Text\n" + gs;
                writeFile(fname, content);

                String fileToWrite = pname + "_runme";
                content = runme;
                writeFile(fileToWrite, content);
            }
    }
//run createTestEvoRunner on all files
    public static void getFileTests() {
        numberOfException = 0;
        numberOfFiles = 0;
        DoOnAllFiles(path,doOnFiles.CHECKISBUGGY);
        DoOnAllFiles(path,doOnFiles.CREATETEST);
    }
// helper function to createTestEvoRunner, used to couunt the actual files and files with exception
    private static void IsBuggy(File file) {
        File weights = new File("ProjectBugs_" + pname + ".csv");
        Stream<String> stream = null;
        try {
            stream = Files.lines(weights.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList<>();
        stream.forEach(x -> list.add(x));
        HashMap<String, Integer> stringWeights = new HashMap<String, Integer>();
        list.forEach(x -> {
            String[] split = x.trim().split(",");
            stringWeights.put(split[0].trim(), Integer.valueOf(split[1].trim()));
        });

        String filename = file.getAbsolutePath();
        if (filename.contains("$")) return;
        String classname = filename.substring(filename.indexOf("/" + classesStartIn) + 1).replace('/', '.');
        classname = classname.substring(0, classname.length() - ".class".length());
        //String path=filename.substring(0,filename.indexOf("\\org")); //Windows
        if (stringWeights.containsKey(classname)) {
            numberOfException += stringWeights.get(classname);
        }
        numberOfFiles += 1;
    }
    //passes over the project classes, and populate the string 'gs' with commands to run evosuite on them.
    //processes gets additional time compared to others based on defects4j errors.
    private static void createTestEvoRunner(File file) {
        final float weightPerFile = 1f * (BaseTime * numberOfFiles) / (numberOfFiles + numberOfException * (ExtraTime));
        System.out.println(weightPerFile);
        //File weights = new File("E:\\dropbox\\Dropbox\\28.4.17 backup unsorted\\workspace\\evotrimmer\\ProjectBugs_"+pname+".csv");
        File weights = new File("ProjectBugs_" + pname + ".csv");
        Stream<String> stream = null;
        try {
            stream = Files.lines(weights.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList<>();
        stream.forEach(x -> list.add(x));
        HashMap<String, Integer> stringWeights = new HashMap<String, Integer>();
        list.forEach(x -> {
            String[] split = x.trim().split(",");
            stringWeights.put(split[0].trim(), Integer.valueOf(split[1].trim()));
        });

        String filename = file.getAbsolutePath();
        if (filename.contains("$")) return;
        String classname = filename.substring(filename.indexOf("/" + classesStartIn) + 1).replace('/', '.');
        classname = classname.substring(0, classname.length() - ".class".length());
        //String path=filename.substring(0,filename.indexOf("\\org")); //Windows
        //String path= "Math50b/target/classes"; //linux server
        int weight = (int) weightPerFile;
        if (stringWeights.containsKey(classname)) {
            weight += weight * stringWeights.get(classname) * (ExtraTime);
        }
        //gs+="java -jar evosuite-1.0.3.jar -class "+classname+" -projectCP "+path+"  -Dsearch_budget="+weight+" -Dassertion_timeout="+weight+"\n"; //windows
        gs += "java -jar evosuite-1.0.3.jar -class " + classname + " -projectCP " + path + "  -Dsearch_budget=" + weight + " -Dassertion_timeout=" + weight + "\n"; //linux server
        totalTime += weight;
        //gs+="javac evosuite-tests/"+ classname.replace('.','/').substring(0,classname.length()-".class".length()) +"_ESTest.java -cp \"evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:\""+path+"\n";
        //gs+="javac evosuite-tests/"+ classname.replace('.','/').substring(0,classname.length()-".class".length()) +"_ESTest.java -cp \"evosuite-runtime-1.0.3.jar;evosuite-tests;target/dependency/junit-4.12.jar;target/dependency/hamcrest-core-1.3.jar;"+path+"\"\n";
        //gs+="java -cp \"evosuite-runtime-1.0.3.jar:evosuite-tests:junit-4.12.jar:hamcrest-core-1.3.jar:Lang/\""+path+" org.junit.runner.JUnitCore "+classname.replace('.','/').substring(0,classname.length()-".class".length()) +"\"_ESTest\"\n";
        //gs+="java -cp \"evosuite-runtime-1.0.3.jar;evosuite-tests;target/dependency/junit-4.12.jar;target/dependency/hamcrest-core-1.3.jar;"+path+"\" org.junit.runner.JUnitCore "+classname.replace('.','/').substring(0,classname.length()-".class".length()) +"\"_ESTest\"\n";
    }


    /*
        parse the evosuite tests. inserts relevant data to the file structers for later use. inserts the bugs' data into gs for later print or access
    */
    public static void onlyExceptions(File file) {
        try {
            boolean TRY = false;
            boolean CATCH = false;
            HashMap<String, HashMap<String, ArrayList<String>>> hashmap = new HashMap<>();
            map.put(file.getAbsolutePath(), hashmap);
            Stream<String> stream = Files.lines(file.toPath(), StandardCharsets.UTF_8);
            ArrayList<String> list = new ArrayList<>();
            stream.forEach(x -> list.add(x));
            ArrayList<String> modified = new ArrayList<>();
            String error = null;
            String errorText = null;
            String errorSource = null;
            for (int i = 0; i < list.size(); i++) {
                String filename = file.getName();
                filename = filename.substring(0, filename.length() - "_ESTest.java".length());
                if (!errCount.containsKey(filename))
                    errCount.put(filename, 0);
                String s = list.get(i);
                if (TRY || CATCH) {
                    if (TRY) {
                        if (s.contains("fail(\"Expecting")) {
                            modified.add("////" + s); //add a comment to the try lines
                        } else if (s.contains("} catch(")) {
                            modified.add("////" + s);//add a comment to the catch
                            TRY = false;
                            CATCH = true;

                            error = s.split("\\(")[1].split(" e")[0];

                        } else {

                            modified.add(s);
                        }
                    } else { //catch
                        if (s.contains("}")) {
                            CATCH = false;
                        }
                        if (s.contains("assertThrown")) {
                            errorSource = s.split("\"")[1];
                            if (!hashmap.containsKey(error)) {
                                hashmap.put(error, new HashMap<>());
                                hashmap.get(error).put(errorSource, new ArrayList<>());
                                hashmap.get(error).get(errorSource).add(errorText);
                            } else if (!hashmap.get(error).containsKey(errorSource)) {
                                hashmap.get(error).put(errorSource, new ArrayList<>());
                                hashmap.get(error).get(errorSource).add(errorText);
                            } else {
                                hashmap.get(error).get(errorSource).add(errorText);
                            }
                            int allocated = 0;
                            String name = file.getName().substring(0, file.getName().length() - "_ESTest.java".length());
                            boolean hadException = false;
                            if (!file.getName().contains("$")) {
                                hadException = prevMap.containsKey(file.getAbsolutePath());
                                if (timeAllocated.containsKey(name)){
                                    allocated = timeAllocated.get(name);
                                }
                            }
                            if (!RemoveNoneBugException(errorSource)) {
                                // testnumber | iteration | full path | test file | exception | throwing class | allocated | had exception | error massage
                                gs += testNumber + "," + iteration + "," + file.getAbsolutePath() + ", " + file.getName() + ", " + error + ", " + errorSource + "," + allocated + "," + hadException + ", " + errorText + "\n";
                                errCount.put(filename, errCount.get(filename) + 1);
                            }
                        } else if (CATCH && s.trim().length() > 2) {
                            errorText = s.split("//")[1];
                        }
                        modified.add("////" + s);//add a comment to the catch lines
                    }
                } else if (s.contains("try")) {
                    TRY = true;
                    modified.add("////" + s);//add a comment to the try
                } else {
//                    modified.add(s);
                }
            }
            //Files.write(file.toPath(),modified);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Function in charge of removing exceptions that we believe never indicate a bug.
    //Return true if the exception never indicate a bug.
    private static boolean RemoveNoneBugException(String throwingClass) {
        return throwingClass.toLowerCase().contains("evosuite");
    }

    //takes a java test file, remove the try/catch from tests, causing them to file when ran.
    public static void commentExceptions(File file) {
        try {
            boolean TRY = false;
            boolean CATCH = false;
            Stream<String> stream = Files.lines(file.toPath(), StandardCharsets.UTF_8);
            ArrayList<String> list = new ArrayList<>();
            stream.forEach(x -> list.add(x));
            ArrayList<String> modified = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                String s = list.get(i);
                if (TRY || CATCH) {
                    if (TRY) {
                        if (s.contains("fail(\"Expecting")) {
                            modified.add("////" + s); //add a comment to the try lines
                        } else if (s.contains("} catch(")) {
                            modified.add("////" + s);//add a comment to the catch
                            TRY = false;
                            CATCH = true;
                        } else {
                            modified.add(s);
                        }
                    } else { //catch
                        if (s.contains("}")) {
                            CATCH = false;
                        }
                        modified.add("////" + s);//add a comment to the catch lines
                    }
                } else if (s.contains("try")) {
                    TRY = true;
                    modified.add("////" + s);//add a comment to the try
                } else {
                    modified.add(s);
                }
            }
            Files.write(file.toPath(), modified);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    opens a bash in the current directory, and executes the command
     */
    public static String exec(String command) {
        Process p = null;
        String output="";
        print("starting to execute: " + command);
        try {
            //create the process and instantiate variables
            p = new ProcessBuilder("bash").start();
            PrintWriter stdin = new PrintWriter(p.getOutputStream());
            stdin.println(command+ " 2>&1"); //give the process (bash) our command
            BufferedReader stdout =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stderr =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            print("streams started");
            String i = "";
            String j = "";
            stdin.close(); //we finished giving commands at this point

            while (i != null) { //pipe the process' stdout to our stdout
                i = stdout.readLine();
                print(i);
                output+=i;
            }

            while (j != null) { // pipe te process' stderr to our stdout
                j = stderr.readLine();
                print(j);
            }
            print("streams ended");
            p.waitFor();//wait for the process to die
//            executeCommands();
        } catch (Exception e) {
            System.out.print("\n\n\n failed! exception: " +e+"\n\n\n");
        } finally {
            if (p != null)
                p.destroy();
            else {
                print("failed to destroy the a process, as it was already null.");
            }
        }
        return output;
    }
    //forall exec on a collection
    public static String exec(Collection<String> commandList) {

        String ret="";
        for (String s : commandList) {
            ret+=exec(s);
        }
        return ret;
    }
    //forall exec on a list
    public static String exec(String[] list) {
        String ret="";
        for (String s : list) {
            ret+=exec(s);
        }
        return ret;
    }

    //shorthand for system.out.println
    public static void print(String s) {
        System.out.println(s);
    }

    //write a file fname with body content
    private static void writeFile(String fname, String content) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {

            //fw = new FileWriter("C:\\Users\\rel\\workspace\\evotrimmer\\Exceptions.csv");
            fw = new FileWriter(fname);
            bw = new BufferedWriter(fw);
            bw.write(content);
            System.out.println("Done");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
    }
}
