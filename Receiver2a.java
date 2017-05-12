/* Lorena Strechie s1419115 */

import java.net.*;
import java.io.*;

public class Receiver2a {

    public static final int MAX_PACKET_SIZE = 1027;

    private String fileName;
    private DatagramSocket socket;
    private int port;
    private InetAddress host;

    private boolean debug = true;

    public static void sendAck(int lastseqNum, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // Resend acknowledgement
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte)(lastseqNum >> 8);
        ackPacket[1] = (byte)(lastseqNum);
        DatagramPacket ackData = new  DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(ackData);
        System.out.println("Sent ack: Sequence Number = " + lastseqNum);
    }

    public Receiver2a(int port, String fileName) {
        this.port = port;
        this.fileName = fileName;
    }

    public void receiveData() throws IOException {

        System.out.println("The server was started...");
        try {

            // the maximum size of a buffer.
            byte[] data = new byte[MAX_PACKET_SIZE];

            // Open a datagram socket on the specified port number to listen for packets
            socket = new DatagramSocket(port);


            // Create a new file to store the received data in
            File file = new File(fileName);
            FileOutputStream outputFile = new FileOutputStream(file);


            // Create a flag to indicate the last message
            boolean eof = false;
            int num_bytes_received = 0;

            // Store sequence number
            int packet_number = 0;
            int last_packet_number = 0;

            // the received packet
            DatagramPacket receivedPacket = new DatagramPacket(data, data.length);

            // For each message we will receive
            while (!eof) {
                // receive a new packet
                socket.receive(receivedPacket);

                // get the data
                byte[] receivedData = receivedPacket.getData();

                // get the length of each packet
                int packetLength = receivedPacket.getLength();
                System.out.println("Data received from the packet: "+ packetLength);

                // Get port and address for sending ack
                try {
                    host = receivedPacket.getAddress();
                    port = receivedPacket.getPort();
                } catch (Exception e) {
                    System.out.print(e);
                }

                if (last_packet_number == 0)
                    sendAck(0, socket, host, port);

                // Retrieve sequence number
                packet_number = ((receivedData[0] & 0xff) << 8) + (receivedData[1] & 0xff);
                System.out.println("Packet Number Received: " + packet_number);

                // find the flag byte
                eof = (receivedData[2] > 0) ? true : false;

                // If we've already seen this packet before
                if (last_packet_number >= packet_number){
                    sendAck(packet_number, socket, host, port);

                    // If this is the next packet in sequence
                }else if (last_packet_number + 1 == packet_number){
                    int last_packet = (int) receivedData[2];
                    outputFile.write(receivedData, 3, packetLength - 3);
                    num_bytes_received += packetLength;

                    if (debug) {
                        System.out.println(
                                " Total Number of Bytes Received: " + num_bytes_received
                                        + "\n Packet Number: " + packet_number
                                        + "\n of length " + packetLength
                                        + "\n Bit Flag: " + last_packet
                                        + "\n Last Packet Number: " + last_packet_number
                                        + "\n ********************************");
                    }

                    sendAck(packet_number, socket, host, port);

                    // if this is the last packet
                    if (last_packet == 1) {
                        outputFile.close();
                        System.exit(1);
                    }

                    // move ahead one packet
                    last_packet_number = packet_number;
                }
            }

            socket.close();
            System.out.println("File " + fileName + " has been received.");
            System.exit(0);

        } catch (Exception e) {
            System.out.println(e);
        }
    }



    public static void main(String args[]) throws Exception {

        if(args.length == 2) { // we have the 2 valid arguments
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            Receiver2a receiver2a = new Receiver2a(port, fileName);
            receiver2a.receiveData();

        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            "java Receiver1b <Port> <Filename> [WindowSize]");
        }
    }
}