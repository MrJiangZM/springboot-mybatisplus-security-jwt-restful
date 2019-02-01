package server.controller.example;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import server.config.mq.MqProductor;
import server.tool.Res;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@Api(value = "rabbitMqContronller", description = "rabbitmq测试类")
@RequestMapping("/rabbitmq")
public class MqPController {
    private static final String QUEUE_NAME = "direct-queue1";//队列
    //性能排序：fanout > direct >> topic。比例大约为11：10：6
    private static final String FANOUT_EXCHANGE = "exchange-fanout";//   任何发送到Fanout Exchange的消息都会被转发到与该Exchange绑定(Binding)的所有Queue上

    private static final String DIRECT_EXCHANGE = "exchange-direct";//   任何发送到Direct Exchange的消息都会被转发到RouteKey中指定的Queue。
    private static final String DIRECT_EXCHANGE_ROUTINGKY_ORANGE = "orange";
    private static final String DIRECT_EXCHANGE_ROUTINGKY_BLACK = "black";
    private static final String DIRECT_EXCHANGE_ROUTINGKY_ALL = "all";

    private static final String TOPIC_EXCHANGE = "exchange-topic";//     任何发送到Topic Exchange的消息都会被转发到所有关心RouteKey中指定话题的Queue上   //通配符模式

    //private static final String HEADERS_EXCHANGE = "exchange-headers";// headers匹配


    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public MqPController(MqProductor mqProductor) {
        this.rabbitTemplate = mqProductor.rabbitTemplate();
    }

    @RequestMapping("/simple")//直接发送至队列
    public Res sendMapMq() {
        JSONObject jO = new JSONObject();
        jO.put("time", LocalDateTime.now().toString());
        jO.put("msg", "test-simple");
        rabbitTemplate.convertAndSend(QUEUE_NAME, jO);
        return Res.success(jO);
    }


    @RequestMapping("/ps")
    public Res sendPSMq() {
        JSONObject jO = new JSONObject();
        jO.put("time", LocalDateTime.now().toString());
        jO.put("msg", "test-ps");
        rabbitTemplate.convertAndSend(FANOUT_EXCHANGE, "[no routingKey]", jO);
        return Res.success(jO);
    }

    @RequestMapping(value = "/direct")
    public Res sendDirectMq() {
        JSONObject jO = new JSONObject();
        jO.put("time", LocalDateTime.now().toString());
        jO.put("msg", "test-direct");
        rabbitTemplate.convertAndSend(DIRECT_EXCHANGE, DIRECT_EXCHANGE_ROUTINGKY_ORANGE, jO);
        return Res.success(jO);
    }

    @RequestMapping(value = "/topic")
    public Res sendTopicMq() {
        JSONObject jO = new JSONObject();
        jO.put("time", LocalDateTime.now().toString());
        jO.put("msg", "test-topic");
        rabbitTemplate.convertAndSend(TOPIC_EXCHANGE, "a.orange", jO);
        return Res.success(jO);
    }
}
 
