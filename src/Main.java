import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;


/**
 * Created by rel on 27-Mar-17.
 */
public class Main {
    private static int ExtraTime = 4;
    private static int BaseTime = 60;
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
            for (int i = 0; i < 3; i++) { // run 3 tests, each test will include one base run (60/all) and 3 iterations of dynamic on it.
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
                for (int j = 0; j < 3; j++) {
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

    //TODO: combine the following functions to be a function getting an arguement and acts on all of it
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
//        float weightPerExceptionFile=numberOfFiles*BaseTime/numberOfFilesWithExceptions;//TODO: add baseline, weight and so on..
        float weightPerExtraTimeExceptionFile = 1f * (BaseTime * numberOfFiles) / (numberOfFiles + numberOfFilesWithExceptions * (ExtraTime));
        if (staticDynamic)
            weightPerExtraTimeExceptionFile = BaseTime;
        String filename = file.getAbsolutePath();
        String classname = filename.substring(filename.indexOf("/" + classesStartIn) + 1).replace('/', '.');
        classname = classname.substring(0, classname.length() - ".class".length());
        //String path=filename.substring(0,filename.indexOf("\\org")); //Windows
//        if (errCount.containsKey(name)){
//            totalTime+=weightPerExceptionFile*errCount.get(name);
//            weight=(int)weightPerExceptionFile*errCount.get(name);
//        }
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

        /*TODO:
        remove the .class

        for linux:
            change ; to :
            \ to /
            C:\ to /mnt/c/
            add #!/bin/bash



            check: does line 2 and 3 are even required???
         */
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

    public static void getFileTests() {
        numberOfException = 0;
        numberOfFiles = 0;
        DoOnAllFiles(path,doOnFiles.CHECKISBUGGY);
        DoOnAllFiles(path,doOnFiles.CREATETEST);
    }

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

        /*TODO:
        remove the .class

        for linux:
            change ; to :
            \ to /
            C:\ to /mnt/c/
            add #!/bin/bash



            check: does line 2 and 3 are even required???
         */
    }

    //    private static void createBalancedEvoRunner(File file) {
//        String filename=file.getAbsolutePath();
//        if (filename.contains("$")) return;
//        String classname=filename.substring(filename.indexOf("\\org")+1).replace('\\','.');
//        classname=classname.substring(0,classname.length()-".class".length());
//        String path=filename.substring(0,filename.indexOf("\\org"));
//        gs+="java -jar evosuite-1.0.3.jar -class "+classname+" -projectCP "+path+"  -Dsearch_budget="+60+" -Dassertion_timeout="+60+"\n";
//        //gs+="javac evosuite-tests/"+ classname.replace('.','/').substring(0,classname.length()-".class".length()) +"_ESTest.java -cp \"evosuite-runtime-1.0.3.jar:evosuite-tests:hamcrest-core-1.3.jar:junit-4.12.jar:\""+path+"\n";
//            //gs+="javac evosuite-tests/"+ classname.replace('.','/').substring(0,classname.length()-".class".length()) +"_ESTest.java -cp \"evosuite-runtime-1.0.3.jar;evosuite-tests;target/dependency/junit-4.12.jar;target/dependency/hamcrest-core-1.3.jar;"+path+"\"\n";
//        //gs+="java -cp \"evosuite-runtime-1.0.3.jar:evosuite-tests:junit-4.12.jar:hamcrest-core-1.3.jar:Lang/\""+path+" org.junit.runner.JUnitCore "+classname.replace('.','/').substring(0,classname.length()-".class".length()) +"\"_ESTest\"\n";
//            //gs+="java -cp \"evosuite-runtime-1.0.3.jar;evosuite-tests;target/dependency/junit-4.12.jar;target/dependency/hamcrest-core-1.3.jar;"+path+"\" org.junit.runner.JUnitCore "+classname.replace('.','/').substring(0,classname.length()-".class".length()) +"\"_ESTest\"\n";
//
//        /*TODO:
//        remove the .class
//
//        for linux:
//            change ; to :
//            \ to /
//            C:\ to /mnt/c/
//            add #!/bin/bash
//
//
//
//            check: does line 2 and 3 are even required???
//         */
//    }
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
                            if (!RemoveNoneBugException(error)) {
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

    private static boolean RemoveNoneBugException(String error) {
        return error.toLowerCase().contains("evosuite");
    }

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

    public static void exec(String command) {
        Process p = null;
        print("starting to execute: " + command);
        try {
            p = new ProcessBuilder("bash").start();
            PrintWriter stdin = new PrintWriter(p.getOutputStream());
            stdin.println(command);
            BufferedReader stdout =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stderr =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            print("streams started");
            String i = "";
            String j = "";
            stdin.close();

            while (i != null) {
                i = stdout.readLine();
                print(i);
            }

            while (j != null) {
                j = stderr.readLine();
                print(j);
            }
            print("streams ended");
            p.waitFor();
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
    }

    public static void exec(Collection<String> commandList) {
        for (String s : commandList) {
            exec(s);
        }
    }

    public static void exec(String[] list) {
        for (String s : list) {
            exec(s);
        }
    }

    public static void print(String s) {
        System.out.println(s);
    }
}
