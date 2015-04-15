/* Daniel Shiffman               */
/* Programming from A to Z       */
/* Spring 2006                   */
/* http://www.shiffman.net       */
/* daniel.shiffman@nyu.edu       */

package a2z;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class A2ZFileWriter {
	private String filename;

	FileChannel outfc;

	public A2ZFileWriter(String name) throws FileNotFoundException {
		filename = name;
		// Create an output stream and file channel
		FileOutputStream fos = new FileOutputStream(filename);
		outfc = fos.getChannel();
	}

	public void append(String content) throws IOException {
		// Convert content String into ByteBuffer and write out to file
		ByteBuffer bb = ByteBuffer.wrap(content.getBytes());
		//outfc.position(0);
		outfc.write(bb);
	}
	
	public void close() throws IOException {
		outfc.close();
	}

	public void writeContent(String content) throws IOException {
		// Convert content String into ByteBuffer and write out to file
		ByteBuffer bb = ByteBuffer.wrap(content.getBytes());
		//outfc.position(0);
		outfc.write(bb);
		outfc.close();
	}	  
}
