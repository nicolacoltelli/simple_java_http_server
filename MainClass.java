import java.net.*;
import java.io.*;

public class MainClass {

	static final String data_directory_path = "./data/";
	static final int PORT = 6789; 

	static final String HTML_START =
		"<html>\n" +
		"<title>HTTP Server in java</title>\n" +
		"<body>\n";

	static final String HTML_END =
		"</body>\n" +
		"</html>\n";
	
	public static void main (String args[]) throws Exception {

		//Generate home page based on what content we have
		//	available inside the folder "./data/"
		StringBuffer response_buffer = new StringBuffer();
		response_buffer.append(HTML_START);
		response_buffer.append("<b>This is the HTTP Server Home Page. </b><BR>\n");
		response_buffer.append("<p>To directly access a resource use \"host:port/resource\".</p>\n");

		File[] files_list;
		File data_directory = new File(data_directory_path);

		if (data_directory.isDirectory()){

			response_buffer.append("<div>\n");
			response_buffer.append("\tBelow is a list containing links to all the available resources.\n");

			response_buffer.append("\t<ui>\n");
			files_list = data_directory.listFiles();
			for (File file: files_list){
				if (file.isFile()){
					response_buffer.append("\t\t<li>" + file.getName() +
						": <a href=\"./" + file.getName() + "\">" + file.getName() + "</a></li>\n");
				}
			}
			response_buffer.append("\t</ui>\n");

			response_buffer.append("</div>\n");

		}

		response_buffer.append(HTML_END);


		try (ServerSocket server = new ServerSocket(PORT);) {

			System.out.println(server.getLocalSocketAddress());
			System.out.println("Server Waiting for client on port " + PORT);

			while(true) {
				Socket client = server.accept();
				(new Thread (new RequestHandler(client, response_buffer))).start();
    		}

		}

	}

}