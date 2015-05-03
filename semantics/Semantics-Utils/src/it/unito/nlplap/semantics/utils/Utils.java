package it.unito.nlplap.semantics.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Utils {

	public static String fileToText(String path) throws FileNotFoundException {
		return fileToText(new File(path));
	}

	public static String fileToText(File file) throws FileNotFoundException {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			StringBuilder textBuilder = new StringBuilder();
			while (sc.hasNextLine())
				textBuilder.append(sc.nextLine());
			return textBuilder.toString();
		} finally {
			sc.close();
		}
	}

}
