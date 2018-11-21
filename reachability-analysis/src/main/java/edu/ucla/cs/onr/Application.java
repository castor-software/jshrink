package edu.ucla.cs.onr;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarFile;

import edu.ucla.cs.onr.reachability.*;
import edu.ucla.cs.onr.util.SootUtils;
import edu.ucla.cs.onr.methodwiper.MethodWiper;
import edu.ucla.cs.onr.util.ClassFileUtils;

import org.apache.log4j.PropertyConfigurator;
import soot.*;

// TODO: We rely on the output of this application when in "--verbose" mode. This is currently a bit of a mess
// ,I suggest we use a logger to manage this better

public class Application {

	private static boolean DEBUG_MODE = true; //Enabled by default, needed for testing
	private static boolean VERBOSE_MODE = false;
	private static Set<File> decompressedJars = new HashSet<File>();

	//I use this for testing, to see if the correct methods have been removed
	/*package*/ static Set<MethodData> removedMethods = new HashSet<MethodData>();

	//I use this for testing, to see if the correct classes have been removed
	/*package*/ static Set<String> removedClasses = new HashSet<String>();

	public static boolean isDebugMode() {
		return DEBUG_MODE;
	}

	public static boolean isVerboseMode(){
		return VERBOSE_MODE;
	}

