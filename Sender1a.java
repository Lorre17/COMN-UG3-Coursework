/* Lorena Strechie s1419115 */

import java.io.*;
import java.net.*;

public class Sender1a {

    public static final int MAX_PACKET_SIZE = 1027; //in bytes => 1KB + 3 bytes
    private int port;
    private String fileName;
    private DatagramSocket clientSocket = null;
    private InetAddress host;

    public Sender1a(String hostName, int port, String fileName){
        this.port = port;
        this.fileName = fileName;
        try {
            this.host = InetAddress.getByName(hostName);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static byte[] createHeader(byte[] data, int seqNum, int eof) {
        data[0] = (byte)((seqNum >> 8) & 0xff);
        System.out.println(" Data[0] =  " + data[0]);
        data[1] = (byte)(seqNum & 0xff);
        System.out.println(" Data[1] =  " + data[1]);
        System.out.println(" **************************");
        data[2] = (byte) eof;
        return data;
    }

    public void readAndSendData (){
        try {

            // read in the image to be sent
            FileInputStream inputFile = new FileInputStream(fileName);

            // create new client socket
            clientSocket = new DatagramSocket();

            int packetLength; // the length of the current packet
            short seqNum = 0; // we use a short, because it's two bytes
            byte eof = 0;
            int totalBytesSent = 0;

            long startTime = System.currentTimeMillis();

            while (inputFile.available() > 0) { // while there's still more to read
                int dataLeft = inputFile.available();

                // creating specific packet lengths
                if (dataLeft > MAX_PACKET_SIZE - 3) packetLength = MAX_PACKET_SIZE - 3;
                else {
                    // if we don't have a more than 1024 bytes left, we can make the packet smaller
                    packetLength = dataLeft;
                    eof = 1;
                }

                byte[] data = new byte[packetLength + 3]; // we make the array which will hold the packet
                inputFile.read(data, 3, packetLength); // we offset by three as we need a 3byte header
                totalBytesSent += packetLength; // we add the packet length to the current total bytes counter

                // Assigning values to the headers and put headers in packets
                data = createHeader(data, seqNum, eof);

                DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
                clientSocket.send(packet);

                if(eof == 0) {
                    System.out.println(
                            " Sequence number:  " + seqNum
                            + "\n Packet length:  " + packetLength
                            + "\n Data length:  " + data.length
                            + "\n Total bytes sent: " + totalBytesSent
                            + "\n **************************");

                } else {
                    System.out.println(
                            " Sequence number:  " + seqNum
                            + "\n Packet length:  " + packetLength
                            + "\n Data length:  " + data.length
                            + "\n Total bytes sent: " + totalBytesSent
                            + "\n **************************"
                            + "\n FILE SENT"
                    );
                    inputFile.close();
                    clientSocket.close();
                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;

                    System.out.println("\n THE CLIENT WAS CLOSED - ELAPSED TIME: " + elapsedTime/1000.0 + "s\n");
                    System.exit(1);
                }

                // Update data
                seqNum ^= 1 << 0;
                Thread.sleep(10);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]){
        if(args.length == 3){ // valid arguments, specify host

            String host = args[0];
            int port= Integer.parseInt(args[1]);
            String fileName = args[2];
            Sender1a sender1a = new Sender1a(host, port, fileName);
            sender1a.readAndSendData();

        } else if (args.length > 3) {
            try {
                int retryTimeout = Integer.parseInt(args[3]);
                int windowSize = Integer.parseInt(args[4]);
            } catch (Exception e) {
                System.err.println(e);
            }
        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            "Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
        }
    }
}