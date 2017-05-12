/* Lorena Strechie s1419115 */

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

public class Receiver2b {

    public static final int MAX_PACKET_SIZE = 1027;

    private String fileName;
    private DatagramSocket socket;
    private int port;
    private InetAddress host;
    private int windowSize;

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

    public Receiver2b(int port, String fileName, int windowSize) {
        this.port = port;
        this.fileName = fileName;
        this.windowSize = windowSize;
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
            int packet_wanted = 0;
            int last_packet_number = 0;

            // the received packet
            DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
            HashMap<Integer, byte[]> storageBuffer = new HashMap<Integer, byte[]>();

            // For each message we will receive
            while (!eof) {
                // receive a new packet
                socket.receive(receivedPacket);
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

                // Retrieve sequence number and find the flag byte
                packet_number = ((receivedData[0] & 0xff) << 8) + (receivedData[1] & 0xff);
                System.out.println("Packet Number Received: " + packet_number);
                eof = (receivedData[2] > 0) ? true : false;

                // If we've already seen this packet before
                if (last_packet_number >= packet_number){
                    sendAck(packet_number, socket, host, port);

                    // If this is the next packet in sequence
                } else if (last_packet_number + 1 == packet_number){
                    int last_packet = (int) receivedData[2];
                    outputFile.write(receivedData, 3, packetLength - 3);
                    num_bytes_received += packetLength;
                    packet_wanted = packet_number + 1;

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

                    while (storageBuffer.get(packet_wanted) != null) { // while the next packet in order is stored in buffer
                        outputFile.write(storageBuffer.get(packet_wanted)); // deliver packet
                        storageBuffer.remove(packet_wanted);

                        if (packet_wanted == last_packet) {
                            outputFile.close();
                            break;
                        } else {
                            packet_wanted++; // move onto next packet
                        }
                    }
                } else {
                    byte[] dataStripped = Arrays.copyOfRange(receivedData, 3, packetLength - 3); // strip HEADER
                    storageBuffer.put(packet_number, dataStripped); // store in HashMap

                    sendAck(packet_number, socket, host, port);
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

        if(args.length == 3) { // we have the 3 valid arguments
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            int windowSize = Integer.parseInt(args[2]);
            Receiver2b receiver2b = new Receiver2b(port, fileName, windowSize);
            receiver2b.receiveData();

        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            "java Receiver1b <Port> <Filename> [WindowSize]");
        }
    }
}