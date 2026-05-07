package com.cx.springboot02.common.mq.topic.order_cancel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.cx.springboot02.common.mq.MessageQueueFinal;
import com.cx.springboot02.common.order.Context;
import com.cx.springboot02.common.order.payState.OrderCancelState;
import com.cx.springboot02.common.pay.AliReturnPayBean;
import com.cx.springboot02.common.utils.DateUtils;
import com.cx.springboot02.pojo.Order;
import com.cx.springboot02.service.impl.OrderServiceImpl;
import com.cx.springboot02.service.impl.PayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCancelConsumerSchedule implements CommandLineRunner {

    @Autowired
    PayServiceImpl payService;

    private String consumerGroup = MessageQueueFinal.ORDER_CANCEL_CONSUMER_GROUP;

    @Value("${rocketmq.namesrv-addr}")
    private String nameSrvAddr;

    @Autowired
    OrderServiceImpl orderService;

    @lombok.Data
    public static class Data{
        Order order;
        AliReturnPayBean alipayBean;
    }
    public void messageListener() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(this.consumerGroup);
        consumer.setNamesrvAddr(this.nameSrvAddr);

        /**
         * 订阅主题
         */
        consumer.subscribe(MessageQueueFinal.ORDER_CANCEL_TOPIC, "*");

        /**
         * 设置消费消息数
         */
        consumer.setConsumeMessageBatchMaxSize(1);

        /**
         * 设置消费模式
         */
        consumer.setMessageModel(MessageModel.BROADCASTING);//设置广播消费模式
        /**
         * 注册消息监听
         */
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
            for (Message message : messages) {
                System.out.println("["+ DateUtils.getCurrentTime() +"]监听到消息：" + new String(message.getBody()));
                String str = new String(message.getBody());
                JSONObject jsonObject = JSON.parseObject(str);
                Data data = JSON.toJavaObject(jsonObject, Data.class);
                Order order = data.getOrder();
                AliReturnPayBean alipayBean = data.getAlipayBean();
                Context context1 = new Context(order.getId());
                OrderCancelState orderCancelState = new OrderCancelState();
                if(orderCancelState.doAction(context1)){
//                    Context.payService.aliRefund()
                    log.info("订单"+order.getId()+"已经成功被消费[商家超时未接取,已取消订单]{}",order);
                    try {
                        //进行退款
                        payService.aliRefund(alipayBean);
                    } catch (AlipayApiException e) {
                        e.printStackTrace();
                    }
                }else{
                    log.info("订单"+order.getId()+"已经成功被消费[订单已经被商家接取]{}",order);
                }

            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
    }

    @Value("${rocketmq.producer.isOnOff:off}")
    private String isOnOff;

    @Override
    public void run(String... args) throws Exception {
        if ("on".equalsIgnoreCase(isOnOff)) {
            this.messageListener();
        } else {
            log.info("RocketMQ is disabled, skipping consumer startup");
        }
    }
}
