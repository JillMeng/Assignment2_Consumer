
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Consumer {

    private final static String QUEUE_NAME = "QUEUE";

    public static void main(String[] argv) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
        factory.setUsername("radmin");
        factory.setPassword("jm");
        factory.setHost("ec2-52-27-220-226.us-west-2.compute.amazonaws.com");
        factory.setPort(5672);
        final Connection connection = factory.newConnection();

        //hashmap to store jsonObject
        ConcurrentHashMap<Integer, List<JSONObject>> hashMap = new ConcurrentHashMap<>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {

                    final Channel channel = connection.createChannel();
                    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    // max one message per receiver
                    channel.basicQos(1);
                    System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        JSONObject jsonObject = new JSONObject(message);
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        //retrieve LiftID and store in a hashmap
                        int liftID = jsonObject.getInt("liftID");
                        hashMap.computeIfAbsent(liftID,n -> Collections.synchronizedList(new ArrayList())).add(jsonObject);
                        System.out.println( "Callback thread ID = " + Thread.currentThread().getId() + " Received '" + jsonObject + "'");
                    };
                    // process messages
                    channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
                } catch (IOException ex) {
                    Logger.getLogger(Consumer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        // start threads and block to receive messages
        for(int i=0; i<100; i++) {
            Thread recv =new Thread(runnable);
            recv.start();
        }
    }
}
