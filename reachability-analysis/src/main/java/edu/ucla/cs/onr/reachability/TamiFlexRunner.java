package edu.ucla.cs.onr.reachability;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.ucla.cs.onr.Application;

public class TamiFlexRunner {
	private String tamiflex_path;
	private String project_path;
	private boolean rerun;
	
	// classes that are referenced or instantiated via Java reflection
	public HashSet<String> accessed_classes;
	// fields that are referenced or accessed via Java reflection
	public HashSet<String> accessed_fields;
	// methods that are referenced or invoked via Java reflection
	public HashSet<String> used_methods;
	
	public TamiFlexRunner(String tamiflexJarPath, String mavenProjectPath, boolean rerunTamiFlex) {
		this.tamiflex_path = tamiflexJarPath;
		this.project_path = mavenProjectPath;
		this.rerun = rerunTamiFlex;
		accessed_classes = new HashSet<String>();
		accessed_fields = new HashSet<String>();
		used_methods = new HashSet<String>();
	}
	
	public void run() throws IOException {
		File pom_file = new File(project_path + File.separator + "pom.xml");
		if(pom_file.exists()) {
			File tamiflex_output = new File(project_path + File.separator + "out");
			if(!tamiflex_output.exists()) {
				runTamiFlex(pom_file);
			} else if (rerun) {
				// delete the previous output and rerun TamiFlex
				tamiflex_output.delete();
				runTamiFlex(pom_file);
			}
			
			// analyze the result
			if(tamiflex_output.exists()) {
				String log = tamiflex_output.getAbsolutePath() + File.separator + "refl.log";
				analyze(log);
			} else {
				System.err.println("Error: TamiFlex does not run successfully. No output folder exists.");
			}
		}
	}
	
