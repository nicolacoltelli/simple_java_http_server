import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;

public class RequestHandler implements Runnable {

	static final boolean DEBUG = true; 

	static final String data_directory_path = "./data/";

	//Default timeout of 120 seconds (standard)
	//	or 10 seconds (testing) after which we
	//	will close the connection to the client
	//static final int timeout = 120000;
	static final int timeout = 10000;

	//Length of the interval at which we will check
	//	if the client has sent a reqeust
	static final int timeout_interval = 500;

	Socket connected_client = null;
	StringBuffer response_buffer = null;

	public RequestHandler(Socket client, StringBuffer response_buffer) {
		connected_client = client;
		this.response_buffer = response_buffer;
	}

	public void run() {

		try ( BufferedReader input_from_client = new BufferedReader(new InputStreamReader (connected_client.getInputStream()));
				DataOutputStream output_to_client = new DataOutputStream(connected_client.getOutputStream());
			)
			{

			System.out.println( "Client "+
				connected_client.getInetAddress() + ":" + connected_client.getPort() + " is connected");

			//Keeps track of the number of request this Socket receives
			int requests_count = -1;

			alive_loop: do {

				++requests_count;

				//This is as default. Client can opt out.
				connected_client.setKeepAlive(true);

				//Implementing timeout.
				int count_timeouts = 0;
				while ( !input_from_client.ready() ){

					Thread.sleep(timeout_interval);
					++count_timeouts;

					if (count_timeouts>timeout/timeout_interval){
						if (DEBUG) System.out.println( "Client "+ connected_client.getInetAddress() +
						":" + connected_client.getPort() + " has timed out");
						break alive_loop;
					}

				}

				if (requests_count > 0){
					if (DEBUG) System.out.println( "Connection with client "+ connected_client.getInetAddress() +
						":" + connected_client.getPort() + " has been kept alive");
				}

				if (DEBUG) System.out.println("The HTTP request string is ....");

				String header_line = input_from_client.readLine();
				if (DEBUG) System.out.println(header_line);
				StringTokenizer tokenizer = new StringTokenizer(header_line);
				String http_method = tokenizer.nextToken();
				String http_query_string = tokenizer.nextToken();
				
				while (input_from_client.ready()) {

					String request_string = input_from_client.readLine();
					if (DEBUG) System.out.println(request_string);

					if (request_string.equals("Connection: close")){
						System.out.println( "Client "+ connected_client.getInetAddress() +
							":" + connected_client.getPort() + " decided not to use keep alive");
						connected_client.setKeepAlive(false);
					}

				}


				if (http_method.equals("GET")) {

					if (http_query_string.equals("/")) {
						// The default home page
						sendResponse(200, response_buffer.toString(), false, output_to_client);
					} else {
						//This is interpreted as a file name
						String filename = http_query_string.replaceFirst("/", "");
						filename = URLDecoder.decode(filename, "UTF-8");
						if (new File(data_directory_path + filename).isFile()){
							sendResponse(200, data_directory_path + filename, true, output_to_client);
						} else {
							sendResponse(404, "<b>The Requested resource was not found ...." +
								"Usage: http://" + InetAddress.getLocalHost().getHostAddress() + ":6789/</b>", false, output_to_client);
						}
					}

				} else {
					sendResponse(404, "<b>The Requested resource not found ...." +
						"Usage: http://" + InetAddress.getLocalHost().getHostAddress() + ":6789/</b>", false, output_to_client);
				}

			} while (connected_client.getKeepAlive());

			System.out.println( "Client "+
				connected_client.getInetAddress() + ":" + connected_client.getPort() + " disconnected");

			connected_client.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendResponse (int status_code, String response_string, boolean is_file, DataOutputStream output_to_client) {

		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer" + "\r\n";
		String contentLengthLine = null;
		String filename = is_file ? response_string : null;
		String contentTypeLine = "Content-Type: text/html" + "\r\n";

		try (BufferedInputStream input_file = (is_file ? new BufferedInputStream(new FileInputStream(filename)) : null)){
		
			if (status_code == 200){
				statusLine = "HTTP/1.1 200 OK" + "\r\n";
			} else{
				statusLine = "HTTP/1.1 404 Not Found" + "\r\n";
			}

			//is_file == true => status_code == 200
			if (is_file) {
				
				contentLengthLine = "Content-Length: " + Integer.toString(input_file.available()) + "\r\n";
				
				if (!filename.endsWith(".htm") && !filename.endsWith(".html")){
					int extension_index = filename.lastIndexOf('.');
					if (extension_index >= 0 && extension_index < filename.length() - 1){
						//String extension = filename.substring(extension_index + 1);
						String file_type = Files.probeContentType(Paths.get(filename));
						contentTypeLine = "Content-Type: " + file_type + "\r\n";
					} else {
						contentTypeLine = "Content-Type: " + "\r\n";
					}
				}

			} else {
				contentLengthLine = "Content-Length: " + response_string.length() + "\r\n";
			}

				output_to_client.writeBytes(statusLine);
				output_to_client.writeBytes(serverdetails);
				output_to_client.writeBytes(contentTypeLine);
				output_to_client.writeBytes(contentLengthLine);
				//The following must be uncommented if we don't
				//	want to keepalive the connection.
				//output_to_client.writeBytes("Connection: close\r\n");
				output_to_client.writeBytes("\r\n");

			if (is_file){
				sendFile(input_file, output_to_client);
			} else {
				output_to_client.writeBytes(response_string);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException f) {
			f.printStackTrace();
		}

	}

	public void sendFile (BufferedInputStream input_file, DataOutputStream out) {
		
		byte[] buffer = new byte[1024] ;
	
		int bytes_read;
		try{
			while ((bytes_read = input_file.read(buffer)) != -1 ) {
				out.write(buffer, 0, bytes_read);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}

}