package edu.ucla.cs.jshrinkapp;

import java.io.*;
import java.util.*;

import edu.ucla.cs.jshrinklib.JShrink;
import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapser;
import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapserData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.methodinliner.InlineData;
import edu.ucla.cs.jshrinklib.reachability.*;

import org.apache.log4j.PropertyConfigurator;

public class Application {
	//I use this for testing to see if the correct methods have been removed
	/*package*/ static final Set<MethodData> removedMethods = new HashSet<MethodData>();

	//I use this for testing to see if the correct classes have been removed
	/*package*/ static final Set<String> removedClasses = new HashSet<String>();

	//I use this for testing to see if the correct methods have been inlined.
	/*package*/ static InlineData inlineData = null;

	//I use this for testing to see if the correct methods, etc, have been collapsed.
	/*package*/ static ClassCollapserData classCollapserData = null;

	//I use the following for testing to ensure the right kind of method wipe has been used
	/*package*/ static boolean removedMethod = false;
	/*package*/ static boolean wipedMethodBody = false;
	/*package*/ static boolean wipedMethodBodyWithExceptionNoMessage = false;
	/*package*/ static boolean wipedMethodBodyWithExceptionAndMessage = false;

	public static void main(String[] args) {

		//Re-initialise this each time Application is run (for testing)
		removedMethods.clear();
		removedClasses.clear();
		inlineData = null;
		classCollapserData = null;
		removedMethod = false;
		wipedMethodBody = false;
		wipedMethodBodyWithExceptionNoMessage = false;
		wipedMethodBodyWithExceptionAndMessage = false;

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


		EntryPointProcessor entryPointProcessor = new EntryPointProcessor(commandLineParser.includeMainEntryPoint(),
			commandLineParser.includePublicEntryPoints(),
			commandLineParser.includeTestEntryPoints(),
			true,
			commandLineParser.getCustomEntryPoints());

		// These can all be seen as TODOs for now.
		if (!commandLineParser.getMavenDirectory().isPresent()) {
			System.err.println("Sorry, we can only process Maven directories for now!");
			System.exit(1);
		}

		if(commandLineParser.removeClasses()){
			System.err.println("Sorry, we do not support the \"remove classes\" functionality for now!");
			System.exit(1);
		}

		if(!commandLineParser.getClassesToIgnore().isEmpty()){
			System.err.println("Sorry, we do not support the \"classes to ignore\" functionality for now!");
			System.exit(1);
		}


		JShrink jShrink = null;
		try {
			if(JShrink.instanceExists()){
				jShrink = JShrink.resetInstance(commandLineParser.getMavenDirectory().get(), entryPointProcessor,
					commandLineParser.getTamiflex(), commandLineParser.useSpark());
			} else {
				jShrink = JShrink.createInstance(commandLineParser.getMavenDirectory().get(), entryPointProcessor,
					commandLineParser.getTamiflex(), commandLineParser.useSpark());
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		assert (jShrink != null);

		if (commandLineParser.inlineMethods()) {
			inlineData = jShrink.inlineMethods(commandLineParser.isPruneAppInstance(), true);
		}

		if (commandLineParser.collapseClasses()) {
			classCollapserData = jShrink.collapseClasses(commandLineParser.isPruneAppInstance(), true);
		}


		Set<MethodData> methodsToRemove = new HashSet<MethodData>();
		methodsToRemove.addAll(jShrink.getAllLibMethods());
		methodsToRemove.removeAll(jShrink.getUsedLibMethods());
		if (commandLineParser.isPruneAppInstance()) {
			methodsToRemove.addAll(jShrink.getAllAppMethods());
			methodsToRemove.removeAll(jShrink.getUsedAppMethods());
		}

		if (commandLineParser.removeMethods()) {
			removedMethods.addAll(jShrink.removeMethods(methodsToRemove));
			removedMethod = true;
		} else if (commandLineParser.includeException()) {
			removedMethods.addAll(
				jShrink.wipeMethodAndAddException(methodsToRemove, commandLineParser.getExceptionMessage()));
			if(commandLineParser.getExceptionMessage().isPresent()){
				wipedMethodBodyWithExceptionAndMessage = true;
			} else {
				wipedMethodBodyWithExceptionNoMessage = true;
			}
		} else {
			removedMethods.addAll(jShrink.wipeMethods(methodsToRemove));
			wipedMethodBody = true;
		}

		jShrink.updateClassFiles();
	}
}