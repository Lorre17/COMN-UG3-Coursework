/* Lorena Strechie s1419115 */

import java.util.*;
import java.io.*;
import java.net.*;

class Sender2a {

    // the maximum packet size
    public static final int MAX_PACKET_SIZE = 1027;

    private int port;
    private String fileName;
    private int retryTimeout;
    private short windowSize;
    private InetAddress host;
    private int retransmissions = 0;

    // constructor
    public Sender2a(String host, int port, String fileName, int retryTimeout, short windowSize) {
        this.port = port;
        this.fileName = fileName;
        this.retryTimeout = retryTimeout;
        this.windowSize = windowSize;
        try {
            this.host = InetAddress.getByName(host);
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public static byte[] createHeader(byte[] data, int seqNum, boolean eof) {

        data[0] = (byte) ((seqNum >> 8) & 0xff);
        data[1] = (byte) (seqNum & 0xff);
        data[2] = (eof) ? (byte)(1) : (byte)(0);

        return data;
    }

    public void readAndSendData() throws IOException {
        System.out.println("The client has started sending the file...");

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(retryTimeout);
        File file = new File(fileName);

        InputStream inputFile = new FileInputStream(file);

        float fileSize = (float) inputFile.available()/ (float) 1024;
        long time1 = System.currentTimeMillis();

        // the length of the current packet
        int packet_length;


        byte[] sendData;
        int dataLeft; // data remaining at each loop

        int nextSeqNum = 1;

        int acknextSeqNum = -1;
        int base = 1;
        boolean eof = false;
        int totalBytesSent = 0; // Total number of bytes sent so far -- debugging purposes

        Vector <byte[]> sentMessageList = new Vector <byte[]>();


        // while there's still more packets to send
        while (inputFile.available() > 0) {
            dataLeft = inputFile.available();

            // If the amount we have remaining is greater than the packet size
            if (dataLeft > MAX_PACKET_SIZE - 3) {

                // the packet size is 1024 bytes
                packet_length = MAX_PACKET_SIZE - 3;

            } else {
                // if less than max size, we make the packet equal to the amount of data left < 1024 bytes
                packet_length = dataLeft;
                eof = true; // we signal the end of file
            }

            // to hold the packet
            byte[] data = new byte[packet_length + 3];

            // read data from the 3rd byte onwards byte 0,1,2 are header
            inputFile.read(data, 3, packet_length);

            totalBytesSent += packet_length;



            // Put headers in packet
            sendData = createHeader(data, nextSeqNum, eof);

            // Create Datagram packet
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, host, port);

            while (true) {

                if (base < (nextSeqNum - windowSize)) {
                    boolean ackPacketReceived;

                    while (true) {

                        byte[] ackData = new byte[2];
                        DatagramPacket ackpack = new DatagramPacket(ackData, ackData.length);

                        try {
                            socket.receive(ackpack);
                            socket.setSoTimeout(10);
                            acknextSeqNum = ((ackData[0] & 0xff) << 8) + (ackData[1] & 0xff);
                            ackPacketReceived = true;
                        } catch (SocketTimeoutException e) {
                            ackPacketReceived = false;
                        }

                        if (ackPacketReceived) {
                            if (acknextSeqNum >= (base + 1)) {
                                base = acknextSeqNum;
                            }
                            break;
                        } else {
                            for (int y = base-1; y < nextSeqNum-1; y++) {
                                byte[] resendMessage;
                                resendMessage = sentMessageList.get(y);

                                DatagramPacket resendPacket = new DatagramPacket(resendMessage, resendMessage.length, host, port);
                                socket.send(resendPacket);
                                retransmissions++;
                            }
                        }
                    }
                } else {
                    break;
                }
            }

            // Send packet to receiver
            try {
                socket.send(sendPacket);
                sentMessageList.add(sendData);
                nextSeqNum++;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IO/Error sending packet " + (base + windowSize) + ", nextseqnum: " + nextSeqNum + ".");
                socket.close();
                System.out.println("\n THE CLIENT WAS CLOSED \n");
                System.exit(0);
            }

           // Receiving ACK
            while (true) {
                boolean ackPacketReceived;
                byte[] ack = new byte[2];
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    socket.receive(ackpack);
                    socket.setSoTimeout(10);
                    acknextSeqNum = ((ack[0]  << 8) & 0xff) + (ack[1] & 0xff);
                    ackPacketReceived = true;
                } catch (SocketTimeoutException e) {
                    break;
                }

                if (ackPacketReceived) {
                    if (acknextSeqNum >= (base + 1)) {
                        base = acknextSeqNum;

                    }
                }
            }
        }

        socket.close();
        System.out.println("File " + fileName + " has been sent");

        long time2 = System.currentTimeMillis();
        double throughput =  fileSize / ((time2 - time1)/1000.0);
        System.out.println("File size: " + fileSize + "KB, Transfer time: " + ((time2 - time1)/1000) + " seconds. Throughput: " + throughput + "KBps. Retransmissions: " + retransmissions);
    }

    public static void main(String args[]) throws Exception {
        if (args.length == 5) { // valid arguments, specify host
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int timeout = Integer.parseInt(args[3]);
            short windowSize = (short) Integer.parseInt(args[4]);
            if (windowSize >= 32767) {
                System.out.println("Invalid window size!");
                System.exit(0);
            }
            Sender2a sender2a = new Sender2a(host, port, fileName, timeout, windowSize);
            sender2a.readAndSendData();

        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            " java Sender2a_LONG localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
        }
    }
}