	public void runTamiFlex(File pom_file) throws IOException {
		// save a copy of the pom file
		File copy = new File(pom_file.getAbsolutePath() + ".tmp");
		FileUtils.copyFile(pom_file, copy);
		
		// double check if the tamiflex jar exists
		File tamiflex_jar = new File(tamiflex_path);
		if(tamiflex_jar.exists()) {
			// update the tamiflex jar path with the absolute path
			// because 'mvn test' is run in the root directory of the given project
			// a relative path will not work
			this.tamiflex_path = tamiflex_jar.getAbsolutePath();
		} else {
			System.err.println("Error: the TamiFlex jar does not exist in " + tamiflex_path);
		}
		
		// inject TamiFlex as the java agent in the POM file
		injectTamiFlex(pom_file.getAbsolutePath());
		
		try {
			// run 'mvn test'
			boolean testResult = runMavenTest();
			if(Application.isDebugMode() || Application.isVerboseMode()) {
				if(testResult) {
					System.out.println("mvn test succeeds.");
				} else {
					System.out.println("mvn test fails.");
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			// restore the pom file
			FileUtils.copyFile(copy, pom_file);
			copy.delete();
		}
	}
	
	public void analyze(String log) {
		File tamiflex_log = new File(log);
		if(tamiflex_log.exists()) {
			try {
				List<String> lines = FileUtils.readLines(tamiflex_log, Charset.defaultCharset());

				for(String line : lines) {
					String[] ss = line.split(";");
					String reference = ss[1];
					if(reference.startsWith("[L")) {
						// sometimes it starts with [L, seems like a formating issue in TamiFlex
						reference = reference.substring(2);
					}
					
					if(reference.endsWith("[]")) {
						// this is an array type
						String base_type = reference.substring(0, reference.length() - 2);
						if(!isPrimitiveType(base_type)) {
							accessed_classes.add(base_type);
						}
					} else if (reference.startsWith("<") && reference.endsWith(">")) {
						// this is either a field or a method 
						String class_name = reference.split(": ")[0];
						String class_member = reference.split(": ")[1];
						if(class_member.contains("(") && class_member.contains(")")) {
							// this is a method in the format of "return_type method_subsignature"
							String method_subsignature = class_member.split(" ")[1];
							used_methods.add(class_name + "." + method_subsignature);
						} else {
							// this is a field in the format of "field_type field_name"
							String field_name = class_member.split(" ")[1];
							accessed_fields.add(class_name + "." + field_name);
						}
					} else {
						// this is a class type
						accessed_classes.add(reference);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Error: There is no TamiFlex log file - " + log);
		}
	}
	
	private boolean isPrimitiveType(String t) {
		if(t.equals("boolean") || t.equals("byte") || t.equals("char") || t.equals("short")
				|| t.equals("int") || t.equals("long") || t.equals("float") || t.equals("double")) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean runMavenTest() throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec("mvn test", null, new File(project_path));
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		boolean testResult = false;
		String output = null;
		while ((output = stdInput.readLine()) != null) {
			if(output.contains("BUILD SUCCESS")) {
				testResult = true;
			} else if (output.contains("BUILD FAILURE")) {
				testResult = false;
			}
		}
		p.waitFor();
		
		return testResult;
	}
	
	/**
	 * 
	 * Inject TamiFlex as a java agent in the test plugin in the given POM file
	 * 
	 * @param path
	 */
	public void injectTamiFlex(String path) {
		File pom_file = new File(path);
		try {
			// parse the pom file as xml
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pom_file);
			doc.getDocumentElement().normalize();
			
			// build the xpath to locate the artifact id
			// note that the parent node also contains a artifactId node
			// so we need to specify that the artifactId node we are looking 
			// for is under the project node
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile("/project/build/plugins/plugin/artifactId[text()=\"maven-surefire-plugin\"]");
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if(nodes.getLength() == 0) {
            	// This POM does not declare the sunfire plugin
            	// We must declare one explicitly together with the java agent argument
            	Node plugin_node = doc.createElement("plugin");
            	
            	Node groupId_node = doc.createElement("groupId");
            	groupId_node.setTextContent("org.apache.maven.plugins");
            	
            	Node artifactId_node = doc.createElement("artifactId");
            	artifactId_node.setTextContent("maven-surefire-plugin");
            	
            	Node version_node = doc.createElement("version");
            	version_node.setTextContent("2.20.1");
            	
            	Node config_node = doc.createElement("configuration");
        		Node arg_node = doc.createElement("argLine");
        		arg_node.setTextContent("-javaagent:" + tamiflex_path);
        		config_node.appendChild(arg_node);
        		
        		plugin_node.appendChild(groupId_node);
        		plugin_node.appendChild(artifactId_node);
        		plugin_node.appendChild(version_node);
        		plugin_node.appendChild(config_node);
        		
        		XPathExpression expr2 = xpath.compile("/project/build/plugins");
        		NodeList nodes2 = (NodeList) expr2.evaluate(doc, XPathConstants.NODESET);
        		if(nodes2.getLength() == 0) {
        			// no plugins node, must inject one
        			Node plugins_node = doc.createElement("plugins");
        			plugins_node.appendChild(plugin_node);
        			
        			XPathExpression expr3 = xpath.compile("/project/build");
        			NodeList nodes3 = (NodeList) expr3.evaluate(doc, XPathConstants.NODESET);
        			if(nodes3.getLength() == 0) {
        				// no build node, must inject one
        				Node build_node = doc.createElement("plugins");
        				build_node.appendChild(plugins_node);
        				
        				XPathExpression expr4 = xpath.compile("/project");
        				NodeList nodes4 = (NodeList) expr4.evaluate(doc, XPathConstants.NODESET);
        				if(nodes4.getLength() == 1) {
        					Node project_node = nodes4.item(0);
        					project_node.appendChild(build_node);
        				} else {
        					System.err.println("There are zero or multiple project nodes in the POM file. "
        							+ "Please double check if it is correct.");
        				}
        			} else if (nodes3.getLength() == 1){
        				// found the build node
        				Node build_node = nodes3.item(0);
        				build_node.appendChild(plugins_node);
        			} else {
        				System.err.println("There are multiple build nodes within the project node in the POM file. "
    							+ "Please double check if it is correct.");
        			}
        		} else if (nodes2.getLength() == 1) {
        			// found the plugins node
        			Node plugins_node = nodes2.item(0);
        			plugins_node.appendChild(plugin_node);
        		} else {
        			System.err.println("There are multiple plugins nodes within the build node in the POM file. "
							+ "Please double check if the POM file is correct.");
        		}
            } else if (nodes.getLength() == 1) {
            	// This POM declares the sunfire plugin explicitly
            	// We only need to inject the java agent argument
            	Node plugin_node = nodes.item(0).getParentNode();
            	NodeList children = plugin_node.getChildNodes();
            	Node config_node = null;
            	for(int i = 0; i < children.getLength(); i++) {
            		Node child = children.item(i);
            		if(child.getNodeName().equals("configuration")) {
            			// configuration node exists
            			config_node = child;
            			break;
            		}
            	}
            	
            	if(config_node != null) {
            		NodeList config_children = config_node.getChildNodes();
        			Node arg_node = null;
        			for(int j = 0; j < config_children.getLength(); j++) {
        				Node config_child = config_children.item(j);
        				if(config_child.getNodeName().equals("argLine")) {
        					arg_node = config_child;
        					break;
        				}
        			}
        			
        			if(arg_node == null) {
        				// no argLine option, insert it
        				arg_node = doc.createElement("argLine");
        				arg_node.setTextContent("-javaagent:" + tamiflex_path);
        				config_node.appendChild(arg_node);
        			} else {
        				// already have argLine option, append the java agent after the existing options
        				String arg_option = arg_node.getTextContent();
        				// ignore the case where tamiflex has been added as a java agent
        				if(!arg_option.contains(tamiflex_path)) {
        					if(arg_option.isEmpty()) {
        						// add the java agent option directly
        						arg_node.setTextContent("-javaagent:" + tamiflex_path);
        					} else {
        						// append the new java agent option
        						arg_option += " -javaagent:" + tamiflex_path;
        						arg_node.setTextContent(arg_option);
        					}
        				}
        			}
            	} else {
            		// no configuration node, insert it
            		config_node = doc.createElement("configuration");
            		Element arg_node = doc.createElement("argLine");
            		arg_node.setTextContent("-javaagent:" + tamiflex_path);
            		config_node.appendChild(arg_node);
            		plugin_node.appendChild(config_node);
            	}
            } else {
            	// is it possible to have two sunfire plugins？
            	System.err.println("There are more than one sunfire plugin in the POM file. "
            			+ "Please double check if it is correct.");
            }
            
            // rewrite the POM file
    		TransformerFactory transformerFactory = TransformerFactory.newInstance();
    		Transformer transformer = transformerFactory.newTransformer();
    		DOMSource source = new DOMSource(doc);
    		StreamResult result = new StreamResult(pom_file);
    		transformer.transform(source, result);
		} catch (SAXException | ParserConfigurationException 
				| IOException | XPathExpressionException | TransformerException e) {
			e.printStackTrace();
		}
	}
}
