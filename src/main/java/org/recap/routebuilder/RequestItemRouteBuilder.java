package org.recap.routebuilder;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.logging.Logger;
import org.recap.ReCAPConstants;
import org.recap.mqconsumer.RequestItemQueueConsumer;
import org.recap.request.ItemRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by sudhishk on 2/12/16.
 */
@Component
public class RequestItemRouteBuilder {

    private Logger logger = Logger.getLogger(RequestItemQueueConsumer.class);

    @Autowired
    ItemRequestService itemRequestService;

    @Autowired
    public void RequestItemRouteBuilder(CamelContext camelContext, ItemRequestService itemRequestService) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.REQUEST_ITEM_QUEUE)
                            .routeId(ReCAPConstants.REQUEST_ITEM_QUEUE_ROUTEID)
                            .threads(10)
                            .choice()
                            .when(header(ReCAPConstants.REQUEST_TYPE_QUEUE_HEADER).isEqualTo(ReCAPConstants.REQUEST_TYPE_RETRIEVAL))
                            .bean(new RequestItemQueueConsumer(itemRequestService), "requestItemOnMessage")
                            .when(header(ReCAPConstants.REQUEST_TYPE_QUEUE_HEADER).isEqualTo(ReCAPConstants.REQUEST_TYPE_EDD))
                            .bean(new RequestItemQueueConsumer(itemRequestService), "requestItemEDDOnMessage")
                            .when(header(ReCAPConstants.REQUEST_TYPE_QUEUE_HEADER).isEqualTo(ReCAPConstants.REQUEST_TYPE_BORROW_DIRECT))
                            .bean(new RequestItemQueueConsumer(itemRequestService), "requestItemBorrowDirectOnMessage")
                            .when(header(ReCAPConstants.REQUEST_TYPE_QUEUE_HEADER).isEqualTo(ReCAPConstants.REQUEST_TYPE_RECALL))
                            .bean(new RequestItemQueueConsumer(itemRequestService), "requestItemRecallOnMessage");

                }
            });


            /* PUL Topics*/
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.PUL_REQUEST_TOPIC)
                            .routeId(ReCAPConstants.PUL_REQUEST_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "pulRequestTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.PUL_EDD_TOPIC)
                            .routeId(ReCAPConstants.PUL_EDD_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "pulEDDTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.PUL_RECALL_TOPIC)
                            .routeId(ReCAPConstants.PUL_RECALL_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "pulRecalTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.PUL_BORROW_DIRECT_TOPIC)
                            .routeId(ReCAPConstants.PUL_BORROW_DIRECT_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "pulBorrowDirectTopicOnMessage");
                }
            });
            /* PUL Topics*/

            /* CUL Topics*/
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.CUL_REQUEST_TOPIC)
                            .routeId(ReCAPConstants.CUL_REQUEST_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "culRequestTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.CUL_EDD_TOPIC)
                            .routeId(ReCAPConstants.CUL_EDD_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "culEDDTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.CUL_RECALL_TOPIC)
                            .routeId(ReCAPConstants.CUL_RECALL_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "culRecalTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.CUL_BORROW_DIRECT_TOPIC)
                            .routeId(ReCAPConstants.CUL_BORROW_DIRECT_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "culBorrowDirectTopicOnMessage");
                }
            });
            /* CUL Topics*/

            /* NYPL Topics */
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.NYPL_REQUEST_TOPIC)
                            .routeId(ReCAPConstants.NYPL_REQUEST_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "nyplRequestTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.NYPL_EDD_TOPIC)
                            .routeId(ReCAPConstants.NYPL_EDD_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "nyplEDDTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.NYPL_RECALL_TOPIC)
                            .routeId(ReCAPConstants.NYPL_RECALL_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "nyplRecalTopicOnMessage");
                }
            });

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(ReCAPConstants.NYPL_BORROW_DIRECT_TOPIC)
                            .routeId(ReCAPConstants.NYPL_BORROW_DIRECT_TOPIC_ROUTEID)
                            .bean(new RequestItemQueueConsumer(itemRequestService), "nyplBorrowDirectTopicOnMessage");
                }
            });
            /* NYPL Topics */


        } catch (Exception e) {
            logger.info(e.getMessage());
        }

    }

}