	public static void main(String[] args) {

		//Re-initialise this each time Application is run (for testing)
		removedMethods.clear();
		decompressedJars.clear();
		removedClasses.clear();

		//I just put this in to stop an error
		PropertyConfigurator.configure(
			Application.class.getClassLoader().getResourceAsStream("log4j.properties"));

		//Load the command line arguments
		ApplicationCommandLineParser commandLineParser = null;

		try {
			commandLineParser = new ApplicationCommandLineParser(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		assert (commandLineParser != null);

		DEBUG_MODE = commandLineParser.isDebug();
		VERBOSE_MODE = commandLineParser.isVerbose();

		IProjectAnalyser projectAnalyser = null;

		EntryPointProcessor entryPointProcessor = new EntryPointProcessor(commandLineParser.includeMainEntryPoint(),
				commandLineParser.includePublicEntryPoints(), commandLineParser.includeTestEntryPoints(),
				commandLineParser.getCustomEntryPoints());

		if(commandLineParser.getMavenDirectory().isPresent()){
			projectAnalyser =  new MavenSingleProjectAnalyzer(
					commandLineParser.getMavenDirectory().get().getAbsolutePath(), entryPointProcessor);
		} else {
			projectAnalyser =  new CallGraphAnalysis(commandLineParser.getLibClassPath(),
					commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath(), entryPointProcessor);
		}

		assert(projectAnalyser != null);

		projectAnalyser.setup();

		try {

			if(Application.isVerboseMode()){
				for(File file : projectAnalyser.getAppClasspaths()) {
					System.out.println("app_size_before_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}
				for(File file: projectAnalyser.getLibClasspaths()){
					System.out.println("lib_size_before_debloat_" + file.getAbsolutePath() + ","
						+ClassFileUtils.getSize(file));
				}
			}

			extractJars(projectAnalyser.getAppClasspaths());
			extractJars(projectAnalyser.getLibClasspaths());
			extractJars(projectAnalyser.getTestClasspaths());

			if(Application.isVerboseMode()){
				for(File file : projectAnalyser.getAppClasspaths()){
					System.out.println("app_size_decompressed_before_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}

				for(File file : projectAnalyser.getLibClasspaths()){
					System.out.println("lib_size_decompressed_before_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}
			}

			if(commandLineParser.removeMethods() && !commandLineParser.methodsToRemove().isEmpty()){
				/*
				Our application has two modes. In this mode, the methods to be removed are specified at the command-line
				level. The call graph analysis is not run, and the methods are directly wiped.
				 */
				Set<File> classPathsOfConcern = new HashSet<File>();
				classPathsOfConcern.addAll(projectAnalyser.getAppClasspaths());
				classPathsOfConcern.addAll(projectAnalyser.getLibClasspaths());
				classPathsOfConcern.addAll(projectAnalyser.getTestClasspaths());

				SootUtils.setup_trimming(projectAnalyser.getLibClasspaths(),
						projectAnalyser.getAppClasspaths(),projectAnalyser.getTestClasspaths());
				Scene.v().loadNecessaryClasses();

				Set<SootClass> classesToRewrite = new HashSet<SootClass>();



				for(MethodData methodData : commandLineParser.methodsToRemove()){
					SootClass sootClass = Scene.v().loadClassAndSupport(methodData.getClassName());

					if(!sootClass.isEnum() && sootClass.declaresMethod(methodData.getSubSignature())) {
						SootMethod sootMethod = sootClass.getMethod(methodData.getSubSignature());
						if (MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod,
							Application.getExceptionMessage())) {
							Application.removedMethods.add(methodData);
							classesToRewrite.add(sootClass);
						}
					}
				}

				modifyClasses(classesToRewrite, classPathsOfConcern);

			} else {
				/*
				In this mode, the call-graph analysis is run to determine what methods are touched and which are untouched.
				*/
				projectAnalyser.run();

				G.reset();
				SootUtils.setup_trimming(projectAnalyser.getLibClasspaths(),
						projectAnalyser.getAppClasspaths(),projectAnalyser.getTestClasspaths());
				Scene.v().loadNecessaryClasses();


				if(Application.isVerboseMode()) {

					System.out.println("number_lib_classes," + projectAnalyser.getLibClasses().size());
					System.out.println("number_lib_methods," + projectAnalyser.getLibMethods().size());
					System.out.println("number_app_classes," + projectAnalyser.getAppClasses().size());
					System.out.println("number_app_methods," + projectAnalyser.getAppMethods().size());
					System.out.println("number_used_lib_classes," + projectAnalyser.getUsedLibClasses().size());
					System.out.println("number_used_lib_methods," + projectAnalyser.getUsedLibMethods().size());
					System.out.println("number_used_app_classes," + projectAnalyser.getUsedLibClasses().size());
					System.out.println("number_used_app_methods," + projectAnalyser.getUsedAppMethods().size());

					for(MethodData entrypoint : projectAnalyser.getEntryPoints()){
						System.out.println("entry_point," + entrypoint.getSignature());
					}

					/* Removed these as I don't need them and they are a bit spammy
					for (MethodData method : projectAnalyser.getLibMethods()) {
						System.out.println("lib_method," + method.toString());
					}

					for (MethodData method : projectAnalyser.getAppMethods()) {
						System.out.println("app_method," + method.toString());
					}

					for (MethodData method : projectAnalyser.getUsedLibMethods()) {
						System.out.println("lib_method_touched," + method.toString());
					}

					for (MethodData method : projectAnalyser.getUsedAppMethods()) {
						System.out.println("app_method_touched," + method.toString());
					}
					*/
				}

				Set<MethodData> libMethodsRemoved = new HashSet<MethodData>();
				Set<MethodData> appMethodsRemoved = new HashSet<MethodData>();

				if(commandLineParser.removeMethods()) {
					Set<MethodData> methodsToRemove = new HashSet<MethodData>();

					Set<SootClass> classesToRewrite = new HashSet<SootClass>(); //Take note of all classes that have changed
					Set<File> classPathsOfConcern = new HashSet<File>(); //The classpaths where these classes can be found

					//Remove the unused library methods and classes
					classPathsOfConcern.addAll(projectAnalyser.getLibClasspaths());
					Set<MethodData> libMethodsToRemove = new HashSet<MethodData>();
					libMethodsToRemove.addAll(projectAnalyser.getLibMethods());
					libMethodsToRemove.removeAll(projectAnalyser.getUsedLibMethods());

					methodsToRemove.addAll(libMethodsToRemove);

					//Remove the unused app methods (if applicable)
					if (commandLineParser.isPruneAppInstance()) {
						classPathsOfConcern.addAll(projectAnalyser.getAppClasspaths());
						Set<MethodData> appMethodToRemove = new HashSet<MethodData>();
						appMethodToRemove.addAll(projectAnalyser.getAppMethods());
						appMethodToRemove.removeAll(projectAnalyser.getUsedAppMethods());

						methodsToRemove.addAll(appMethodToRemove);
					}

					//Remove any classes in which all the methods are removed
					Map<SootClass, Set<MethodData>> classIntCount = new HashMap<SootClass, Set<MethodData>>();
					for(MethodData method : methodsToRemove){
						SootClass sootClass = Scene.v().loadClassAndSupport(method.getClassName());
						if(!classIntCount.containsKey(sootClass)){
							Set<MethodData> methods = new HashSet<MethodData>();
							for(SootMethod sootMethod : sootClass.getMethods()){
								methods.add(SootUtils.sootMethodToMethodData(sootMethod));
							}
							classIntCount.put(sootClass, methods);
						}
						classIntCount.get(sootClass).remove(method);
					}

					Set<SootClass> classesToRemove = new HashSet<SootClass>();
					for(Map.Entry<SootClass, Set<MethodData>> entry : classIntCount.entrySet()){
						if(entry.getValue().isEmpty()){
							classesToRemove.add(entry.getKey());
							Set<MethodData> methods = new HashSet<MethodData>();
							for(SootMethod sootMethod : entry.getKey().getMethods()){
								methods.add(SootUtils.sootMethodToMethodData(sootMethod));
							}
							//If we remove the class we obviously remove the method
							Application.removedMethods.addAll(methods);
							Application.removedClasses.add(entry.getKey().getName());
							methodsToRemove.removeAll(methods);
						}
					}


					for (MethodData method : methodsToRemove) {
						SootClass sootClass = Scene.v().loadClassAndSupport(method.getClassName());
						if (!sootClass.isEnum() && sootClass.declaresMethod(method.getSubSignature())) {
							SootMethod sootMethod = sootClass.getMethod(method.getSubSignature());
							if (MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod,
									getExceptionMessage())) {
								Application.removedMethods.add(SootUtils.sootMethodToMethodData(sootMethod));
								classesToRewrite.add(sootClass);
								appMethodsRemoved.add(method);
							}
						}
					}

					removeClasses(classesToRemove,classPathsOfConcern);
					modifyClasses(classesToRewrite,classPathsOfConcern);
				}

				if(Application.isVerboseMode()) {
					System.out.println("number_lib_methods_removed," + libMethodsRemoved.size());
					System.out.println("number_app_methods_removed," + appMethodsRemoved.size());
				}
			}

			if(Application.isVerboseMode()){
				for(File file : projectAnalyser.getAppClasspaths()){
					System.out.println("app_size_decompressed_after_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}

				for(File file : projectAnalyser.getLibClasspaths()){
					System.out.println("lib_size_decompressed_after_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				compressJars();
				if(Application.isVerboseMode()){
					for(File file : projectAnalyser.getAppClasspaths()){
						System.out.println("app_size_after_debloat_" + file.getAbsolutePath() + ","
								+ ClassFileUtils.getSize(file));
					}

					for(File file : projectAnalyser.getLibClasspaths()){
						System.out.println("lib_size_after_debloat_" + file.getAbsolutePath() + ","
								+ ClassFileUtils.getSize(file));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static String getExceptionMessage() {
		return "Method has been removed";
	}

	private static void modifyClasses(Set<SootClass> classesToRewrite, Set<File> classPaths){
		for (SootClass sootClass : classesToRewrite) {
			try {
				ClassFileUtils.writeClass(sootClass, classPaths);
			} catch (IOException e) {
				System.err.println("An exception was thrown when attempting to rewrite a class:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static void removeClasses(Set<SootClass> classesToRemove, Set<File> classPaths){
		for(SootClass sootClass : classesToRemove){
			try{
				ClassFileUtils.removeClass(sootClass, classPaths);
			} catch (IOException e){
				System.err.println("An exception was thrown when attempting to delete a class:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static void extractJars(List<File> classPaths) throws IOException{
		for(File file : new HashSet<File>(classPaths)){
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(file);
			} catch (IOException e) {
				continue;
			}

			assert(jarFile != null);

			ClassFileUtils.decompressJar(file);
			decompressedJars.add(file);
		}
	}

	private static void compressJars() throws IOException {
		for(File file : decompressedJars){
			if(!file.exists()){
				System.out.println("File '" + file.getAbsolutePath() + "' does not exist");
			} else if(!file.isDirectory()){
				System.out.println("File '" + file.getAbsolutePath() + "' is not a directory");
			}
			assert(file.exists() && file.isDirectory());
			ClassFileUtils.compressJar(file);
		}
	}
}
