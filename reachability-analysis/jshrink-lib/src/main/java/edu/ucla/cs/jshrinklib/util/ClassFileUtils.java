package edu.ucla.cs.jshrinklib.util;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

public class ClassFileUtils {

	public static final String ORIGINAL_FILE_POST_FIX="_original"; //package private as used by tests

	public static long getSize(File file) throws IOException{

		if(!file.exists()){
			throw new IOException("File '" + file.getAbsolutePath() + " does not exist");
		}

		long length=0;
		if(file.isDirectory()){
			for(File innerFile : file.listFiles()){
				length += getSize(innerFile);
			}
		} else {
			length += file.length();
		}

		return length;
	}

	public static Optional<File> getClassFile(SootClass sootClass, Collection<File> paths) {
		String classPath = sootClass.getName().replaceAll("\\.", File.separator) + ".class";

		for (File p : paths) {
			File test = new File(p + File.separator + classPath);
			if (test.exists()) {
				return Optional.of(test);
			}
		}
		return Optional.empty();
	}

	public static void decompressJar(File jarFile) throws IOException{

		ZipFile jarToReturn = null;
		try {
			jarToReturn = new ZipFile(jarFile);
		} catch (ZipException e) {
			throw new IOException("File '" + jarFile.getAbsolutePath() + "' is not a zipped file. " +
					"Are you sure it's a valid Jar?");
		}

		try {
			//Extract the jar file into a temporary directory
			File tempDir = File.createTempFile("jar_expansion", "tmp");
			tempDir.delete();
			if(!tempDir.mkdir()){
				throw new IOException("Could not 'mkdir " + tempDir.getAbsolutePath() + "'");
			}

			try {
				jarToReturn.extractAll(tempDir.getAbsolutePath());
			} catch(ZipException e){
				throw new IOException("Failed to extract .jar file. Following exception thrown:" +
						System.lineSeparator() + e.getLocalizedMessage());
			}

			jarToReturn.getFile().delete();
			FileUtils.moveDirectory(tempDir, jarToReturn.getFile());

		} catch(IOException e){
			throw new IOException("Failed to create a temporary directory. The following exception was thrown:"
					+ System.lineSeparator() + e.getLocalizedMessage());
		}
	}

	public static void compressJar(File file) throws IOException{
		try {
			ZipFile zipFile = new ZipFile(File.createTempFile("tmpJarFile", ".jar_tmp"));
			zipFile.getFile().delete();
			ZipParameters zipParameters = new ZipParameters();
			zipParameters.setCompressionLevel(9);

			//It's in a busy state otherwise... hope this is ok
			zipFile.getProgressMonitor().setState(ProgressMonitor.STATE_READY);

			boolean created=false;
			for(File f : file.listFiles()){
				if(f.isDirectory()){
					if(!created){
						zipFile.createZipFileFromFolder(f,zipParameters,false, 0);
						created=true;
					} else {
						zipFile.addFolder(f, zipParameters);
					}
				} else{
					if(!created){
						zipFile.createZipFile(f, zipParameters);
						created=true;
					} else {
						zipFile.addFile(f, zipParameters);
					}
				}
			}

			// Regular file.delete(), does not always work. I have to force it (I don't know why)
			FileUtils.forceDelete(file);
			FileUtils.moveFile(zipFile.getFile(), file);


		} catch(ZipException|IOException e){
			throw new IOException("Unable to create zip (Jar) file '" + file.getAbsolutePath() + "'" +
					" Following exception thrown:" + System.lineSeparator() + e.getLocalizedMessage());
		}
	}

	public static void removeClass(SootClass sootClass, Collection<File> classPath) throws IOException{
		Optional<File> fileToReturn = getClassFile(sootClass, classPath);

		if(!fileToReturn.isPresent()){
			throw new IOException("Cannot find file for class '" +  sootClass.getName() + "'");
		}

		assert(fileToReturn.isPresent());
		FileUtils.forceDelete(fileToReturn.get());
	}

	public static void writeClass(SootClass sootClass, Collection<File> classPath) throws IOException{

		Optional<File> fileToReturn = getClassFile(sootClass, classPath);

		if(!fileToReturn.isPresent()){
			throw new IOException("Cannot find file for class '" + sootClass.getName() + "'");
		}

		assert(fileToReturn.isPresent());

		//I don't fully understand why, but you need to retrieve the methods before writing to the fole
		for (SootMethod sootMethod : sootClass.getMethods()) {
			if(sootMethod.isConcrete()){
				sootMethod.retrieveActiveBody();
			}
		}

		OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileToReturn.get()));
		PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

		JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
		jasminClass.print(writerOut);
		writerOut.flush();
		streamOut.close();
	}

	public static Set<File> extractJars(List<File> classPaths) throws IOException{
		Set<File> decompressedJars = new HashSet<File>();
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
		return decompressedJars;
	}

	public static void compressJars(Set<File> decompressedJars) throws IOException {
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

	public static boolean directoryContains(File dir, File file){
		/* To find if a file is within a directory, we simply keep calling file.getParentFile(), until we find
		a parent directory equal to the directory. We will eventually get to a point where file.getParentFile() == null
		in the case where a file is not within a directory.
		*/

		if(file.getParentFile() == null){
			return false;
		}

		if(file.getParentFile().equals(dir)){
			return true;
		}

		return directoryContains(dir, file.getParentFile());
	}
}
