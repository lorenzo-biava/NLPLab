package it.unito.nlplap.syntax.yoddish;

import java.util.Locale;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YoddishSample {

	private static final Logger LOG = LogManager.getLogger(YoddishSample.class);

	public static void main(String[] args) throws Exception {

		YoddishTranslator yoddishTranslator = new YoddishTranslator();

		Scanner scanner = null;
		try {
			scanner = new Scanner(System.in);
			int choice = 0;

			do {
				System.out
						.println("Select mode:\n\n1) Hard-coded sentences\n2) Input sentence");
				choice = scanner.nextInt();
				String text = "";
				if (choice == 1) {

					text += "Young Skywalker has become twisted by the Dark Side.";
					text += " "
							+ "The boy you trained, he is gone, consumed by Darth Vader.";
					// gone è letto come verbo e non come aggettivo

					text += " " + "There are always two, no more.";
					text += " " + "The mind of a child is truly wonderful.";
					text += " " + "You still have much to learn.";
					text += " "
							+ "When you reach nine hundred years old, you will not look as good.";
					text += " " + "The council does agree with you.";
					text += " " + "Skywalker will be your apprentice.";
					text += " " + "Master Obi-Wan has lost a planet.";
					text += " " + "The Clone Wars has begun.";
					text += " " + "There is no time to question.";
					text += " " + "Do not mourn them. Do not miss them.";
				} else {
					scanner.nextLine();					
					System.out.println("Input sentence: ");
					
					text = scanner.nextLine();
				}
				Locale language = Locale.ENGLISH;

				String yoddish = yoddishTranslator.toYoddish(text, language);

				LOG.info(String.format("Sentence '%s', in Yoddish is:\n%s",
						text, yoddish));

				System.out.println("Continue (1) or exit (2): ");
				choice = scanner.nextInt();
			} while (choice != 2);
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

}
