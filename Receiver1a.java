/* Lorena Strechie s1419115 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Receiver1a {

    public static final int MAX_PACKET_SIZE = 1027;

    private String fileName;
    private DatagramSocket serverSocket;
    private int port;

    private boolean debug = true;


    public Receiver1a(int port, String fileName) {
        this.port = port;
        this.fileName = fileName;
    }

    public void receiveData() {
        System.out.println("The server has started...");
        try {

            // the maximum size of a buffer.
            byte[] buffer = new byte[MAX_PACKET_SIZE];

            // Open a datagram socket on the specified port number to listen for packets
            serverSocket = new DatagramSocket(port);


            // Create a new file to store the received data in
            File file = new File(fileName);
            FileOutputStream outputFile = new FileOutputStream(file);

            // the received packet
            DatagramPacket packet =
                    new DatagramPacket(buffer,buffer.length);

            // a count of the number of bytes stored for debug
            int num_bytes_received = 0;

            // create variable for last packet receive
            boolean eof = false;
            int last_packet = 0;

            long startTime = System.currentTimeMillis();

            // until we've seen the byte flag
            while (!eof) {

                // receive a new packet
                serverSocket.receive(packet);
                // get the data
                byte[] data = packet.getData();
                // get the length of each packet (length of last package should be less than 1027)
                int packet_length = packet.getLength();

                // decode the headers in the packet data
                byte[] header = Arrays.copyOfRange(data, 0, 2);

                // identify the packet number
                short seq_number = ByteBuffer.wrap(header).getShort();

                // find the flag byte and change the bit for last packet
                eof = (data[2] > 0) ? true : false;
                last_packet = (eof == true) ? 1 : 0;

                // write the packet data to file.
                outputFile.write(data, 3, packet_length - 3);

                num_bytes_received += packet_length - 3;

                if (!eof) {
                    System.out.println(
                            " Received Sequence Number: " + seq_number
                            + "\n Received Packet Length: " + (packet_length - 3)
                            + "\n Received Data Length: " + data.length
                            + "\n EOF Flag:" + last_packet
                            + "\n Total Bytes Received: " + num_bytes_received
                            + "\n ********************************");
                } else {
                    System.out.println(
                            " Received Sequence Number: " + seq_number
                            + "\n Received Packet Length: " + (packet_length - 3)
                            + "\n Received Data Length: " + data.length
                            + "\n EOF Flag:" + last_packet
                            + "\n Total Bytes Received: " + num_bytes_received
                            + "\n ********************************"
                            + "\n FILE RECEIVED"
                    );
                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;
                    outputFile.close();
                    System.out.println("\n THE SERVER WAS CLOSED - ELAPSED TIME: " + elapsedTime/1000.0 + "s\n");
                    System.exit(1);

                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) throws Exception {

        if(args.length == 2) { // we have the 2 valid arguments
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            Receiver1a receiver1a = new Receiver1a(port, fileName);
            receiver1a.receiveData();

        } else if (args.length == 3) { // windowSize specified
            try {
                int windowSize = Integer.parseInt(args[2]);
            } catch (Exception e) {
                System.err.println(e);
            }
        } else{ // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            "java Receiver1a <Port> <Filename> [WindowSize]");
        }
    }
}