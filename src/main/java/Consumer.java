
import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;
import org.json.JSONObject;
import redis.clients.jedis.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Consumer {

    private static final String EXCHANGE_NAME = "messages";
    private final static String QUEUE_NAME = "Skier_Queue";

    public static void main(String[] argv) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
//        factory.setUsername("radmin");
//        factory.setPassword("radmin");
//        factory.setHost("ec2-54-186-85-234.us-west-2.compute.amazonaws.com");
//        factory.setPort(5672);
        Connection connection = factory.newConnection();

        //connect to Jedis
//        JedisPool pool = new JedisPool("35.165.251.248", 6378);
        JedisPool pool = new JedisPool("localhost", 6379);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {

                    Channel channel = connection.createChannel();
                    channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
                    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");
                    System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        //Database 1.0
//                        JSONObject jsonObject = new JSONObject(message);
//                        String keyStr = jsonObject.getString("key");
                        //Database 2.0
                        JSONObject jsonObject = new JSONObject(message);
                        int skierID = jsonObject.getInt("skierID");
                        String keyStr = String.valueOf(skierID);
                        //add jsonObject to Jedis
                        try (Jedis jedis = pool.getResource()) {
                            jedis.set(keyStr, jsonObject.toString());
//                            System.out.println(jedis.get(keyStr) + " is saved to Redis." );
                        }
                    };
                    // consume messages
                    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
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
